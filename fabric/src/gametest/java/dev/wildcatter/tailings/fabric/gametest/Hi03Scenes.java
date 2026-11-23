package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.BlastingMachineBlockEntity;
import dev.wildcatter.tailings.entity.BlastholeDrillEntity;
import dev.wildcatter.tailings.entity.EmulsionTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.BlastLoggerItem;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.Shot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Iron, episode 3, "Drill and blast" (TAILINGS_ONLY=hi03): the rotary blasthole drill (docs/multiblocks/open-pit-p3-blasting.md)
 * drilling a 3 x 5 staggered pattern on a 14 m bench, the emulsion truck charging it, the logger tying it in, the blasting machine
 * arming and firing it with flyrock, the danger zone and the siren (heavy-iron-fixes, fix 1), and the muck pile slumped to 37 degrees.
 * <ul>
 * <li>the site: a bench 14 m high in banded rock, its face running east-west along z 0 and facing north over a pit floor; three
 * patterns on it, A (x 28-64), B (x 128-164) and C (x 228-264), each 15 holes, 7 x 8 m staggered, 16 m deep; a blast shelter with
 * its blasting machine 55-60 m behind each;</li>
 * <li>A is drilled in real time for its first two holes (the rest at 40x, off camera), charged by the truck in real time at its first
 * stand (the rest pumped 10x, off camera), stemmed 6 m with its own cuttings, logged, armed and fired with the camera standing in the
 * zone and walking out of it through the siren; B and C are drilled at 40x and charged off camera, and fired for the side-on shot (B,
 * real time) and the slowed shot (C, the game at a quarter speed, tick rate 5);</li>
 * <li>the drill and the truck run on their own diesel; a villager stands at the drill's mast for scale, removed before anything is
 * fired; one stands in A's shelter, under its roof, 59 m from the nearest hole.</li>
 * </ul>
 * Filmed at 2880 x 1620 with a readout card read from the machines each tick. HI03_SHOTS=a,b films only those.
 */
