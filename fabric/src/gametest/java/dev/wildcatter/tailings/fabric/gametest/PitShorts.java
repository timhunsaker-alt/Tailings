package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.BlastingMachineBlockEntity;
import dev.wildcatter.tailings.block.DraglineBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.RopeShovelBlockEntity;
import dev.wildcatter.tailings.entity.BlastholeDrillEntity;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.entity.EmulsionTruckEntity;
import dev.wildcatter.tailings.entity.RopeShovelEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.BlastLoggerItem;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineLayout;
import dev.wildcatter.tailings.pit.RopeShovelLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The pit shorts' footage (TAILINGS_ONLY=pitShorts), two vertical shorts filmed in a portrait window on the seed's own ground at
 * golden hour. PIT_SHORTS_SET picks the short (one per run, each its own world):
 * <ul>
 * <li>{@code pit}: drill, blast, load, haul. A bench cut into the ground over a pit floor: the blasthole drill walks a 3 x 4 pattern on
 * it, the emulsion truck charges the holes, they are stemmed, logged and fired from 60 m; the face breaks to muck; the rope shovel
 * digs it into an ultra truck, which drives off and tips its load over a waste dump's crest. Films {@code 01_blast} (the hook),
 * {@code 02_drill}, {@code 03_charge}, {@code 04_load}, {@code 05_haul}, {@code 06_dump}.</li>
 * <li>{@code dragline}: the walking dragline stripping overburden off a bauxite seam and casting it onto the spoil in the last strip's
 * hole, an ultra truck parked by its tub for scale. Films {@code 01_cast} (the hook), {@code 02_drag}, {@code 03_swing}, {@code 04_low},
 * {@code 05_walk}, {@code 06_scale}.</li>
 * </ul>
 * PIT_SHORTS_SHOTS=1,3 films only those (a shot the later ones need still runs, unfilmed). Stills go to PIT_SHORTS_OUT; films to
 * {@code fabric/build/scene-videos/pitShorts_*.mov}, or with PIT_SHORTS_FRAMES=1 a frame a tick to {@code <out>/frames/<id>/} (a locked
 * screen records nothing). Each film's events are logged at their second in the film. The machines run fast (the drill, the shovel's
 * passes and the dragline's cycles), as the server scenes run them; the film says how fast.
 */
final class PitShorts {
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String SET = System.getenv().getOrDefault("PIT_SHORTS_SET", "pit");
	private static final Path OUT = Path.of(System.getenv().getOrDefault("PIT_SHORTS_OUT", "pit-shorts")).toAbsolutePath();
	private static final int PW = Integer.parseInt(System.getenv().getOrDefault("PIT_SHORTS_PW", "486"));
	private static final int PH = Integer.parseInt(System.getenv().getOrDefault("PIT_SHORTS_PH", "864"));
	private static final long TIME = Long.parseLong(System.getenv().getOrDefault("PIT_SHORTS_TIME", "11600"));
	private static final boolean FRAMES = "1".equals(System.getenv("PIT_SHORTS_FRAMES"));

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o;
	/** The film running now: its tick, and what happened in it. */
	private int ft;
	private final List<String> events = new ArrayList<>();
	private final List<String> summary = new ArrayList<>();

	PitShorts(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[tailings-pit-shorts] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("PIT_SHORTS_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(shot));
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(16);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(75);
			client.options.gamma().set(1.0);
			client.options.bobView().set(false);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		context.getInput().resizeWindow(PW, PH);
		context.waitTicks(10);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			context.waitTicks(20);
			try {
				if ("dragline".equals(SET)) {
					dragline();
				} else {
					pit();
				}
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
			}
			log("summary:\n  " + String.join("\n  ", summary));
			log("done");
		}
	}

	// ---- camera, stills and films ----

