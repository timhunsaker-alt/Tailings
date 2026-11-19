package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.control.ControlNet;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.plc.lang.Project;
import dev.wildcatter.plc.lang.Sfc;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.plant.Machine;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The PLC shorts' {@code belts} footage (TAILINGS_ONLY=plcBelts): a sequential function chart starts the pit line's crushing circuit
 * last to first, as a real plant does: horn and amber, then the stockpile belt, the transfer belt, the gyratory, its apron feeder, and
 * green with the line up. The chart is a real SFC unit in a real cabinet, downloaded and run from its screen; the machines are the
 * real ones on Wildcatter's control cable. World shots are filmed in a portrait window, the chart in the landscape one.
 * PLC_BELTS_SHOTS=1,3 films only those; stills go to PLC_BELTS_OUT, films to {@code fabric/build/scene-videos/plcBelts_*.mov}.
 * <ol>
 * <li>{@code hook}: the line already running loaded, low along the transfer belt as it climbs;</li>
 * <li>{@code chart}: the chart's steps lighting one after another (screen);</li>
 * <li>{@code warn}: the stack light to amber with the horn, then the stockpile belt starting behind it;</li>
 * <li>{@code belt1}: the transfer belt starting, seen down its grade to the crusher;</li>
 * <li>{@code crusher}: close on the feeder's head end as its pans start, then up to the gyratory over it;</li>
 * <li>{@code lineup}: the stack light to green, rising to the whole line running.</li>
 * </ol>
 * The motors have no grid in the scene, so the test runs each machine while the PLC's own output for it is on: the order and timing on
 * screen are the chart's. With no grid there is no running feedback, so the chart's transitions are timed, not proved.
 */
final class PlcBelts {
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final Path OUT = Path.of(System.getenv().getOrDefault("PLC_BELTS_OUT", "plc-belts")).toAbsolutePath();
	private static final int PW = Integer.parseInt(System.getenv().getOrDefault("PLC_BELTS_PW", "486"));
	private static final int PH = Integer.parseInt(System.getenv().getOrDefault("PLC_BELTS_PH", "864"));
	private static final long TIME = Long.parseLong(System.getenv().getOrDefault("PLC_BELTS_TIME", "12350"));
	private static final String UNIT = "BELTS";
	/**
	 * PLC_BELTS_FRAMES=1 saves each film a frame a tick ({@code <PLC_BELTS_OUT>/frames/<id>/}, 20 a second) instead of recording the
	 * screen: for a locked screen, which records only itself. The game waits for each frame, so the film runs at game speed.
	 */
	private static final boolean FRAMES = "1".equals(System.getenv("PLC_BELTS_FRAMES"));
	/** The chart's steps in order, and when each comes on after start, for the log. */
	private static final String[] STEPS = {"IDLE", "WARN", "BELT_2", "BELT_1", "CRUSH", "FEED", "LINE_UP", "STOP"};

	private final ClientGameTestContext context;
	private TestServerContext server;
	private final Map<BlockPos, String> runs = new LinkedHashMap<>();
	private BlockPos cabinet;
	private BlockPos startPb;
	private BlockPos stopPb;
	private BlockPos o;
	private UltraHaulTruckEntity truck;
	private final Map<String, Integer> stepY = new LinkedHashMap<>();
	private double scroll;
	private boolean portrait;