final class Hi03Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	/** The pit's floor (first air) 14 m under the bench's first air at o's y. */
	private static final int PF = -14;
	/** The patterns' first holes (x; z 6, the front row 6.5 m from the face). */
	private static final int AX = 64, BX = 164, CX = 264;
	/** The stemming: 6 m keeps the collar's flyrock to 43 m, so the zone is the blast's 50 m (the default 4 m reaches the 100 m cap). */
	private static final int STEM = 6;

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o;
	private final List<String> summary = new ArrayList<>();
	private long filmStart;
	private volatile boolean endMove;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;
	private volatile boolean filming;

	private UUID drill, truck;
	private final List<BlockPos> holesA = new ArrayList<>(), holesB = new ArrayList<>(), holesC = new ArrayList<>();
	private BlockPos machineA, machineB, machineC;
	private double lastFuel = Double.NaN;
	private double engineKw;
	private long holeStart = -1;
	private String lastDrill = "";

	Hi03Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(16);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.gamma().set(1.0);
			client.options.bobView().set(false);
			client.options.menuBackgroundBlurriness().set(0);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
			// a client with no input for ten minutes drops to 10 fps (FramerateLimitTracker LONG_AFK), and a client game test waits a
			// frame a tick: every film after that would draw at half speed (Rock to Metal episode 9). Throttle only when minimized.
			client.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
		});
		context.getInput().resizeWindow(W, H);
		context.waitTicks(10);
		guiWidth(720);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule do_mob_spawning false");
			server.runCommand("weather clear");
			server.runCommand("time set 6000");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.waitTicks(20);
			hud(false);
			cardWidth("Told to a player in the zone: BLAST IN 10 s:");
			try {
				stage();
				placeDrill(AX);
				scaleFilm();
				drillA();
				drillOthers();
				prime();
				chargeA();
				chargeOthers();
				stemAndLog();
				patternFilm();
				waitGassed(holesA);
				armFilm();
				sirenFilm();
				muckFilm();
				blastFilm();
				slowFilm();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			} finally {
				server.runOnServer(srv -> srv.tickRateManager().setTickRate(20));
				server.runCommand("gamemode spectator @p");
				cardWidth("");
				context.runOnClient(client -> client.gui.setScreen(null));
				hud(true);
				log("summary:\n  " + String.join("\n  ", summary));
			}
		}
	}

	private Cam dcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(o, x, y, z, ax, ay, az);
	}

	private static void log(String line) {
		System.out.println("[hi03] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi03] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI03_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	// ---- window, card, camera, films (as Hi02Scenes) ------------------------------------------------------------------------------

	private void hud(boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
	}

	private void guiWidth(int units) {
		context.runOnClient(client -> {
			int scale = Math.max(1, Math.round(client.getWindow().getWidth() / (float) units));
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.waitTicks(2);
	}

	private void hideCursor() {
		context.runOnClient(client -> org.lwjgl.sdl.SDLMouse.SDL_HideCursor());
	}

	private static void card(String... lines) {
		Rtm01Scenes.Card.lines = lines;
	}

	private static void cardWidth(String text) {
		Rtm01Scenes.Card.widthOf = text;
	}

	private void showCard(boolean on) {
		card();
		context.runOnClient(client -> {
			if (on && !(client.gui.screen() instanceof Rtm01Scenes.Card)) {
				client.gui.setScreen(new Rtm01Scenes.Card());
			} else if (!on && client.gui.screen() instanceof Rtm01Scenes.Card) {
				client.gui.setScreen(null);
			}
		});
		context.waitTicks(2);
	}

	private void cam(Cam c, boolean cut) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y(), dz = c.tz() - c.z();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (!cut && !Float.isNaN(lastYaw)) {
			yaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw);
		}
		lastYaw = yaw;
		float y = yaw;
		if (cut) {
			server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y() - 1.62, c.z(), yaw, pitch));
		}
		context.runOnClient(client -> {
			var p = client.player;
			if (p == null) {
				return;
			}
			if (!p.isSpectator()) {
				p.getAbilities().flying = true;
			}
			p.setDeltaMovement(Vec3.ZERO);
			p.setPos(c.x(), c.y() - 1.62, c.z());
			p.setYRot(y);
			p.setYHeadRot(y);
			p.setXRot(pitch);
			if (cut) {
				p.setOldPosAndRot();
			}
		});
	}

	private void settle(int ticks) {
		context.waitTicks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 600);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		context.waitTicks(5);
	}

	private void event(String what) {
		log(String.format(Locale.ROOT, "event %.1f s (clock %.1f s): %s", ft / 20.0, (System.nanoTime() - filmStart) / 1e9, what));
	}

	interface Beat {
		void at(ServerLevel level, int t);
	}

	private static double ease(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	private static IntFunction<Cam> move(Cam a, Cam b, int move) {
		return t -> a.lerp(b, ease((double) t / move));
	}

	/** From a to b over {@code from} to {@code to} ticks of the move, holding at each end. */
	private static IntFunction<Cam> moveBetween(Cam a, Cam b, int from, int to) {
		return t -> a.lerp(b, ease((double) (t - from) / Math.max(1, to - from)));
	}

	private static IntFunction<Cam> via(Cam a, Cam b, Cam c, int move) {
		return t -> t < move / 2 ? a.lerp(b, ease(t / (move / 2.0))) : b.lerp(c, ease((t - move / 2.0) / (move - move / 2.0)));
	}

	/** Round a centre (relative to {@code base}) at a radius and height, from an angle (degrees, 0 = south of it), {@code turn} over the move. */
	private static IntFunction<Cam> orbitAround(BlockPos base, double cx, double cy, double cz, double r, double h, double from, double turn, int move) {
		return t -> {
			double a = Math.toRadians(from + turn * ease((double) t / move) * 0.15 + turn * ((double) t / move) * 0.85);
			return Cam.of(base, cx + Math.sin(a) * r, cy + h, cz + Math.cos(a) * r, cx, cy, cz);
		};
	}

	private void film(String id, int move, IntFunction<Cam> path, Beat beat) {
		boolean want = wanted(id);
		for (String mob : new String[] {"cow", "pig", "sheep", "chicken", "horse", "donkey", "rabbit", "llama", "goat", "fox", "wolf", "cat", "frog", "bee", "armadillo", "turtle", "parrot"}) {
			server.runCommand("kill @e[type=minecraft:" + mob + "]");
		}
		cam(path.apply(0), true);
		if (want) {
			settle(40);
			cam(path.apply(0), true);
			context.waitTicks(5);
		}
		hideCursor();
		Filming film = want ? context.computeOnClient(client -> Filming.start(client, id)) : null;
		filmStart = System.nanoTime();
		filming = true;
		int total = PAD + move + PAD;
		endMove = false;
		int frozen = -1;
		for (ft = 0; ft <= total; ft++) {
			if (endMove && frozen < 0 && ft > PAD) {
				frozen = Math.min(move, ft - PAD);
				total = ft + PAD;
			}
			int t = frozen >= 0 ? frozen : Math.max(0, Math.min(move, ft - PAD));
			cam(path.apply(t), false);
			int tick = ft;
			server.runOnServer(srv -> {
				if (beat != null) {
					beat.at(srv.overworld(), tick);
				}
			});
			if (ft % 10 == 0) {
				hideCursor();
			}
			if (ft % 20 == 0) {
				server.runOnServer(this::keep);
			}
			context.waitTick();
		}
		filming = false;
		double clock = (System.nanoTime() - filmStart) / 1e9;
		if (film != null) {
			film.stop();
		}
		String line = String.format(Locale.ROOT, "%s: %.1f s by ticks, %.1f s by the clock%s", id, total / 20.0, clock, want ? "" : " (not filmed)");
		log(line);
		summary.add(line);
	}

	private void ticks(int n) {
		for (int t = 0; t < n; t++) {
			if (t % 20 == 0) {
				server.runOnServer(this::keep);
			}
			context.waitTick();
		}
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%,.1f", v);
	}

	private void keep(MinecraftServer srv) {
		if (++keeps % 15 == 0) {
			ServerLevel level = srv.overworld();
			BlastholeDrillEntity d = drillE(level);
			EmulsionTruckEntity t = truckE(level);
			log(String.format(Locale.ROOT, "tick %.1f ms; drill: %s; truck: %s", srv.getAverageTickTimeNanos() / 1e6, d == null ? "-" : d.status(), t == null ? "-" : t.status()));
		}
	}

	// ---- the site -----------------------------------------------------------------------------------------------------------------

	/** The bench (z 0 on, its top the first air at o's y) over the pit floor 14 m down to its north; banded rock all through. */
	private void ground(ServerLevel level) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -60; x <= 300; x++) {
			for (int z = -130; z <= 130; z++) {
				boolean pit = z < 0 && z > -70;
				for (int y = -30; y <= -1; y++) {
					BlockState st;
					int band = Math.floorMod(y * 3 + (x + z) / 23, 11);
					st = (band == 0 ? Blocks.ANDESITE : band == 6 ? Blocks.GRANITE : Math.floorMod(x * 7 + y * 13 + z * 5, 13) == 0 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
					boolean top = pit ? y == PF - 1 : y == -1;
					if (top) {   // the floors: stone with a little andesite and tuff, no bands across them
						int k = Math.floorMod(x * 31 + z * 17 + y, 9);
						st = (k == 0 ? Blocks.ANDESITE : k == 4 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
					}
					if (pit && y >= PF) {
						st = air;
					} else if (pit && y == PF - 1 && Math.floorMod(x * 31 + z * 17, 5) == 0) {
						st = Blocks.GRAVEL.defaultBlockState();
					} else if (!pit && y == -1 && z > 100 && Math.floorMod(x * 31 + z * 17, 4) == 0) {
						st = Blocks.COARSE_DIRT.defaultBlockState();   // the haul road behind the shelters
					}
					level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), st, Block.UPDATE_CLIENTS);
				}
				for (int y = 0; y <= 40; y++) {
					level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), air, Block.UPDATE_CLIENTS);
				}
			}
		}
		log("ground laid");
	}

	/** A blast shelter at (x0..x0+6, z0..z0+6): walls on its north (to the shot) and east, a roof 3 m up; open west and south. */
	private void shelter(ServerLevel level, int x0, int z0) {
		BlockState wall = Blocks.SMOOTH_STONE.defaultBlockState();
		for (int x = x0; x <= x0 + 6; x++) {
			for (int z = z0; z <= z0 + 6; z++) {
				for (int y = 0; y <= 3; y++) {
					boolean north = z == z0, east = x == x0 + 6, roof = y == 3;
					if (roof || (north || east) && y < 3) {
						level.setBlock(o.offset(x, y, z), wall, Block.UPDATE_ALL);
					}
				}
			}
		}
	}

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -2400, 2600, 360, 260);
			int g = s.getY();
			o = new BlockPos(s.getX() + 60, g, s.getZ() + 130);
			Trailer.pad(level, o.getX() - 60, o.getZ() - 130, o.getX() + 300, o.getZ() + 130, g, 18, 70);
			Trailer.forceChunks(level, o.getX() - 100, o.getZ() - 170, o.getX() + 340, o.getZ() + 170);
			ground(level);
			// the shelters and their blasting machines, 55-60 m behind each pattern's back row
			shelter(level, AX - 21, 75);
			shelter(level, BX - 21, 75);
			shelter(level, CX - 21, 75);
			machineA = o.offset(AX - 18, 0, 78);
			machineB = o.offset(BX - 18, 0, 78);
			machineC = o.offset(CX - 18, 0, 78);
			for (BlockPos m : new BlockPos[] {machineA, machineB, machineC}) {
				level.setBlock(m, TailingsBlocks.BLASTING_MACHINE.defaultBlockState(), Block.UPDATE_ALL);
			}
			log("site at " + o.toShortString());
		});
		// the blaster's villager in A's shelter, under its roof (no AI, silent)
		server.runCommand(String.format(Locale.ROOT, "summon villager %.1f %d %.1f {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[160f,0f],Tags:[\"hi03s\"]}",
				o.getX() + AX - 17.5, o.getY(), o.getZ() + 79.5));
		server.runCommand("kill @e[type=item]");
	}

	// ---- the drill ---------------------------------------------------------------------------------------------------------------

	private BlastholeDrillEntity drillE(ServerLevel level) {
		return drill != null && level.getEntity(drill) instanceof BlastholeDrillEntity d ? d : null;
	}

	private EmulsionTruckEntity truckE(ServerLevel level) {
		return truck != null && level.getEntity(truck) instanceof EmulsionTruckEntity t ? t : null;
	}

	/** The drill facing south, its mast (at its back end) over the first hole at (x, 6), 6.5 m in from the face. */
	private void placeDrill(int x) {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlastholeDrillEntity d = drillE(level);
			if (d == null) {
				d = TailingsEntities.BLASTHOLE_DRILL.create(level, EntitySpawnReason.COMMAND);
				d.setPos(o.getX() + x + 0.5, o.getY(), o.getZ() + 6.5 + BlastholeDrillEntity.HOLE_BACK);
				d.setYRot(0);
				d.setFuel(d.tank());
				level.addFreshEntity(d);
				drill = d.getUUID();
			} else {
				d.setPos(o.getX() + x + 0.5, o.getY(), o.getZ() + 6.5 + BlastholeDrillEntity.HOLE_BACK);
				d.setYRot(0);
			}
			d.setting("Rows", 3);
			d.setting("Columns", 5);
			d.setting("Burden", Blasting.BURDEN);
			d.setting("Spacing", Blasting.SPACING);
			d.setting("Bench", Blasting.BENCH);
			d.setting("Subdrill", Blasting.SUBDRILL);
			d.setting("Staggered", 1);
			d.setting("Electric", 0);
			log("drill at " + x + ": hole column " + d.holeColumn().subtract(o).toShortString());
		});
		ticks(10);
	}

	private String goDrill(int divisor) {
		return server.computeOnServer(srv -> {
			BlastholeDrillEntity d = drillE(srv.overworld());
			d.fast(divisor);
			String said = d.go();
			StringBuilder plan = new StringBuilder();
			for (var h : d.plan()) {
				plan.append(String.format(Locale.ROOT, " r%d c%d (%d, %d)", h.row() + 1, h.col() + 1, h.x() - o.getX(), h.z() - o.getZ()));
			}
			log("GO: " + said + ";" + plan);
			return said;
		});
	}

	private String drillStatus() {
		return server.computeOnServer(srv -> {
			BlastholeDrillEntity d = drillE(srv.overworld());
			return d == null ? "gone" : d.status();
		});
	}

	/** Until the drill's pattern is done (or it stops), keeping the log. */
	private void untilDrillDone(int most) {
		for (int i = 0; i < most; i += 20) {
			ticks(20);
			String st = drillStatus();
			if (i % 400 == 0) {
				log("drill: " + st);
			}
			if (st.startsWith("DONE") || st.contains("OUT OF FUEL") || st.contains("BLOCKED") || st.contains("NOT LEVEL") || st.contains("SHORT")) {
				log("drill: " + st);
				return;
			}
		}
		log("drill not done: " + drillStatus());
	}

	/** The drill's engine output from its burn: 36 MJ a litre at 38 % (PitFuel), measured over a second. */
	private void meter(BlastholeDrillEntity d, int tick) {
		if (d == null || tick % 20 != 0) {
			return;
		}
		double f = d.fuel();
		if (!Double.isNaN(lastFuel) && lastFuel >= f) {
			engineKw = (lastFuel - f) * 36.0e6 * 0.38 / 1000.0;
		}
		lastFuel = f;
	}

	private String[] drillCard(ServerLevel level, int tick) {
		BlastholeDrillEntity d = drillE(level);
		if (d == null) {
			return new String[] {"^Blasthole drill", "gone"};
		}
		meter(d, tick);
		String st = d.status();
		String now = st.split("\\. Holes drilled")[0];
		if (!now.equals(lastDrill) && filming) {
			event("drill: " + now);
			if (now.startsWith("jacks down")) {
				holeStart = level.getGameTime();
			} else if (now.startsWith("hole drilled")) {
				holeStart = -1;
			}
		}
		lastDrill = now;
		float lift = d.lift();
		String depth = lift > 0 ? String.format(Locale.ROOT, "Hole: %.1f of 16 m; 10 s a metre", lift * 16) : "Hole: -";
		String clock = holeStart < 0 ? "" : String.format(Locale.ROOT, "; on this hole %d:%02d", (level.getGameTime() - holeStart) / 1200, (level.getGameTime() - holeStart) / 20 % 60);
		return new String[] {"^Blasthole drill, 270 mm bit", "Now: " + (now.length() > 44 ? now.substring(0, 44) : now), depth,
				String.format(Locale.ROOT, "Holes drilled: %d of 15%s", d.holesDrilled(), clock),
				String.format(Locale.ROOT, "Diesel: %s kW (from its burn); %s L", f0(engineKw), f0(d.fuel()))};
	}

	/** hi03_scale: a villager at the drill's mast foot, by the first hole's collar; the camera rises up the 26 m mast. */
	private void scaleFilm() {
		server.runCommand(String.format(Locale.ROOT, "summon villager %.1f %d %.1f {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[210f,0f],Tags:[\"hi03a\"]}",
				o.getX() + AX - 4.5, o.getY(), o.getZ() + 5.5));
		ticks(10);
		Cam a = dcam(AX - 10, 1.7, 0.5, AX - 4.5, 1.2, 5.5);
		Cam b = dcam(AX - 16, 9, -6, AX - 1, 9, 7);
		Cam c = dcam(AX - 30, 20, -22, AX, 13, 10);
		film("hi03_scale", 1000, via(a, b, c, 1000), null);
		server.runCommand("kill @e[tag=hi03a]");
	}

	/** Pattern A: its first hole in real time (hi03_drill), the tram to the second (hi03_tram), the wide as it drills; the rest at 40x. */
	private void drillA() {
		boolean real = wanted("hi03_drill") || wanted("hi03_tram") || wanted("hi03_wide");
		goDrill(real ? 1 : 40);
		if (real) {
			showCard(true);
			int[] done = {-1};
			film("hi03_drill", 4400, move(dcam(AX + 5, 14, 34, AX, 11, 6.5), dcam(AX + 3, 13, 35, AX, 9, 6.5), 4400), (level, t) -> {
				card(drillCard(level, t));
				BlastholeDrillEntity d = drillE(level);
				if (d != null && d.holesDrilled() >= 1) {
					if (done[0] < 0) {
						done[0] = t;
						event("hole 1 drilled");
					}
					if (t - done[0] > 60) {
						endMove = true;
					}
				}
			});
			int[] drilling = {-1};
			film("hi03_tram", 1800, move(dcam(AX - 28, 9, 34, AX - 4, 4, 10), dcam(AX - 26, 10, 30, AX - 8, 4, 9), 1800), (level, t) -> {
				card(drillCard(level, t));
				BlastholeDrillEntity d = drillE(level);
				if (d != null && d.status().startsWith("drilling hole 2")) {
					if (drilling[0] < 0) {
						drilling[0] = t;
					}
					if (t - drilling[0] > 100) {
						endMove = true;
					}
				}
			});
			// hi03_head: close on the rotary head as it turns the rods and comes down the mast, from the deck side between the mast's legs
			film("hi03_head", 500, t -> {
				double lift = server.computeOnServer(srv -> {
					BlastholeDrillEntity d = drillE(srv.overworld());
					return d == null ? 0.0 : (double) d.lift();
				});
				double head = 23 - 16 * lift - 0.5;
				return dcam(AX - 8 + 2.5, head + 2.5, 6.5 + 10, AX - 8 + 0.5, head, 6.5);
			}, (level, t) -> card(drillCard(level, t)));
			showCard(false);
			film("hi03_wide", 1400, orbitAround(o, AX - 18, -4, 8, 95, 50, 210, 40, 1400), (level, t) -> drillCard(level, t));
			server.runOnServer(srv -> drillE(srv.overworld()).fast(40));
			log("pattern A: holes after the second drilled at 40x, off camera");
			summary.add("pattern A: holes 3-15 drilled at 40x off camera");
		}
		untilDrillDone(20000);
		holesA.addAll(collars(AX));
		log("pattern A: " + holesA.size() + " holes");
		require(holesA.size() == 15, "pattern A should have 15 holes, has " + holesA.size());
	}

	/** Patterns B and C, at 40x, off camera; then the drill parked west of A, clear of every shot. */
	private void drillOthers() {
		placeDrill(BX);
		goDrill(40);
		untilDrillDone(12000);
		holesB.addAll(collars(BX));
		placeDrill(CX);
		goDrill(40);
		untilDrillDone(12000);
		holesC.addAll(collars(CX));
		log("patterns B and C: " + holesB.size() + ", " + holesC.size() + " holes");
		require(holesB.size() == 15 && holesC.size() == 15, "patterns B and C should have 15 holes each");
		server.runOnServer(srv -> {
			BlastholeDrillEntity d = drillE(srv.overworld());
			d.setPos(o.getX() - 25.5, o.getY(), o.getZ() + 40.5);
			d.setYRot(90);
		});
		summary.add("patterns B and C drilled at 40x off camera; the drill parked west of A");
	}

	/** The collars of the pattern whose first hole is at x. */
	private List<BlockPos> collars(int x) {
		return server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			List<BlockPos> found = new ArrayList<>();
			for (BlockPos p : BlockPos.betweenClosed(o.offset(x - 44, -1, 0), o.offset(x + 4, -1, 30))) {
				if (level.getBlockEntity(p) instanceof BlastholeBlockEntity hole && hole.isCollar()) {
					found.add(p.immutable());
					if (hole.depth() != 16 || hole.shortHole()) {
						log("hole at " + p.subtract(o).toShortString() + ": " + hole.depth() + " m" + (hole.shortHole() ? ", SHORT" : ""));
					}
				}
			}
			return found;
		});
	}

	// ---- priming, charging, stemming, logging -------------------------------------------------------------------------------------

	/** A cast booster and an electronic delay detonator down every hole (by hand, off camera). */
	private void prime() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int id = 1;
			for (List<BlockPos> pattern : List.of(holesA, holesB, holesC)) {
				for (BlockPos c : pattern) {
					BlastholeBlockEntity hole = (BlastholeBlockEntity) level.getBlockEntity(c);
					hole.addBooster();
					hole.addDetonator(id++);
				}
			}
			log("primed " + (id - 1) + " holes");
		});
	}

	/** The truck with its hose reel (its back) at (x, z), facing east, stopped. */
	private void standTruck(ServerLevel level, double x, double z) {
		EmulsionTruckEntity t = truckE(level);
		if (t == null) {
			t = TailingsEntities.EMULSION_TRUCK.create(level, EntitySpawnReason.COMMAND);
			t.setPos(o.getX() + x, o.getY(), o.getZ() + z);
			t.setYRot(-90);
			t.setFuel(t.tank());
			t.setMatrix(Blasting.TRUCK_MATRIX_KG);
			t.setStemming(STEM);
			level.addFreshEntity(t);
			truck = t.getUUID();
		}
		t.setYRot(-90);
		t.setPos(o.getX() + x, o.getY(), o.getZ() + z);
		Vec3 reel = t.reel();
		t.setPos(t.getX() + (o.getX() + x - reel.x), o.getY(), t.getZ() + (o.getZ() + z - reel.z));
	}

	private boolean truckBusy() {
		return server.computeOnServer(srv -> {
			EmulsionTruckEntity t = truckE(srv.overworld());
			return t != null && t.busy();
		});
	}

	private BlockPos firstUncharged(List<BlockPos> pattern) {
		return server.computeOnServer(srv -> {
			for (BlockPos c : pattern) {
				if (srv.overworld().getBlockEntity(c) instanceof BlastholeBlockEntity h && h.chargeKg() <= 0) {
					return c;
				}
			}
			return null;
		});
	}

	/** Charges a pattern stand by stand: the reel set beside the first uncharged hole, toward the pattern's middle row. */
	private void chargeAll(List<BlockPos> pattern, int pump) {
		server.runOnServer(srv -> {
			EmulsionTruckEntity t = truckE(srv.overworld());
			t.fastPump(pump);
			t.setMatrix(Blasting.TRUCK_MATRIX_KG);
		});
		for (int stands = 0; stands < 12; stands++) {
			BlockPos c = firstUncharged(pattern);
			if (c == null) {
				return;
			}
			double rz = c.getZ() - o.getZ() + (c.getZ() - o.getZ() < 13 ? 3.5 : -3.5);
			server.runOnServer(srv -> {
				standTruck(srv.overworld(), c.getX() - o.getX() + 0.5, rz);
			});
			ticks(5);
			server.runOnServer(srv -> log("charging: " + truckE(srv.overworld()).chargeAll(srv.overworld())));
			for (int i = 0; i < 2400 && truckBusy(); i += 10) {
				ticks(10);
			}
		}
	}

	/** Pattern A: the first stand in real time (hi03_charge), the rest pumped 10x off camera. */
	private void chargeA() {
		server.runOnServer(srv -> standTruck(srv.overworld(), AX - 4 + 0.5, 9.5));
		ticks(10);
		server.runOnServer(srv -> log("charging A, stand 1: " + truckE(srv.overworld()).chargeAll(srv.overworld())));
		showCard(true);
		film("hi03_charge", 1700, move(dcam(AX - 11, 4, 17, AX - 2, 0.5, 8), dcam(AX - 10, 4.5, 16, AX - 2, 0.5, 8), 1700), (level, t) -> card(truckCard(level)));
		showCard(false);
		for (int i = 0; i < 2400 && truckBusy(); i += 10) {
			ticks(10);
		}
		chargeAll(holesA, 10);
		summary.add("pattern A: the first stand charged in real time, the rest pumped at 10x off camera");
	}

	private void chargeOthers() {
		chargeAll(holesB, 10);
		chargeAll(holesC, 10);
		server.runOnServer(srv -> {
			EmulsionTruckEntity t = truckE(srv.overworld());
			t.setPos(o.getX() - 25.5, o.getY(), o.getZ() + 60.5);
		});
		summary.add("patterns B and C charged at 10x off camera; the truck parked west of A");
	}

	private String[] truckCard(ServerLevel level) {
		EmulsionTruckEntity t = truckE(level);
		if (t == null) {
			return new String[] {"^Emulsion truck", "gone"};
		}
		String st = t.status();
		BlastholeBlockEntity near = null;
		int charged = 0;
		for (BlockPos c : holesA) {
			if (level.getBlockEntity(c) instanceof BlastholeBlockEntity h && h.chargeKg() > 0) {
				charged++;
			}
		}
		String head = st.split(":")[0];
		if (filming && !head.equals(lastDrill)) {
			event("truck: " + st);
		}
		lastDrill = head;
		String now = st.startsWith("charging ") ? st.replaceAll("charging -?\\d+, -?\\d+, -?\\d+: ", "charging a hole: ").split(";")[0] : st.split(";")[0];
		return new String[] {"^Emulsion truck, pumping 20 kg/s", "Now: " + now, String.format(Locale.ROOT, "Matrix in the truck: %s kg", f0(t.matrixKg())),
				String.format(Locale.ROOT, "Column: 16 m hole less %d m stemming: %s kg", STEM, f0(Blasting.chargeKg(Blasting.column(16, STEM)))),
				String.format(Locale.ROOT, "Holes charged: %d of 15", charged)};
	}

	/** Every hole stemmed to the collar with its own cuttings; each pattern logged (row 65 ms, hole 25 ms) and loaded, key in. */
	private void stemAndLog() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			List<List<BlockPos>> patterns = List.of(holesA, holesB, holesC);
			BlockPos[] machines = {machineA, machineB, machineC};
			for (int k = 0; k < 3; k++) {
				ItemStack logger = new ItemStack(TailingsItems.BLAST_LOGGER);
				for (BlockPos c : patterns.get(k)) {
					BlastholeBlockEntity hole = (BlastholeBlockEntity) level.getBlockEntity(c);
					hole.stemWithCuttings();
					BlastLoggerItem.log(level, logger, c);
				}
				BlastingMachineBlockEntity m = (BlastingMachineBlockEntity) level.getBlockEntity(machines[k]);
				log("pattern " + "ABC".charAt(k) + ": " + m.load(logger) + "; " + ((BlastholeBlockEntity) level.getBlockEntity(patterns.get(k).get(0))).readout());
				m.setKey(true);
			}
		});
	}

	private void waitGassed(List<BlockPos> pattern) {
		long last = server.computeOnServer(srv -> {
			long l = 0;
			for (BlockPos c : pattern) {
				l = Math.max(l, ((BlastholeBlockEntity) srv.overworld().getBlockEntity(c)).chargedTick());
			}
			return l;
		});
		long now = server.computeOnServer(srv -> srv.overworld().getGameTime());
		if (now - last < Blasting.SLEEP_TICKS + 40) {
			log("waiting " + (Blasting.SLEEP_TICKS + 40 - (now - last)) / 20 + " s for the last charge to gas");
			ticks((int) (Blasting.SLEEP_TICKS + 40 - (now - last)));
		}
	}

	/** hi03_pattern: pattern A from above, charged, stemmed and logged; the card reads the holes. */
	private void patternFilm() {
		showCard(true);
		film("hi03_pattern", 700, move(dcam(AX - 12, 15, -8, AX - 18, 0, 13), dcam(AX - 22, 17, -6, AX - 18, 0, 13), 700), (level, t) -> {
			double kg = 0, stem = 0;
			int lo = Integer.MAX_VALUE, hi = 0;
			for (BlockPos c : holesA) {
				BlastholeBlockEntity h = (BlastholeBlockEntity) level.getBlockEntity(c);
				kg += h.chargeKg();
				stem = Math.max(stem, h.stemming());
				lo = Math.min(lo, h.delayMs());
				hi = Math.max(hi, h.delayMs());
			}
			card("^Pattern A: 15 holes, 3 rows of 5, staggered", "Each 16 m deep: 14 m bench, 2 m subdrill", "Burden 7 m, spacing 8 m",
					String.format(Locale.ROOT, "Charged: %s kg in all; stemmed %.0f m", f0(kg), stem),
					String.format(Locale.ROOT, "Logged: rows 65 ms apart, holes 25 ms: %d to %d ms", lo, hi));
		});
		showCard(false);
	}

	// ---- arming and firing --------------------------------------------------------------------------------------------------------

	private static BlastingMachineBlockEntity machine(ServerLevel level, BlockPos at) {
		return (BlastingMachineBlockEntity) level.getBlockEntity(at);
	}

	private static String[] statusLines(String head, String status) {
		List<String> out = new ArrayList<>();
		out.add(head);
		for (String part : status.split("; ")) {
			while (part.length() > 46) {
				int cut = part.lastIndexOf(' ', 46);
				cut = cut <= 0 ? 46 : cut;
				out.add(part.substring(0, cut));
				part = "  " + part.substring(cut).trim();
			}
			out.add(part);
		}
		return out.toArray(new String[0]);
	}

	private double nearestHole(List<BlockPos> pattern, Vec3 at) {
		double near = Double.MAX_VALUE;
		for (BlockPos c : pattern) {
			near = Math.min(near, Math.hypot(c.getX() + 0.5 - at.x, c.getZ() + 0.5 - at.z));
		}
		return near;
	}

	/** hi03_arm: A's blasting machine in its shelter, the villager beside it; ARM tests the shot and reads the zone to clear. */
	private void armFilm() {
		showCard(true);
		String[] said = {"15 holes loaded"};
		film("hi03_arm", 500, move(dcam(AX - 25, 2.4, 86, AX - 18, 0.8, 78), dcam(AX - 24, 2.6, 85, AX - 18, 0.8, 78), 500), (level, t) -> {
			if (t == PAD + 60) {
				said[0] = machine(level, machineA).arm();
				event("ARM: " + said[0]);
			}
			card(statusLines(String.format(Locale.ROOT, "^Blasting machine, %.0f m from the nearest hole", nearestHole(holesA, Vec3.atCenterOf(machineA))),
					t < PAD + 60 ? "Key in; " + said[0] + "; press ARM" : said[0]));
		});
		showCard(false);
	}

	private static java.lang.reflect.Field overlayText, overlayTime;

	/** The action-bar message the game is showing the camera's player now, or "". */
	private String overlayNow() {
		return context.computeOnClient(client -> {
			try {
				if (overlayText == null) {
					overlayText = net.minecraft.client.gui.Hud.class.getDeclaredField("overlayMessageString");
					overlayText.setAccessible(true);
					overlayTime = net.minecraft.client.gui.Hud.class.getDeclaredField("overlayMessageTime");
					overlayTime.setAccessible(true);
				}
				Object c = overlayText.get(client.gui.hud);
				int left = overlayTime.getInt(client.gui.hud);
				return c instanceof Component comp && left > 0 ? comp.getString() : "";
			} catch (ReflectiveOperationException ex) {
				return "?";
			}
		});
	}

	/**
	 * hi03_siren: the camera's player (creative, not a spectator) stands 36 m behind pattern A, in the zone. A is armed; FIRE asks a
	 * second press while a player is in the zone; the second starts the 10 s siren. Through it the player is told each second how long
	 * is left; the camera walks out to 67 m, by A's shelter, and the shot goes. It holds as the muck spills and slumps.
	 */
	private void sirenFilm() {
		server.runCommand("gamemode creative @p");
		context.waitTicks(10);
		showCard(true);
		String[] machineSays = {""};
		String[] told = {""};
		Cam s = dcam(AX - 24, 12, 55, AX - 18, -1, 14);
		Cam e = dcam(AX - 24, 5, 88, AX - 18, -3, 12);
		int fire2 = PAD + 90;
		IntFunction<Cam> walk = moveBetween(s, e, 110, 270);
		String[] seen = {""};
		int[] seenAt = {0};
		int[] step = {0};
		film("hi03_siren", 540, t -> {
			// what the game shows the player now (its action bar, hidden with the HUD): fresh only while the siren keeps sending it
			String now = overlayNow();
			step[0]++;
			if (!now.equals(seen[0])) {
				seen[0] = now;
				seenAt[0] = step[0];
			}
			told[0] = step[0] - seenAt[0] < 30 ? now : "";
			return walk.apply(t);
		}, (level, t) -> {
			BlastingMachineBlockEntity m = machine(level, machineA);
			if (t == PAD) {
				machineSays[0] = m.arm();
				event("ARM: " + machineSays[0]);
			} else if (t == PAD + 30) {
				machineSays[0] = m.fire();
				event("FIRE: " + machineSays[0]);
			} else if (t == fire2) {
				machineSays[0] = m.fire();
				event("FIRE again: " + machineSays[0]);
			}
			String st = m.status();
			if (!st.equals(machineSays[0])) {
				event("machine: " + st);
				machineSays[0] = st;
			}
			Vec3 me = level.players().isEmpty() ? Vec3.ZERO : level.players().get(0).position();
			double near = nearestHole(holesA, me);
			double zone = m.zone();
			java.util.regex.Matcher fly = java.util.regex.Pattern.compile("flyrock to (\\d+) m: clear (\\d+) m").matcher(st);
			String machineLine = st.startsWith("PLAYER IN DANGER ZONE") ? "PLAYER IN DANGER ZONE: press FIRE again"
					: st.startsWith("ARMED") && fly.find() ? "ARMED: flyrock to " + fly.group(1) + " m, clear " + fly.group(2) + " m"
					: st.startsWith("FIRED") && st.split(",").length > 1 ? st.split(",")[0] + ", " + st.split(",")[1].trim() : st.length() > 44 ? st.substring(0, 44) : st;
			card("^Blasting machine: FIRE", "Machine: " + machineLine,
					String.format(Locale.ROOT, "Camera: %.0f m from the nearest hole: %s", near, zone > 0 && near < zone ? "IN THE ZONE" : "clear"),
					"Told to a player in the zone:", "  " + (told[0].isEmpty() ? "-" : told[0].replace(". ", ".|").split("\\|")[0]),
					"  " + (told[0].contains(". ") ? told[0].substring(told[0].indexOf(". ") + 2) : ""));
		});
		showCard(false);
		server.runCommand("gamemode spectator @p");
		context.waitTicks(5);
		server.runOnServer(srv -> {
			var m = machine(srv.overworld(), machineA);
			Shot shot = m.lastShot();
			log("shot A: " + m.status() + (shot == null ? "" : String.format(Locale.ROOT, "; bank %d, %.0f t; fragments %d, strikes %d, reach %.0f m, slumped %d, flyrock %d",
					shot.bank(), shot.tonnes(), shot.fragmentsThrown(), shot.strikes(), shot.reach(), shot.slumped(), shot.flyrock())));
			for (var v : srv.overworld().getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new net.minecraft.world.phys.AABB(o).inflate(400))) {
				log(String.format(Locale.ROOT, "villager at %s: health %.1f of %.1f", v.blockPosition().subtract(o).toShortString(), v.getHealth(), v.getMaxHealth()));
			}
		});
	}

	/** hi03_muck: pattern A's muck pile from the pit, slumped off the face; the card reads the shot. */
	private void muckFilm() {
		String[] lines = server.computeOnServer(srv -> {
			Shot shot = machine(srv.overworld(), machineA).lastShot();
			if (shot == null) {
				return new String[] {"^Shot A", "no shot"};
			}
			return new String[] {"^Shot A, fired", String.format(Locale.ROOT, "Broken: %,d bank m³, %s t", shot.bank(), f0(shot.tonnes())),
					String.format(Locale.ROOT, "Swell: 4 muck blocks for every 3 broken"), String.format(Locale.ROOT, "Rolled off the face to 37°: %,d blocks", shot.slumped()),
					String.format(Locale.ROOT, "Fragments thrown: %d; farthest reach %.0f m", shot.fragmentsThrown(), shot.reach())};
		});
		showCard(true);
		card(lines);
		film("hi03_muck", 1000, orbitAround(o, AX - 18, -10, -2, 60, 22, 160, 50, 1000), null);
		showCard(false);
	}

	/** hi03_blast: pattern B fired, from the pit to its north-west, about 67 m from its nearest hole; real time. */
	private void blastFilm() {
		waitGassed(holesB);
		String armed = server.computeOnServer(srv -> machine(srv.overworld(), machineB).arm());
		log("B: " + armed);
		Vec3 eye = new Vec3(o.getX() + BX - 68, o.getY() + 3, o.getZ() - 46);
		log(String.format(Locale.ROOT, "B's camera %.0f m from its nearest hole", nearestHole(holesB, eye)));
		film("hi03_blast", 520, move(dcam(BX - 68, 3, -46, BX - 18, -7, 6), dcam(BX - 66, 3, -48, BX - 18, -7, 6), 520), (level, t) -> {
			if (t == PAD) {
				event("FIRE: " + machine(level, machineB).fire());
			}
			String st = machine(level, machineB).status();
			if (filming && !st.equals(lastDrill)) {
				event("machine B: " + st);
				lastDrill = st;
			}
		});
		server.runOnServer(srv -> log("shot B: " + machine(srv.overworld(), machineB).status()));
	}

	/** hi03_slow: pattern C fired, front-on from the pit 68 m out, the game slowed to a quarter (tick rate 5) through the shot. */
	private void slowFilm() {
		waitGassed(holesC);
		log("C: " + server.computeOnServer(srv -> machine(srv.overworld(), machineC).arm()));
		Cam c = dcam(CX - 16, -2, -62, CX - 16, -6, 8);
		film("hi03_slow", 400, t -> c, (level, t) -> {
			if (t == PAD) {
				event("FIRE: " + machine(level, machineC).fire());
			} else if (t == PAD + 170) {
				level.getServer().tickRateManager().setTickRate(5);
				event("slowed: tick rate 5 (a quarter of real time)");
			} else if (t == PAD + 340) {
				level.getServer().tickRateManager().setTickRate(20);
				event("real time again: tick rate 20");
			}
			String st = machine(level, machineC).status();
			if (filming && !st.equals(lastDrill)) {
				event("machine C: " + st);
				lastDrill = st;
			}
		});
		server.runOnServer(srv -> srv.tickRateManager().setTickRate(20));
		server.runOnServer(srv -> log("shot C: " + machine(srv.overworld(), machineC).status()));
	}
}