	private void put(Cam c) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y(), dz = c.tz() - c.z();
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y() - 1.62, c.z(), yaw, pitch));
	}

	private Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return Cam.of(o, x, y, z, tx, ty, tz);
	}

	/** From a to b, eased, over [from, to] of a film's ticks. */
	private static IntFunction<Cam> move(Cam a, Cam b, int from, int to) {
		return t -> {
			double s = Math.max(0, Math.min(1, (double) (t - from) / Math.max(1, to - from)));
			return a.lerp(b, s * s * (3 - 2 * s));
		};
	}

	private void ticks(int n) {
		for (int t = 0; t < n; t++) {
			context.waitTick();
			if (t % 20 == 0) {
				server.runCommand("kill @e[type=item]");
			}
		}
	}

	private void settle(int ticks) {
		ticks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 400);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		ticks(5);
	}

	private void still(String name) {
		Path at = context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(OUT).disableCounterPrefix().withSize(1080, 1920));
		log("still " + at);
	}

	private void frame(String id, int t) {
		context.takeScreenshot(TestScreenshotOptions.of(String.format(Locale.ROOT, "f%04d", t)).withDestinationDir(OUT.resolve("frames").resolve(id))
				.disableCounterPrefix().withSize(972, 1728));
	}

	/** Something seen in the film running now, at its second. */
	private void event(String what) {
		events.add(String.format(Locale.ROOT, "%s at %.2f s", what, ft / 20.0));
	}

	/**
	 * A film of {@code length} ticks: the camera where {@code cam} puts it each tick, {@code each} run every tick before it. Unwanted, the
	 * same ticks run unfilmed (the machines carry on as they would).
	 */
	private void film(int shot, String id, int length, IntFunction<Cam> cam, IntConsumer each) {
		boolean filmed = wanted(shot);
		put(cam.apply(0));
		if (filmed) {
			settle(30);
			still(id + "_a");
		}
		events.clear();
		Filming film = filmed && !FRAMES ? context.computeOnClient(client -> Filming.start(client, "pitShorts_" + id)) : null;
		for (ft = 0; ft <= length; ft++) {
			put(cam.apply(ft));
			if (each != null) {
				each.accept(ft);
			}
			context.waitTick();
			if (filmed && FRAMES) {
				frame(id, ft);
			}
			if (ft % 20 == 0) {
				server.runCommand("kill @e[type=item]");
			}
		}
		if (film != null) {
			film.stop();
		}
		String line = String.format(Locale.ROOT, "%s%s: %.2f s; %s", id, filmed ? "" : " (not filmed)", length / 20.0, events);
		log("film " + line);
		summary.add(line);
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity> T entity(UUID id, Class<T> type) {
		return server.computeOnServer(srv -> {
			Entity e = srv.overworld().getEntity(id);
			return type.isInstance(e) ? (T) e : null;
		});
	}

	// ---- the pit: drill, blast, load, haul ----

	private UUID drillId, shovelId, truckId, chargerId;
	private Vec3 shovelOffset;
	private BlockPos firing;

	/**
	 * The pit's site, relative to o (o's y the ground's first air): a bench of bare rock 14 m high north of z 0, the drill's pattern on
	 * it; the pit's floor south of the face, at y -14, benched up to the ground on its west, east and south sides; south of the pit a waste
	 * dump, its crest at z 100 with a berm, its face falling away south.
	 */
	private void pitSite() {
		BlockPos[] at = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 300, 400, 170, 210);
			int g = s.getY();
			BlockPos o = new BlockPos(s.getX() + 70, g, s.getZ() + 70);
			Trailer.pad(level, o.getX() - 70, o.getZ() - 90, o.getX() + 100, o.getZ() + 125, g, 18, 45);
			BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
			for (int x = -45; x <= 95; x++) {
				for (int z = -70; z <= 122; z++) {
					int ring;   // the pit: its floor x -10..70, z 0..45, 14 deep; benches 5 high and 6 wide up to the ground round it
					int out = Math.max(Math.max(-10 - x, x - 70), z - 45);
					ring = out <= 0 ? 0 : (out + 5) / 6;
					int depth = z >= 0 ? Math.max(0, 14 - 5 * ring) : 0;
					boolean bench = z < 0 && z >= -62 && x >= -35 && x <= 90;
					if (bench || depth > 0) {
						for (int yy = -20; yy < -depth; yy++) {
							boolean top = yy == -depth - 1;
							Block rock = top && depth > 0 && Math.floorMod(x * 31 + z * 17, 4) == 0 ? Blocks.GRAVEL
									: Math.floorMod(x * 7 + yy * 13 + z * 5, 9) == 0 ? Blocks.ANDESITE : Blocks.STONE;
							level.setBlock(p.set(o.getX() + x, o.getY() + yy, o.getZ() + z), rock.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
						for (int yy = -depth; yy <= 2; yy++) {
							level.setBlock(p.set(o.getX() + x, o.getY() + yy, o.getZ() + z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
					// the dump: a haul road of gravel to its crest at z 100, a berm on the crest, the dump's face falling south of it
					if (z >= 66 && z <= 100 && x >= 18 && x <= 42) {
						level.setBlock(p.set(o.getX() + x, o.getY() - 1, o.getZ() + z), Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
					if (z == 100 && x >= 16 && x <= 44) {
						level.setBlock(p.set(o.getX() + x, o.getY(), o.getZ() + z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
					if (z > 100 && x >= 0 && x <= 60) {
						int edge = Math.min(x - 0, 60 - x);
						int drop = Math.min(Math.min(14, z - 97), 1 + edge);
						for (int yy = -20; yy < -drop; yy++) {
							level.setBlock(p.set(o.getX() + x, o.getY() + yy, o.getZ() + z), (yy == -drop - 1 ? Blocks.COARSE_DIRT : Blocks.STONE).defaultBlockState(),
									Block.UPDATE_CLIENTS);
						}
						for (int yy = -drop; yy <= 2; yy++) {
							level.setBlock(p.set(o.getX() + x, o.getY() + yy, o.getZ() + z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
				}
			}
			// the cut round the pad into the seed's hills shows rock, not dirt: a highwall of stone with its bands
			for (int x = -90; x <= 120; x++) {
				for (int z = -110; z <= 145; z++) {
					for (int yy = -20; yy <= 60; yy++) {
						BlockState st = level.getBlockState(p.set(o.getX() + x, o.getY() + yy, o.getZ() + z));
						if (st.is(Blocks.DIRT) || st.is(Blocks.COARSE_DIRT) && z < 60) {
							int band = Math.floorMod(yy * 3 + (x + z) / 9, 11);
							Block rock = band == 0 ? Blocks.GRANITE : band == 5 ? Blocks.ANDESITE : band == 8 ? Blocks.TUFF : Blocks.STONE;
							level.setBlock(p, rock.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
				}
			}
			at[0] = o;
		});
		o = at[0];
		log("pit site at " + o.toShortString());
	}

	private void pit() {
		pitSite();
		server.runCommand("time set " + TIME);
		// the rope shovel built on the floor's east side, where its boom lies clear, its cable from a switch house; lifted, and parked
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos sh = o.offset(56, -14, 42);
			for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
				int[] off = part.offset();
				Trailer.part(level, TailingsBlocks.plantBlock(part.machine()), sh.offset(off[0], off[1], off[2]));
			}
			Trailer.part(level, TailingsBlocks.PIT_SWITCH_HOUSE, o.offset(-4, -14, 36));
			var house = (PitSwitchHouseBlockEntity) level.getBlockEntity(o.offset(-4, -14, 36));
			house.forceSupply(true);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
			TrailingCableItem.pickUp(reel, house);
			long[] from = TrailingCableItem.from(reel);
			var crawlers = (RopeShovelBlockEntity) level.getBlockEntity(sh);
			log("shovel cable " + crawlers.couple(level, BlockPos.of(from[0]), (int) from[1]) + "; outlet " + house.close(0));
			shovelId = crawlers.machine();
			log("shovel: " + crawlers.run(level));
		});
		ticks(40);
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(shovelId) instanceof RopeShovelEntity e) {
				BlockPos sh = o.offset(56, -14, 42);
				shovelOffset = e.position().subtract(sh.getX(), sh.getY(), sh.getZ());
				log("shovel lifted at " + e.position() + ", " + shovelOffset + " from its crawlers' cell; " + e.status());
			} else {
				log("shovel not lifted");
			}
		});
		// the drill on the bench, its mast over the pattern's first hole (x 20, z -7), facing into the bench
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var drill = TailingsEntities.BLASTHOLE_DRILL.create(level, EntitySpawnReason.COMMAND);
			drill.setPos(o.getX() + 20.5, o.getY(), o.getZ() - 12.5);
			drill.setYRot(180);
			drill.setFuel(drill.tank());
			drill.fast(16);
			drill.setting("Rows", 3);
			drill.setting("Columns", 4);
			level.addFreshEntity(drill);
			log("drill: " + drill.go() + "; plan " + drill.plan());
			drillId = drill.getUUID();
		});
		int[] holes = {0};
		// 02: the drill on its pattern: close on its mast at the bench's edge as it drills its first hole and trams to the next
		// the camera over the pit's edge south-east of the drill, following it as it trams along the row
		Vec3[] seen = {null};
		film(2, "02_drill", 200, t -> {
			BlastholeDrillEntity d = entity(drillId, BlastholeDrillEntity.class);
			Vec3 at = d == null ? new Vec3(o.getX() + 20.5, o.getY(), o.getZ() - 12.5) : d.position();
			seen[0] = seen[0] == null ? at : seen[0].lerp(at, 0.08);
			Vec3 f = seen[0];
			return new Cam(f.x + 14 - t * 0.01, f.y + 7.5 + t * 0.006, f.z + 25, f.x, f.y + 8, f.z - 2);
		}, t -> {
			BlastholeDrillEntity d = entity(drillId, BlastholeDrillEntity.class);
			if (d != null && d.holesDrilled() != holes[0]) {
				holes[0] = d.holesDrilled();
				event("hole " + holes[0] + " drilled");
			}
			if (t % 20 == 0 && d != null) {
				log("drill t" + t + ": " + d.status());
			}
		});
		// the rest of the pattern, faster, unfilmed
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(drillId) instanceof BlastholeDrillEntity d) {
				d.fast(60);
			}
		});
		for (int i = 0; i < 600; i++) {
			ticks(10);
			BlastholeDrillEntity d = entity(drillId, BlastholeDrillEntity.class);
			if (d == null) {
				continue;
			}
			if (i % 20 == 0) {
				log("drill: " + d.status() + ", " + d.holesDrilled() + " holes");
			}
			if (!d.running() && (d.status().startsWith("DONE") || d.status().contains("OUT OF FUEL") || d.status().contains("BLOCKED") || d.status().contains("NOT LEVEL"))) {
				log("drill: " + d.status());
				break;
			}
		}
		// prime every hole; the emulsion truck charges them from two stands
		List<BlockPos> collars = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			List<BlockPos> found = new ArrayList<>();
			if (level.getEntity(drillId) instanceof BlastholeDrillEntity d) {
				d.setPos(o.getX() + 60.5, o.getY(), o.getZ() - 40.5);   // back on the bench, out of the shot
			}
			for (BlockPos p : BlockPos.betweenClosed(o.offset(0, -1, -40), o.offset(70, -1, -1))) {
				if (level.getBlockEntity(p) instanceof BlastholeBlockEntity hole && hole.isCollar()) {
					hole.addBooster();
					hole.addDetonator(1 + found.size());
					found.add(p.immutable());
				}
			}
			var truck = TailingsEntities.EMULSION_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 25.5, o.getY(), o.getZ() - 9.75);
			truck.setYRot(90);
			truck.setFuel(truck.tank());
			truck.setMatrix(Blasting.TRUCK_MATRIX_KG);
			truck.fastPump(10);
			level.addFreshEntity(truck);
			chargerId = truck.getUUID();
			return found;
		});
		log(collars.size() + " holes primed: " + collars);
		if (collars.size() != 12) {
			throw new AssertionError("the drill should leave 12 holes, found " + collars.size());
		}
		boolean first = true;
		for (double[] stand : new double[][] {{25.5, -9.75}, {41.5, -9.75}, {25.5, -18.25}, {41.5, -18.25}}) {
			server.runOnServer(srv -> {
				if (srv.overworld().getEntity(chargerId) instanceof EmulsionTruckEntity truck) {
					truck.setPos(o.getX() + stand[0], o.getY(), o.getZ() + stand[1]);
				}
			});
			ticks(5);
			server.runOnServer(srv -> {
				if (srv.overworld().getEntity(chargerId) instanceof EmulsionTruckEntity truck) {
					log("charging: " + truck.chargeAll(srv.overworld()));
				}
			});
			if (first) {
				// 03: the emulsion truck at its stand, its hose in the holes
				film(3, "03_charge", 120, move(cam(17, 3.5, -2.5, 25, 1.5, -9.5), cam(18, 4.5, -4.5, 26, 1.5, -10), 0, 120), null);
				first = false;
			}
			for (int t = 0; t < 120; t++) {
				ticks(10);
				EmulsionTruckEntity truck = entity(chargerId, EmulsionTruckEntity.class);
				if (truck == null || !truck.busy()) {
					break;
				}
			}
		}
		// stemmed with the cuttings, logged, armed and fired from 60 m back on the bench
		firing = o.offset(32, 0, -82);
		String armed = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (level.getEntity(chargerId) instanceof EmulsionTruckEntity truck) {
				truck.setPos(o.getX() + 75.5, o.getY(), o.getZ() - 45.5);
			}
			ItemStack logger = new ItemStack(TailingsItems.BLAST_LOGGER);
			for (BlockPos p : collars) {
				if (level.getBlockEntity(p) instanceof BlastholeBlockEntity hole) {
					hole.charge(0, level.getGameTime() - Blasting.SLEEP_TICKS);   // the film does not wait the gassing minute
					hole.stemWithCuttings();
					BlastLoggerItem.log(level, logger, p);
				}
			}
			level.setBlock(firing, TailingsBlocks.BLASTING_MACHINE.defaultBlockState(), Block.UPDATE_ALL);
			var machine = (BlastingMachineBlockEntity) level.getBlockEntity(firing);
			log("blasting machine: " + machine.load(logger));
			machine.setKey(true);
			String said = machine.arm();
			log(said + " | " + machine.fire());
			return said;
		});
		if (!armed.startsWith("ARMED: 12 of 12")) {
			throw new AssertionError("the shot should arm with all 12 holes: " + armed);
		}
		// the siren's 10 s, most of it before the film: it fires 2 s in (the film's settle and still take 35 ticks)
		ticks(Blasting.SIREN_TICKS - 85);
		String[] last = {""};
		// 01, the hook: the shot from over the pit's edge (inside the dust's 32 m), the bench and its face heaving to muck and spilling
		film(1, "01_blast", 190, move(cam(47, 4, 9, 32, -5, -10), cam(45, 4.5, 6, 32, -5, -10), 0, 190), t -> {
			String status = server.computeOnServer(srv -> srv.overworld().getBlockEntity(firing) instanceof BlastingMachineBlockEntity m ? m.status() : "gone");
			String head = status.split("[:;]")[0];
			if (!head.equals(last[0])) {
				last[0] = head;
				event(status.length() > 60 ? status.substring(0, 60) : status);
			}
		});
		ticks(60);
		log("shot: " + server.computeOnServer(srv -> srv.overworld().getBlockEntity(firing) instanceof BlastingMachineBlockEntity m ? m.status() : "gone"));
		// the muck: how far south its toe runs at the shovel's line
		int toe = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int front = 0;
			for (int x = 26; x <= 42; x++) {
				for (int z = 0; z <= 30; z++) {
					if (level.getBlockState(o.offset(x, -14, z)).is(TailingsBlocks.MUCK)) {
						front = Math.max(front, z);
					}
				}
			}
			return front;
		});
		log("muck toe at z " + toe);
		// the shovel to the muck's toe (its crawlers' cell 2 m off it, as the pit_big scene stands it), the ultra on its right
		BlockPos cell = o.offset(28, -14, toe + 3);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (level.getEntity(shovelId) instanceof RopeShovelEntity e) {
				e.setPos(cell.getX() + shovelOffset.x, cell.getY() + shovelOffset.y, cell.getZ() + shovelOffset.z);
				e.fastPasses(3);
				e.setAutoHere();
				log("shovel at " + e.position() + ": " + e.status());
			}
			UltraHaulTruckEntity truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(cell.getX() + 30.0, cell.getY(), cell.getZ() + 6.0);
			truck.setYRot(0);
			truck.setFuel(truck.tank());
			level.addFreshEntity(truck);
			truckId = truck.getUUID();
		});
		ticks(20);
		// the first pass unfilmed, so the film opens on the shovel at work
		long[] passes = {0};
		boolean[] digging = {false};
		for (int i = 0; i < 80; i++) {
			ticks(10);
			RopeShovelEntity e = entity(shovelId, RopeShovelEntity.class);
			if (e != null && i % 6 == 0) {
				log("shovel: " + e.passes() + " passes; " + e.status());
			}
			if (e != null && e.passes() >= 1 && e.digging()) {
				break;
			}
		}
		// 04: the shovel's dipper in the muck, the swing and the dump into the ultra, from the floor south-east of it
		Vec3 sp = entity(shovelId, RopeShovelEntity.class).position();
		double sx = sp.x - o.getX(), sz = sp.z - o.getZ();
		film(4, "04_load", 300, move(cam(sx + 25, -6, sz + 22, sx + 11, -5, sz - 2), cam(sx + 22, -7, sz + 19, sx + 12, -5, sz - 1), 0, 300), t -> {
			RopeShovelEntity e = entity(shovelId, RopeShovelEntity.class);
			if (e == null) {
				return;
			}
			if (e.digging() != digging[0]) {
				digging[0] = e.digging();
				event(digging[0] ? "dipper into the muck" : "dipper out, swinging");
			}
			if (e.passes() != passes[0]) {
				passes[0] = e.passes();
				event("pass " + passes[0] + " dumped into the truck");
			}
		});
		// the rest of the passes, till the ultra is full
		int[] moves = {0};
		for (int i = 0; i < 240; i++) {
			ticks(10);
			UltraHaulTruckEntity tr = entity(truckId, UltraHaulTruckEntity.class);
			RopeShovelEntity e = entity(shovelId, RopeShovelEntity.class);
			if (i % 10 == 0) {
				log("loading: " + (e != null ? e.passes() + " passes; " + e.status() : "no shovel") + "; truck " + (tr != null ? tr.load().tonnes() : "gone"));
			}
			if (tr != null && tr.load().room() < 50_000 && (e == null || !e.busy())) {
				break;
			}
			// its face worked out of reach: it trams on into the muck 4 m, the truck with it (off camera, set there)
			if (e != null && e.status().contains("worked out") && moves[0] < 6) {
				moves[0]++;
				server.runOnServer(srv -> {
					ServerLevel level = srv.overworld();
					if (level.getEntity(shovelId) instanceof RopeShovelEntity sh) {
						sh.setPos(sh.getX(), sh.getY(), sh.getZ() - 4);
						sh.setAutoHere();
					}
					if (level.getEntity(truckId) instanceof UltraHaulTruckEntity t2) {
						t2.setPos(t2.getX(), t2.getY(), t2.getZ() - 4);
					}
				});
				log("shovel trammed on 4 m into the muck (" + moves[0] + ")");
			}
		}
		String loaded = server.computeOnServer(srv -> srv.overworld().getEntity(truckId) instanceof UltraHaulTruckEntity tr ? tr.load().tonnes() : "gone");
		log("the ultra holds " + loaded);
		summary.add("the ultra loaded: " + loaded);
		// 05: loaded, it pulls away south across the floor, the camera low on the floor west of its path
		Vec3 tp = entity(truckId, UltraHaulTruckEntity.class).position();
		double tx = tp.x - o.getX(), tz = tp.z - o.getZ();
		boolean[] moving = {false};
		film(5, "05_haul", 170, t -> {
			UltraHaulTruckEntity tr = entity(truckId, UltraHaulTruckEntity.class);
			Vec3 at = tr == null ? tp : tr.position();
			double s = Math.min(1, t / 170.0);
			return new Cam(o.getX() + tx - 22, o.getY() - 11.5 + s * 1.5, o.getZ() + tz + 18 - s * 2, at.x, at.y + 4, at.z);
		}, t -> {
			if (t == 20) {
				server.runOnServer(srv -> {
					if (srv.overworld().getEntity(truckId) instanceof UltraHaulTruckEntity tr) {
						tr.testDrive(new Input(true, false, false, false, false, false, false));
					}
				});
				event("drive pressed");
			}
			UltraHaulTruckEntity tr = entity(truckId, UltraHaulTruckEntity.class);
			if (!moving[0] && tr != null && Math.abs(tr.speed()) > 0.05) {
				moving[0] = true;
				event("rolling");
			}
		});
		// the loaded ultra to the dump: set on the haul road short of the crest, it backs up to the berm
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(truckId) instanceof UltraHaulTruckEntity tr) {
				tr.testDrive(Input.EMPTY);
				tr.setPos(o.getX() + 30.0, o.getY(), o.getZ() + 84.0);
				tr.setYRot(180);
				tr.testDrive(new Input(false, true, false, false, false, false, false));
			}
		});
		for (int i = 0; i < 60; i++) {
			ticks(10);
			UltraHaulTruckEntity tr = entity(truckId, UltraHaulTruckEntity.class);
			if (tr != null && i > 2 && Math.abs(tr.speed()) < 0.01) {
				break;
			}
		}
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(truckId) instanceof UltraHaulTruckEntity tr) {
				tr.testDrive(Input.EMPTY);
				log("at the dump: z " + (tr.getZ() - o.getZ()) + ", " + tr.status() + ", over the crest " + tr.goingOver() + ", " + tr.load().tonnes());
			}
		});
		ticks(20);
		// 06: it tips over the crest, the load pouring down the dump's face; from the dump's side, low
		UltraHaulTruckEntity parked = entity(truckId, UltraHaulTruckEntity.class);
		double dz = parked.getZ() - o.getZ();
		boolean[] pouring = {false};
		film(6, "06_dump", 300, move(cam(16, 2.0, dz + 17, 29, 0, dz + 7), cam(17, 3.0, dz + 20, 29, -1, dz + 8), 0, 300), t -> {
			if (t == 10) {
				server.runOnServer(srv -> {
					if (srv.overworld().getEntity(truckId) instanceof UltraHaulTruckEntity tr) {
						tr.pressJump(srv.overworld());
					}
				});
				event("tip pressed");
			}
			UltraHaulTruckEntity tr = entity(truckId, UltraHaulTruckEntity.class);
			if (tr != null && !pouring[0] && tr.pouring() > 0) {
				pouring[0] = true;
				event("load pouring out (" + tr.pouring() + " blocks on their way)");
			}
			if (tr != null && t % 40 == 0) {
				log(String.format(Locale.ROOT, "dump t%d: lift %.2f, %d on their way, %s, %s", t, tr.lift(), tr.pouring(), tr.load().tonnes(), tr.status()));
			}
		});
	}

	// ---- the walking dragline ----

	private UUID draglineId;

	/**
	 * The dragline's site, as the dragline scene lays it, relative to o (o's y the ground's first air): grass and dirt 3 m deep over a
	 * bauxite seam; the strip x 40..49, z -52..17, its middle already uncovered; the last strip's hole east of it, x 56..78, its seam
	 * taken, where the spoil goes. The dragline built at (20, 0, 10), its switch house at (2, 0, 40).
	 */
	private void draglineSite() {
		BlockPos[] at = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -700, -300, 160, 170);
			int g = s.getY();
			BlockPos o = new BlockPos(s.getX() + 50, g, s.getZ() + 100);
			Trailer.pad(level, o.getX() - 50, o.getZ() - 100, o.getX() + 110, o.getZ() + 70, g, 18, 50);
			Block bauxite = BuiltInRegistries.BLOCK.getValue(Identifier.parse("wildcatter:bauxite_ore"));
			BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
			for (int x = -20; x <= 100; x++) {
				for (int z = -85; z <= 60; z++) {
					boolean strip = x >= 40 && x <= 49 && z >= -52 && z <= 17;
					boolean hole = x >= 56 && x <= 78 && z >= -70 && z <= 30;
					boolean dug = strip && z >= -46 && z <= 5;
					for (int yy = -10; yy <= -1; yy++) {
						BlockState st;
						if (hole && yy >= -5) {
							st = Blocks.AIR.defaultBlockState();
						} else if (strip && yy >= -3) {
							st = dug ? Blocks.AIR.defaultBlockState() : (yy == -1 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
						} else if (yy == -5 || yy == -4) {
							st = bauxite.defaultBlockState();
						} else {
							st = (yy == -1 ? Blocks.GRASS_BLOCK : yy >= -3 ? Blocks.DIRT : Blocks.STONE).defaultBlockState();
						}
						level.setBlock(p.set(o.getX() + x, o.getY() + yy, o.getZ() + z), st, Block.UPDATE_CLIENTS);
					}
				}
			}
			BlockPos dl = o.offset(20, 0, 10);
			for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
				int[] off = part.offset();
				Trailer.part(level, TailingsBlocks.plantBlock(part.machine()), dl.offset(off[0], off[1], off[2]));
			}
			Trailer.part(level, TailingsBlocks.PIT_SWITCH_HOUSE, o.offset(2, 0, 40));
			var house = (PitSwitchHouseBlockEntity) level.getBlockEntity(o.offset(2, 0, 40));
			house.forceSupply(true);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
			TrailingCableItem.pickUp(reel, house);
			long[] from = TrailingCableItem.from(reel);
			var tub = (DraglineBlockEntity) level.getBlockEntity(dl);
			log("dragline cable " + tub.couple(level, BlockPos.of(from[0]), (int) from[1]) + "; outlet " + house.close(0));
			tub.job().set(0, 10);   // a 10 m strip, 70 m long, to the seam 3 m down, casting right
			tub.job().set(1, 70);
			tub.job().set(2, 3);
			draglineId = tub.machine();
			log("dragline: " + tub.run(level));
			at[0] = o;
		});
		o = at[0];
		log("dragline site at " + o.toShortString());
	}

	private Dragline.Phase phase() {
		DraglineEntity d = entity(draglineId, DraglineEntity.class);
		return d == null ? null : d.phase();
	}

	private void fast(int divisor) {
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(draglineId) instanceof DraglineEntity d) {
				d.fastCycles(divisor);
			}
		});
	}

	/** Runs until the dragline's phase is {@code want} (or a minute of game time passes). */
	private void until(Dragline.Phase want) {
		for (int i = 0; i < 1200; i++) {
			if (phase() == want) {
				return;
			}
			context.waitTick();
		}
		log("never reached " + want + "; now " + phase());
	}

	/** Logs every phase change as an event of the film running. */
	private IntConsumer phases() {
		Dragline.Phase[] last = {null};
		return t -> {
			Dragline.Phase now = phase();
			if (now != last[0]) {
				last[0] = now;
				event(String.valueOf(now));
			}
		};
	}

	private void dragline() {
		draglineSite();
		server.runCommand("time set " + TIME);
		ticks(40);
		// for scale: an ultra truck parked west of the tub, its nose to the camera
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			UltraHaulTruckEntity truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 6.0, o.getY(), o.getZ() + 30.0);
			truck.setYRot(90);
			truck.setFuel(truck.tank());
			level.addFreshEntity(truck);
			truckId = truck.getUUID();
		});
		DraglineEntity d0 = entity(draglineId, DraglineEntity.class);
		if (d0 == null) {
			throw new AssertionError("the dragline did not lift");
		}
		Vec3 c = d0.position();
		double cx = c.x - o.getX(), cz = c.z - o.getZ();
		log("dragline lifted at " + c + " (" + cx + ", " + cz + " from o); " + d0.status());
		// cycles at a fifth of their real time: about 11 s a cycle
		fast(5);
		until(Dragline.Phase.SWING_BACK);
		until(Dragline.Phase.FILL);
		// 02: the bucket dragged in through the overburden toward the tub, close over the strip's near end; cycles at half time
		fast(2);
		until(Dragline.Phase.SWING_BACK);
		until(Dragline.Phase.FILL);
		film(2, "02_drag", 140, move(cam(53, 2.5, 19, 44.5, -1.5, 11), cam(52, 2.2, 17, 44.5, -1.5, 10), 0, 140), phases());
		fast(5);
		until(Dragline.Phase.SWING_OUT);
		// 01, the hook: the bucket swung out over the spoil and dumped, the spoil pouring; from the old strip's floor, looking up
		BlockPos dump = server.computeOnServer(srv -> srv.overworld().getEntity(draglineId) instanceof DraglineEntity d ? d.dumpAt() : null);
		if (dump == null) {
			dump = o.offset(64, -5, 12);
		}
		double ux = dump.getX() - o.getX(), uy = dump.getY() - o.getY(), uz = dump.getZ() - o.getZ();
		log("casting at " + ux + ", " + uy + ", " + uz + " from o");
		film(1, "01_cast", 140, move(cam(ux - 12, -3.5, uz + 14, ux, uy + 6, uz), cam(ux - 11, -3.8, uz + 12, ux, uy + 6, uz), 0, 140), phases());
		until(Dragline.Phase.FILL);
		until(Dragline.Phase.SWING_OUT);
		// 03: the swing: the whole machine wide from the north-east, boom coming round from the strip to the spoil
		film(3, "03_swing", 140, move(cam(cx + 55, 22, cz - 55, cx + 10, 12, cz), cam(cx + 48, 20, cz - 60, cx + 10, 12, cz), 0, 140), phases());
		until(Dragline.Phase.FILL);
		// 04: low by the tub, looking up the boom as it swings out
		until(Dragline.Phase.SWING_OUT);
		film(4, "04_low", 140, move(cam(cx + 12, 1.2, cz + 15, cx + 32, 24, cz - 6), cam(cx + 13, 1.2, cz + 13, cx + 34, 24, cz - 8), 0, 140), phases());
		// 06: for scale, the ultra truck parked beside the tub, both seen from far off to the south; a slow rise
		film(6, "06_scale", 160, move(cam(17, 3, 88, 17, 10, 22), cam(17, 7, 84, 17, 11, 22), 0, 160), phases());
		// on fast till its first step
		fast(14);
		for (int i = 0; i < 2400; i++) {
			DraglineEntity d = entity(draglineId, DraglineEntity.class);
			if (d == null || d.walking() || d.status().startsWith("DONE")) {
				break;
			}
			if (i % 200 == 0) {
				log("dragline: " + d.mode() + "; " + d.status());
			}
			context.waitTick();
		}
		DraglineEntity w = entity(draglineId, DraglineEntity.class);
		if (w == null || !w.walking()) {
			log("it never walked: " + (w == null ? "gone" : w.status()));
			return;
		}
		// it walks: from the start of its next step, a whole step at a sixth of its real time
		fast(6);
		for (int i = 0; i < 1200; i++) {
			DraglineEntity d = entity(draglineId, DraglineEntity.class);
			if (d == null || d.walkPhase() == 0) {
				break;
			}
			context.waitTick();
		}
		for (int i = 0; i < 1200; i++) {
			DraglineEntity d = entity(draglineId, DraglineEntity.class);
			if (d == null || d.walkPhase() > 0) {
				break;
			}
			context.waitTick();
		}
		w = entity(draglineId, DraglineEntity.class);
		Vec3 wc = w.position();
		double wx = wc.x - o.getX(), wz = wc.z - o.getZ();
		float[] lastPhase = {-1};
		// 05: low beside its shoes as they lift, carry it 2 m and set down
		film(5, "05_walk", 230, move(cam(wx - 40, 1.0, wz - 8, wx - 4, 2.0, wz), cam(wx - 38, 1.1, wz - 11, wx - 4, 2.0, wz - 2), 0, 230), t -> {
			DraglineEntity d = entity(draglineId, DraglineEntity.class);
			if (d == null) {
				return;
			}
			float wp = d.walkPhase();
			if (lastPhase[0] < 0 || (wp == 0) != (lastPhase[0] == 0)) {
				event(wp > 0 ? String.format(Locale.ROOT, "stepping (walk phase %.2f, %.1f m walked)", wp, d.walked()) : String.format(Locale.ROOT, "standing (%.1f m walked)", d.walked()));
			}
			lastPhase[0] = wp;
		});
		DraglineEntity end = entity(draglineId, DraglineEntity.class);
		summary.add("dragline: " + (end == null ? "gone" : end.steps() + " steps, " + end.cycles() + " cycles, " + end.status()));
	}
}