	PlcBelts(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[tailings-plc-belts] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("PLC_BELTS_SHOTS");
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
		portrait();
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
				stage();
				program();
				films();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
			} finally {
				close();
			}
			log("done");
		}
	}

	// ---- windows, camera, stills and films ----

	private void portrait() {
		if (!portrait) {
			context.getInput().resizeWindow(PW, PH);
			context.waitTicks(10);
			portrait = true;
		}
	}

	private void landscape() {
		if (portrait) {
			context.getInput().resizeWindow(1440, 810);
			context.waitTicks(10);
			portrait = false;
		}
	}

	/** The camera, its eye at the cam's point, relative to {@code o}. */
	private void put(Cam c) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y(), dz = c.tz() - c.z();
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y() - 1.62, c.z(), yaw, pitch));
	}

	private Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return Cam.of(o, x, y, z, tx, ty, tz);
	}

	/** The machines follow the PLC's outputs for them: each runs while its output is on. */
	private void follow() {
		if (runs.isEmpty() || cabinet == null) {
			return;
		}
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (!(level.getBlockEntity(cabinet) instanceof PlcCabinetBlockEntity box) || box.cpu() == null) {
				return;
			}
			for (Map.Entry<BlockPos, String> run : runs.entrySet()) {
				if (level.getBlockEntity(run.getKey()) instanceof PlantBlockEntity part) {
					if (Boolean.TRUE.equals(box.cpu().read(run.getValue()))) {
						part.forceRunning();
					} else {
						part.stopForced();
					}
				}
			}
		});
	}

	private void ticks(int n) {
		for (int t = 0; t < n; t++) {
			follow();
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
		ticks(10);
	}

	private void still(String name) {
		server.runCommand("kill @e[type=item]");
		TestScreenshotOptions options = TestScreenshotOptions.of(name).withDestinationDir(OUT).disableCounterPrefix();
		if (portrait) {
			options = options.withSize(1080, 1920);
		}
		Path at = context.takeScreenshot(options);
		log("still " + at);
	}

	/** One frame of a film saved a frame a tick. */
	private void frame(String id, int t) {
		TestScreenshotOptions options = TestScreenshotOptions.of(String.format(Locale.ROOT, "f%04d", t)).withDestinationDir(OUT.resolve("frames").resolve(id))
				.disableCounterPrefix();
		if (portrait) {
			options = options.withSize(972, 1728);
		}
		context.takeScreenshot(options);
	}

	/** The step that is on now, or null. */
	private String active() {
		return server.computeOnServer(srv -> {
			var box = (PlcCabinetBlockEntity) srv.overworld().getBlockEntity(cabinet);
			if (box.cpu() == null) {
				return null;
			}
			for (String step : STEPS) {
				if (Boolean.TRUE.equals(box.cpu().read(UNIT + "." + step + ".X"))) {
					return step;
				}
			}
			return null;
		});
	}

	/**
	 * A film: the start still, then {@code length} ticks filmed, the camera eased from a to b. The start-up is pressed {@code lead}
	 * ticks before the film starts (negative: that many ticks into it), and every step change is logged at its second in the film.
	 */
	private void film(String id, Cam a, Cam b, int length, int lead, Runnable each) {
		film(id, a, b, length, lead, each, 0, length);
	}

	/** As above, the camera held at a until {@code from} and eased to b by {@code to}. */
	private void film(String id, Cam a, Cam b, int length, int lead, Runnable each, int from, int to) {
		if (a != null) {
			put(a);
		}
		settle(60);
		if (lead >= 0) {
			still(id + "_a");
			press(startPb);
			ticks(lead);
		} else {
			still(id + "_a");
		}
		String last = active();
		List<String> events = new ArrayList<>();
		Filming film = FRAMES ? null : context.computeOnClient(client -> Filming.start(client, "plcBelts_" + id));
		long started = System.nanoTime();
		for (int t = 0; t <= length; t++) {
			double s = Math.max(0, Math.min(1, (double) (t - from) / (to - from)));
			if (a != null && b != null) {
				put(a.lerp(b, s * s * (3 - 2 * s)));
			}
			if (lead < 0 && t == -lead) {
				press(startPb);
				events.add(String.format(Locale.ROOT, "start pressed at %.2f s (tick %d)", t / 20.0, t));
			}
			if (each != null) {
				each.run();
			}
			follow();
			context.waitTick();
			if (FRAMES) {
				frame(id, t);
			}
			String now = active();
			if (now != null && !now.equals(last)) {
				events.add(String.format(Locale.ROOT, "%s at %.2f s (tick %d, wall %.2f s)", now, (t + 1) / 20.0, t + 1, (System.nanoTime() - started) / 1e9));
				last = now;
			}
			if (t % 20 == 0) {
				server.runCommand("kill @e[type=item]");
			}
		}
		if (film != null) {
			film.stop();
		}
		log("film " + id + ": " + length + " ticks, wall " + String.format(Locale.ROOT, "%.2f", (System.nanoTime() - started) / 1e9) + " s; events " + events);
	}

	// ---- the site: the pit line, its cabinet and its buttons ----

	private void stage() {
		BlockPos[] at = new BlockPos[1];
		PlantBlockEntity[][] groups = new PlantBlockEntity[4][];
		BlockPos[] devices = new BlockPos[2];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -400, 300, 110, 170);
			int g = s.getY();
			BlockPos o = new BlockPos(s.getX() + 40, g, s.getZ() + 60);
			Trailer.pad(level, o.getX() - 45, o.getZ() - 60, o.getX() + 70, o.getZ() + 110, g, 16, 45);
			// the trucks' bench and the gyratory in it, as the PLC trailer's plant
			Trailer.hill(level, o.getX() - 40, o.getZ() - 60, o.getX() + 40, o.getZ() - 1, g, (x, z) -> {
				int dx = Math.max(Math.max(o.getX() - 2 - x, x - (o.getX() + 15)), 0);
				int dz = Math.max(z - (o.getZ() - 3), 0);
				int north = z - (o.getZ() - 60);
				return Math.min(26 - Math.max(dx, dz) * 2, north);
			}, true);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 25, -40, -3);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 21, -2, -1);
			Trailer.fillBox(level, o, Blocks.GRAVEL, -2, 15, 25, 25, -40, -3);
			PlantBlockEntity feeder = Trailer.plant(level, Machine.PRIMARY_APRON_FEEDER, o.offset(5, 0, 3));
			Trailer.plant(level, Machine.SURGE_POCKET, o.offset(3, 2, 1));
			PlantBlockEntity crusher = Trailer.plant(level, Machine.GYRATORY_CRUSHER, o.offset(4, 8, 2));
			Trailer.part(level, TailingsBlocks.GYRATORY_POCKET, o.offset(0, 18, 0));
			Trailer.plant(level, Machine.ROCK_BREAKER, o.offset(12, 26, 2));
			Trailer.part(level, TailingsBlocks.TRUCK_DUMP_WALL, o.offset(1, 22, -2));
			// belt 1 south off the feeder, climbing 1 in 3 to a transfer 10 m up; belt 2 west from under its head, on up to the stacker
			int by = 0;
			for (int z = 15; z <= 45; z++) {
				if (z > 15 && (z - 15) % 3 == 0) {
					by++;
				}
				level.setBlock(o.offset(6, by, z), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			PlantBlockEntity driveA = Trailer.plant(level, Machine.HEAVY_CONVEYOR_DRIVE, o.offset(7, by, 43));
			int y2 = by - 1;
			for (int x = 6; x >= -18; x--) {
				level.setBlock(o.offset(x, y2 + (6 - x) / 3, 45), Trailer.heavyBelt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			int top = y2 + 8;
			PlantBlockEntity driveB = Trailer.plant(level, Machine.HEAVY_CONVEYOR_DRIVE, o.offset(-18, top, 46));
			Trailer.plant(level, Machine.STOCKPILE_HEAD, o.offset(-21, top - 4, 44));
			BlockPos tip = o.offset(-20, 40, 47);
			for (int i = 0; i < 500; i++) {
				dev.wildcatter.tailings.pit.Loose.muck(level, tip, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), Map.of("minecraft:copper_ore", 3));
			}
			var truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 7.0, o.getY() + 26, o.getZ() - 3.0 - truck.spec().length() / 2);
			truck.setYRot(180);
			truck.setFuel(truck.tank());
			truck.load().add("minecraft:copper_ore", 363_000);
			truck.refreshLoad();
			level.addFreshEntity(truck);
			this.truck = truck;
			// started last to first: the stockpile belt, the transfer belt, the gyratory, its feeder
			groups[0] = new PlantBlockEntity[] {driveB};
			groups[1] = new PlantBlockEntity[] {driveA};
			groups[2] = new PlantBlockEntity[] {crusher};
			groups[3] = new PlantBlockEntity[] {feeder};
			// the cabinet on the ground south of the transfer, its door and lamps to the south; start and stop on its west side
			cabinet = o.offset(13, 1, 44);
			Set<BlockPos> laid = new HashSet<>();
			BlockPos box = PlcTrailer.rig(level, cabinet, Direction.EAST, Direction.SOUTH, devices, laid);
			startPb = box.relative(Direction.WEST);
			stopPb = box.above().relative(Direction.WEST);
			level.setBlock(startPb.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(startPb, PlcTrailer.facing(PlcTrailer.wc("pushbutton"), Direction.SOUTH), Block.UPDATE_ALL);
			level.setBlock(stopPb, PlcTrailer.facing(PlcTrailer.wc("pushbutton"), Direction.SOUTH), Block.UPDATE_ALL);
			Set<BlockPos> net = new HashSet<>(List.of(box, box.above()));
			BlockPos lo = o.offset(-45, -8, -45), hi = o.offset(70, 30, 100);
			List<BlockPos> all = new ArrayList<>();
			for (PlantBlockEntity[] group : groups) {
				for (PlantBlockEntity part : group) {
					PlcTrailer.route(level, part.getBlockPos(), net, laid, g, lo, hi);
					all.add(part.getBlockPos());
				}
			}
			PlcTrailer.joints(level, laid);
			all.addAll(List.of(devices));
			all.add(startPb);
			all.add(stopPb);
			var found = ControlNet.walk(level, BigParts.cellsAt(level, cabinet));
			List<BlockPos> missed = new ArrayList<>();
			for (BlockPos p : all) {
				if (!found.devices().contains(ControlNet.mainOf(level, p))) {
					missed.add(p);
				}
			}
			log("staged: " + laid.size() + " cells of control cable; the cabinet reaches " + found.devices().size() + " devices; missed " + missed
					+ (found.badJoint() != null ? ", bad joint at " + found.badJoint().toShortString() : ""));
			if (!missed.isEmpty()) {
				throw new AssertionError("missed " + missed);
			}
			at[0] = o;
		});
		o = at[0];
		String wired = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var box = (PlcCabinetBlockEntity) level.getBlockEntity(cabinet);
			box.seat(PlcModuleItem.Type.CPU_SMALL);
			box.seat(PlcModuleItem.Type.DI16);
			box.seat(PlcModuleItem.Type.DO16);
			PlcTrailer.land(box, startPb, 0, "%IX0.0");
			PlcTrailer.land(box, stopPb, 0, "%IX0.1");
			String[] outs = {"%QX16.0", "%QX16.1", "%QX16.2", "%QX16.3"};
			for (int i = 0; i < groups.length; i++) {
				BlockPos part = groups[i][0].getBlockPos();
				PlcTrailer.land(box, part, 0, outs[i]);
				runs.put(part, outs[i]);
				PlcTrailer.auto(level, part);
			}
			PlcTrailer.land(box, devices[0], 0, "%QX16.4");
			PlcTrailer.land(box, devices[1], 0, "%QX16.5");
			PlcTrailer.land(box, devices[1], 1, "%QX16.6");
			PlcTrailer.land(box, devices[1], 2, "%QX16.7");
			return "ok";
		});
		log("wired: " + wired);
	}

	// ---- the chart ----

	private static final String VARS = """
			VAR
				start_pb AT %IX0.0 : BOOL;
				stop_pb AT %IX0.1 : BOOL;
				belt2 AT %QX16.0 : BOOL;
				belt1 AT %QX16.1 : BOOL;
				crusher AT %QX16.2 : BOOL;
				feeder AT %QX16.3 : BOOL;
				horn AT %QX16.4 : BOOL;
				red AT %QX16.5 : BOOL;
				amber AT %QX16.6 : BOOL;
				green AT %QX16.7 : BOOL;
			END_VAR""";

	/** The start-up chart, laid out as the editor lays out a chart drawn step by step. */
	private String chart() {
		Sfc sfc = new Sfc();
		sfc.kind = "PROGRAM";
		sfc.name = UNIT;
		sfc.declarations = VARS;
		String[][][] acts = {
				{{"N", "red"}},
				{{"N", "horn"}, {"S", "amber"}},
				{{"S", "belt2"}},
				{{"S", "belt1"}},
				{{"S", "crusher"}},
				{{"S", "feeder"}},
				{{"R", "amber"}, {"N", "green"}},
				{{"R", "feeder"}, {"R", "crusher"}, {"R", "belt1"}, {"R", "belt2"}}};
		String[] after = {"start_pb", "WARN.T >= T#2s", "BELT_2.T >= T#1500ms", "BELT_1.T >= T#1500ms", "CRUSH.T >= T#1500ms",
				"FEED.T >= T#1500ms", "stop_pb", "STOP.T >= T#1s"};
		int y = 24;
		int id = 1;
		List<Sfc.Step> steps = new ArrayList<>();
		List<Sfc.Transition> trans = new ArrayList<>();
		for (int i = 0; i < STEPS.length; i++) {
			Sfc.Step step = new Sfc.Step(id++, STEPS[i], i == 0, 8, y);
			for (String[] a : acts[i]) {
				step.actions.add(new Sfc.Association(a[0], a[1], ""));
			}
			stepY.put(STEPS[i], y);
			int rows = Math.max(0, step.actions.size() * 11 - 18);
			Sfc.Transition t = new Sfc.Transition(id++, after[i], 8, y + 18 + rows + 12);
			t.from.add(step.id);
			steps.add(step);
			trans.add(t);
			y = t.y + 10 + 12;
		}
		for (int i = 0; i < trans.size(); i++) {
			trans.get(i).to.add(steps.get((i + 1) % steps.size()).id);
		}
		sfc.steps.addAll(steps);
		sfc.transitions.addAll(trans);
		return sfc.write();
	}

	private dev.wildcatter.client.PlcScreen plcScreen(net.minecraft.client.Minecraft client) {
		return (dev.wildcatter.client.PlcScreen) client.gui.screen();
	}

	private void mouse(int[] at) {
		if (at == null) {
			log("nothing to click");
			return;
		}
		double toWindow = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
		context.getInput().setCursorPos(at[0] * toWindow, at[1] * toWindow);
		context.waitTick();
		context.getInput().pressMouse(1);
		context.waitTick();
	}

	private void click(String label) {
		int[] at = context.computeOnClient(client -> plcScreen(client).buttonForTest(label));
		if (at == null) {
			log("no " + label + " button");
		}
		mouse(at);
	}

	private void open() {
		put(cam(13.5, 2.6, 48.5, 13.2, 1.8, 44.5));
		ticks(5);
		server.runOnServer(srv -> ((PlcCabinetBlockEntity) srv.overworld().getBlockEntity(cabinet)).open(srv.getPlayerList().getPlayers().get(0)));
		ticks(20);
	}

	private void close() {
		context.runOnClient(client -> {
			if (client.gui.screen() != null) {
				client.gui.screen().onClose();
			}
		});
		context.waitTicks(5);
	}

	private void press(BlockPos device) {
		server.runOnServer(srv -> ((dev.wildcatter.control.OperatorDeviceBlockEntity) srv.overworld().getBlockEntity(device)).operate());
	}

	private String read() {
		return server.computeOnServer(srv -> {
			var be = srv.overworld().getBlockEntity(cabinet);
			return be instanceof dev.wildcatter.energy.Inspectable i ? i.inspect().getString() : String.valueOf(be);
		});
	}

	/** The chart into the screen as its own unit, the empty MAIN gone, downloaded and run from the screen's own buttons. */
	private void program() {
		String text = chart();
		log("the chart:\n" + text);
		landscape();
		server.runCommand("time set " + TIME);
		open();
		context.runOnClient(client -> plcScreen(client).loadUnitForTest(Project.Language.SFC, UNIT, text));
		context.waitTicks(3);
		log("units: " + context.computeOnClient(client -> plcScreen(client).unitNamesForTest()));
		if (context.computeOnClient(client -> plcScreen(client).unitNamesForTest()).contains("MAIN")) {
			context.runOnClient(client -> plcScreen(client).deleteUnitForTest("MAIN"));
			context.waitTicks(3);
		}
		context.runOnClient(client -> plcScreen(client).selectUnitForTest(UNIT));
		context.waitTicks(3);
		click("DOWNLOAD");
		ticks(20);
		click("RUN");
		ticks(30);
		context.runOnClient(client -> plcScreen(client).deselectForTest());
		context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
		ticks(10);
		log("chart: errors " + context.computeOnClient(client -> plcScreen(client).errorsForTest().toString()) + " | live "
				+ context.computeOnClient(client -> plcScreen(client).liveForTest()) + " | " + read() + " | active " + active());
		if (!"IDLE".equals(active())) {
			throw new AssertionError("the chart should be running and idle, not " + active());
		}
	}

	/** Eases the chart's view to keep the step that is on a little below the top. */
	private void follow(double rate) {
		String now = active();
		if (now == null) {
			return;
		}
		double target = Math.max(0, stepY.get(now) - 46);
		scroll += (target - scroll) * rate;
		int s = (int) Math.round(scroll);
		context.runOnClient(client -> plcScreen(client).sfcScrollForTest(s));
	}

	private void stopLine() {
		press(stopPb);
		ticks(60);
		log("stopped: active " + active());
	}

	/** Load the belts: ore into the dump pocket over the gyratory, the line runs a while, and stops where it stands, loaded. */
	private void load(int ticks) {
		int put = server.computeOnServer(srv -> {
			var pocket = BigParts.entity(srv.overworld(), o.offset(0, 18, 0));
			int n = 0;
			if (pocket instanceof dev.wildcatter.tailings.block.DumpBlockEntity dump) {
				while (n < 4000 && dump.offer(Map.of("minecraft:copper_ore", 50))) {
					n += 50;
				}
			}
			return n;
		});
		log("pocket: " + put + " blocks put in");
		press(startPb);
		ticks(ticks);
		log("loading: active " + active() + " | " + status());
		stopLine();
	}

	private String status() {
		return server.computeOnServer(srv -> {
			StringBuilder line = new StringBuilder();
			for (BlockPos p : runs.keySet()) {
				if (srv.overworld().getBlockEntity(p) instanceof PlantBlockEntity part) {
					line.append(part.machine().name()).append(' ').append(part.running() ? "running" : "stopped").append(" (").append(part.status()).append("); ");
				}
			}
			return line.toString();
		});
	}

	private void films() {
		// the chart on screen first, while it is open: the steps lighting in turn from the start button
		if (wanted(2)) {
			load(700);
			scroll = 0;
			context.runOnClient(client -> plcScreen(client).sfcScrollForTest(0));
			ticks(20);
			still("02_chart_a");
			film("02_chart", null, null, 230, -20, () -> follow(0.12));
			ticks(40);
			stopLine();
		}
		close();
		portrait();
		server.runCommand("time set " + TIME);
		// a first load so every world shot starts on loaded belts
		load(700);
		if (wanted(3)) {
			// close on the stack light as it goes amber with the horn, then up to the stockpile belt as it starts
			film("03_warn", cam(14.2, 2.7, 48.3, 12.4, 2.4, 45.5), cam(11, 14.5, 51, -3, 12, 45), 170, -30, null, 52, 90);
			ticks(40);
			stopLine();
		}
		if (wanted(4)) {
			load(700);
			// down the transfer belt's grade to the gyratory, the drive beside the camera
			film("04_belt1", cam(9, 9, 34, 6, 3, 20), cam(9, 9.5, 31, 6, 2.5, 16), 130, 30, null);
			ticks(40);
			stopLine();
		}
		if (wanted(5)) {
			load(700);
			// close on the feeder's head end as its pans start to travel, then up and back to the gyratory over it
			film("05_crusher", cam(9.6, 2.9, 17.6, 6.5, 1.4, 11.5), cam(11.5, 6, 22, 5.5, 5, 7), 170, 70, null, 100, 170);
			ticks(40);
			stopLine();
		}
		if (wanted(6) || wanted(1)) {
			load(700);
			if (wanted(6)) {
				// close on the stack light as it goes green, then up and back over the whole line running
				film("06_lineup", cam(14.6, 3.0, 48.2, 12.5, 2.5, 45.5), cam(22, 24, 64, 2, 6, 28), 210, 120, null, 56, 210);
			} else {
				press(startPb);
				ticks(200);
			}
			ticks(200);
			if (wanted(1)) {
				// the hook: the line already up and loaded, low along the transfer belt as it climbs to the transfer
				film("01_hook", cam(10, 5, 22, 6, 7.5, 38), cam(9, 7.5, 30, 6, 10.5, 44), 150, -1000, null);
			}
			log("line: active " + active() + " | " + status());
			stopLine();
		}
	}
}
