package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlock;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.BlastingMachineBlockEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.BlastLoggerItem;
import dev.wildcatter.tailings.pit.Blasting;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Checks for the Heavy Iron fixes (TAILINGS_ONLY=hifix; HIFIX_SHOTS=blast,dozer,... picks): stills of each fixed machine, to look at
 * before committing. Stills go to HIFIX_OUT (default {@code hifix}). Not films: the episodes' own scene classes film them.
 */
final class HiFixScenes {
	private static final int W = 1440, H = 810;
	private static final Path OUT = Path.of(System.getenv().getOrDefault("HIFIX_OUT", "hifix")).toAbsolutePath();

	private final ClientGameTestContext context;
	private TestServerContext server;

	HiFixScenes(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[hifix] " + line);
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HIFIX_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(12);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.bobView().set(false);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
			client.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
		});
		context.getInput().resizeWindow(W, H);
		context.waitTicks(10);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed("tailings");
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule do_mob_spawning false");
			server.runCommand("weather clear");
			server.runCommand("time set 6000");
			server.runCommand("gamemode spectator @p");
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			try {
				if (wanted("blast")) {
					blast();
				}
				extra();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	/** Later fixes' stills. */
	private void extra() {
		if (wanted("dozer")) {
			dozer();
		}
		if (wanted("tbm")) {
			tbm();
		}
		if (wanted("bore")) {
			bore();
		}
		if (wanted("cage")) {
			cage();
		}
		if (wanted("raise")) {
			raise();
		}
	}

	// ---- the raise borer (shaft-and-hoist.md, L4b) ------------------------------------------------------------------------------

	/** A raise borer on a pad over a drive 24 m down, at 20 times the real rate; its section cut open on the east to see the raise. */
	private void raise() {
		BlockPos b = new BlockPos(-300, 120, 1500);
		int depth = 24;
		cam(Cam.of(b, 20, 6, 16, 2, 2, 3));
		settle(20);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			fill(level, b, Blocks.AIR, -15, 25, -depth - 6, 12, -15, 25);
			fill(level, b, Blocks.STONE, -6, 8, -depth - 5, -1, -6, 12);
			fill(level, b, Blocks.AIR, -6, 8, -depth - 2, -depth, -6, 12);   // the drive, 3 m high, under it
			fill(level, b, Blocks.AIR, 6, 8, -depth - 5, -1, -6, 12);        // the section's open side
			var borer = Trailer.plant(level, dev.wildcatter.tailings.plant.Machine.RAISE_BORER, b);
			borer.forceRunning();
			borer.fastCycle(20);
		});
		context.waitTicks(60);
		cam(Cam.of(b, 24, -8, 3, 2, -12, 3));
		settle(10);
		still("raise_01_pilot");
		context.waitTicks(500);
		server.runOnServer(srv -> log("raise borer: " + ((dev.wildcatter.tailings.block.PlantBlockEntity) srv.overworld().getBlockEntity(b)).status()));
		still("raise_02_reaming");
		for (int k = 0; k < 40; k++) {
			context.waitTicks(40);
			boolean done = server.computeOnServer(srv -> ((dev.wildcatter.tailings.block.PlantBlockEntity) srv.overworld().getBlockEntity(b)).raise().phase()
					== dev.wildcatter.tailings.shaft.RaiseBoring.Phase.DONE);
			if (done) {
				break;
			}
		}
		server.runOnServer(srv -> log("raise borer: " + ((dev.wildcatter.tailings.block.PlantBlockEntity) srv.overworld().getBlockEntity(b)).status()));
		cam(Cam.of(b, 24, -10, 3, 2, -12, 3));
		settle(10);
		still("raise_03_done_section");
		cam(Cam.of(b, 2.5, -depth + 1.6, 9, 2.5, -2, 3.5));
		settle(10);
		still("raise_04_from_the_drive");
		cam(Cam.of(b, 16, 8, 14, 2, 2, 3));
		settle(10);
		still("raise_05_borer");
	}

	private static final net.minecraft.world.entity.player.Input FORWARD = new net.minecraft.world.entity.player.Input(true, false, false, false, false, false, false);

	// ---- fix 2: the dozer on the ground, its tracks running, spreading a lift ----------------------------------------------------

	private void dozer() {
		BlockPos o = new BlockPos(-300, 100, 400);
		java.util.UUID[] ids = new java.util.UUID[3];
		cam(Cam.of(o, 22, 6, 20, 6, 3, 18));   // the player there first: a dozer spawned in an unloaded chunk loses its test drive
		settle(20);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			fill(level, o, Blocks.AIR, -20, 60, 0, 20, -20, 60);
			fill(level, o, Blocks.STONE, -20, 60, -4, -1, -20, 60);
			// a ramp up the x axis: a block every 3 m (a 1 in 3), then a flat top
			for (int k = 0; k < 6; k++) {
				fill(level, o, Blocks.STONE, 0, 12, 0, k, 10 + 3 * k, 60);
			}
			// a 1-block kerb under the left track of a parked dozer
			fill(level, o, Blocks.STONE, 30, 32, 0, 0, 0, 12);
			// a 1 m lift of dry tails to spread on from
			for (int x = 40; x <= 56; x++) {
				for (int z = 0; z <= 14; z++) {
					level.setBlock(o.offset(x, 0, z), dev.wildcatter.tailings.TailingsBlocks.DRY_TAILS.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			var climb = dev.wildcatter.tailings.TailingsEntities.TRACK_DOZER.create(level, EntitySpawnReason.COMMAND);
			climb.setPos(o.getX() + 6.5, o.getY(), o.getZ() + 4);
			climb.setYRot(0);
			climb.setBlade(dev.wildcatter.tailings.entity.TrackDozerEntity.Blade.RAISED);
			climb.setFuel(climb.tank());
			level.addFreshEntity(climb);
			climb.testDrive(FORWARD);
			var kerb = dev.wildcatter.tailings.TailingsEntities.TRACK_DOZER.create(level, EntitySpawnReason.COMMAND);
			kerb.setPos(o.getX() + 32.6, o.getY() + 0.0, o.getZ() + 6);
			kerb.setYRot(0);
			kerb.setFuel(kerb.tank());
			level.addFreshEntity(kerb);
			var spread = dev.wildcatter.tailings.TailingsEntities.TRACK_DOZER.create(level, EntitySpawnReason.COMMAND);
			spread.setPos(o.getX() + 48.5, o.getY() + 1, o.getZ() + 6);
			spread.setYRot(0);
			spread.setFuel(spread.tank());
			level.addFreshEntity(spread);
			spread.loadBlade(dev.wildcatter.tailings.pit.PitMass.DRY_TAILS, 18);
			spread.setSpread();
			spread.testDrive(FORWARD);
			ids[0] = climb.getUUID();
			ids[1] = kerb.getUUID();
			ids[2] = spread.getUUID();
		});
		cam(Cam.of(o, 22, 6, 20, 6, 3, 18));
		settle(30);
		server.runOnServer(srv -> {
			for (java.util.UUID id : ids) {
				if (srv.overworld().getEntity(id) instanceof dev.wildcatter.tailings.entity.TrackDozerEntity d) {
					log("dozer early at " + d.position() + ": " + d.status() + "; " + d.readout().getString() + "; fuel out " + d.outOfFuel());
				}
			}
		});
		still("dozer_01_ramp_low");
		context.waitTicks(60);
		still("dozer_02_ramp_up");
		cam(Cam.of(o, 26, 3, 12, 31, 1.5, 6));
		settle(20);
		still("dozer_03_kerb_roll");
		cam(Cam.of(o, 62, 5, 22, 48, 1, 18));
		context.waitTicks(10);
		still("dozer_04_spread_a");
		context.waitTicks(80);
		still("dozer_05_spread_b");
		cam(Cam.of(o, 4, 2.5, 12, 7, 1, 12));
		context.waitTicks(2);
		still("dozer_06_tracks_a");
		context.waitTicks(3);
		still("dozer_07_tracks_b");
		server.runOnServer(srv -> {
			for (java.util.UUID id : ids) {
				if (srv.overworld().getEntity(id) instanceof dev.wildcatter.tailings.entity.TrackDozerEntity d) {
					log("dozer at " + d.position() + ": " + d.status());
				}
			}
		});
	}

	// ---- fix 3: the TBM's round head, and a round bore -----------------------------------------------------------------------------

	private void tbm() {
		BlockPos c = new BlockPos(-300, 100, 700);
		server.runCommand("tailings rule ventilation_interlock false");
		cam(Cam.of(c, 14, 6, -20, 4, 4, -10));
		settle(20);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			fill(level, c, Blocks.AIR, -12, 20, -4, 16, -50, 50);
			fill(level, c, Blocks.STONE, -12, 20, -4, -1, -50, 50);
			fill(level, c, Blocks.STONE, -4, 12, 0, 12, -40, -1);
			Trailer.tbm(level, c, 40);
		});
		context.waitTicks(80);
		server.runOnServer(srv -> ((dev.wildcatter.tailings.block.DrillBlockEntity) srv.overworld().getBlockEntity(c.offset(3, 3, 3))).press(10, null));
		for (int s = 0; s < 40; s++) {
			context.waitTicks(20);
			server.runOnServer(srv -> Trailer.feedTbm(srv.overworld(), c));
		}
		double front = server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c));
		server.runOnServer(srv -> {
			for (var m : srv.overworld().getEntitiesOfClass(dev.wildcatter.tailings.entity.TbmEntity.class, new net.minecraft.world.phys.AABB(c).inflate(90))) {
				log("tbm: " + m.core().readout());
			}
		});
		log("tbm front " + (c.getZ() - front) + " m in");
		// the rock ahead of the head cut away, to see it
		server.runOnServer(srv -> fill(srv.overworld(), c, Blocks.AIR, -4, 12, 0, 12, -40, (int) Math.floor(front - c.getZ()) - 2));
		cam(Cam.of(c, 14, 6, front - c.getZ() - 12, 4, 4, front - c.getZ()));
		settle(20);
		still("tbm_01_head_a");
		context.waitTicks(10);
		still("tbm_02_head_b");
		cam(Cam.of(c, 4, 4, front - c.getZ() - 10, 4, 4, front - c.getZ()));
		settle(10);
		still("tbm_03_head_front");
	}

	/** The drive's own rule run on its own, no machine in the way: a lined round bore 30 m long, seen from its mouth. */
	private void bore() {
		BlockPos o = new BlockPos(-300, 100, 1000);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			fill(level, o, Blocks.AIR, -10, 20, -4, 16, -50, 30);
			fill(level, o, Blocks.STONE, -10, 20, -4, -1, -50, 30);
			fill(level, o, Blocks.STONE, -6, 14, 0, 12, -45, -1);
			var program = new dev.wildcatter.tailings.mine.DrillProgram(o.getY() + 1, dev.wildcatter.tailings.mine.Heading.NORTH, 100);
			var stock = new dev.wildcatter.tailings.mine.DrillStock();
			stock.setRail(1000);
			stock.setGravel(1000);
			stock.setCobble(1000);
			stock.setSegments(1000);
			var drill = new dev.wildcatter.tailings.mine.TunnelDrill(program, stock, o.getX(), o.getY() + 1, o.getZ() + 6);
			var ground = new dev.wildcatter.tailings.block.WorldGround(level, dev.wildcatter.tailings.mine.Heading.NORTH);
			for (int k = 0; k < 44; k++) {
				var a = drill.step(ground);
				drill.line(ground);
				if (a.stop() != dev.wildcatter.tailings.mine.Advance.Stop.NONE) {
					log("bore stopped: " + a.detail());
					break;
				}
			}
		});
		cam(Cam.of(o, 4, 5, 14, 4, 4, -30));
		settle(30);
		still("bore_01_mouth");
		cam(Cam.of(o, 4, 4.5, -6, 4, 4, -40));
		settle(20);
		still("bore_02_inside");
	}

	// ---- fix 4: the cage beside the skips --------------------------------------------------------------------------------------

	private void cage() {
		BlockPos h = new BlockPos(-300, 130, 1200);
		int depth = 30;
		java.util.UUID[] cage = new java.util.UUID[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			fill(level, h, Blocks.AIR, -10, 18, -depth - 3, 32, -10, 40);
			fill(level, h, Blocks.STONE, -10, 18, -1, -1, -10, 40);
			fill(level, h, Blocks.AIR, 2, 6, -1, -1, 2, 6);   // the shaft's collar
			fill(level, h, Blocks.STONE, -4, 14, -depth - 2, -depth - 1, -4, 14);   // the bottom landing's floor
			var headframe = Trailer.plant(level, dev.wildcatter.tailings.plant.Machine.HEADFRAME, h);
			Trailer.plant(level, dev.wildcatter.tailings.plant.Machine.HEADFRAME_BIN, h.offset(2, 0, -4));
			var hoist = Trailer.plant(level, dev.wildcatter.tailings.plant.Machine.HOIST_HOUSE, h.offset(0, 0, 20));
			Trailer.plant(level, dev.wildcatter.tailings.plant.Machine.MEASURING_FLASK, h.offset(7, -depth, 4));
			hoist.forceRunning();
			// the cage's own winder beside the hoist house (cage-winder.md)
			Trailer.plant(level, dev.wildcatter.tailings.plant.Machine.CAGE_WINDER, h.offset(11, 0, 20)).forceRunning();
			var frame = dev.wildcatter.tailings.block.ShaftLayout.Frame.of(headframe);
			dev.wildcatter.tailings.entity.ShaftSkipEntity.hang(level, frame);
			dev.wildcatter.tailings.entity.ShaftSkipEntity.hang(level, frame);
			var cg = dev.wildcatter.tailings.entity.ShaftSkipEntity.hangCage(level, frame);
			cg.deck().add(new ItemStack(net.minecraft.world.item.Items.RAIL, 64));
			cg.setLoaded(true);
			var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			villager.setNoAi(true);
			villager.setPos(cg.getX() + 1, cg.getY(), cg.getZ() + 1);
			level.addFreshEntity(villager);
			villager.startRiding(cg);
			cage[0] = cg.getUUID();
		});
		context.waitTicks(40);
		cam(Cam.of(h, 4.5, 3, 16, 4.5, 1.5, 5));
		settle(20);
		still("cage_01_collar");
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(cage[0]) instanceof dev.wildcatter.tailings.entity.ShaftSkipEntity cg) {
				cg.call(false);
			}
		});
		for (int k = 0; k < 10; k++) {
			context.waitTicks(20);
			server.runOnServer(srv -> {
				var winder = (dev.wildcatter.tailings.block.PlantBlockEntity) srv.overworld().getBlockEntity(h.offset(11, 0, 20));
				log("cage winder: " + winder.status() + "; cage " + winder.cageWinder().at() + " of " + winder.cageWinder().travel());
			});
		}
		cam(Cam.of(h, 18, -10, 14, 4.5, -14, 5));
		settle(10);
		still("cage_02_shaft");
		context.waitTicks(200);
		cam(Cam.of(h, 14, -depth + 3, 14, 4.5, -depth + 1.5, 5));
		settle(10);
		still("cage_03_bottom");
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(cage[0]) instanceof dev.wildcatter.tailings.entity.ShaftSkipEntity cg) {
				log("cage at " + cg.position() + ", riders " + cg.getPassengers().size());
			}
		});
	}

	// ---- camera and stills -------------------------------------------------------------------------------------------------------

	private void cam(Cam c) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y(), dz = c.tz() - c.z();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y() - 1.62, c.z(), yaw, pitch));
		context.runOnClient(client -> {
			var p = client.player;
			if (p != null) {
				p.setDeltaMovement(Vec3.ZERO);
				p.setPos(c.x(), c.y() - 1.62, c.z());
				p.setYRot(yaw);
				p.setYHeadRot(yaw);
				p.setXRot(pitch);
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
	}

	private void still(String name) {
		Path at = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(W, H).withDestinationDir(OUT).disableCounterPrefix());
		log("still " + at);
	}

	static void fill(ServerLevel level, BlockPos o, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		Trailer.fillBox(level, o, block, x0, x1, y0, y1, z0, z1);
	}

	// ---- fix 1: the blast's flyrock, fragments and muck pile ---------------------------------------------------------------------

	/**
	 * A 14 m bench with a 3 x 5 pattern, stemmed 4 m, fired: stills as the face goes, of the fragments in the air, and of the muck pile
	 * slumped off the face. A villager stands in the open 70 m behind it, inside the 100 m.
	 */
	private void blast() {
		BlockPos o = new BlockPos(0, 100, 400);
		List<BlockPos> collars = new ArrayList<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			fill(level, o, Blocks.AIR, -80, 120, -6, 50, -80, 160);
			fill(level, o, Blocks.STONE, -80, 120, -6, -1, -80, 160);   // the pit floor, top at o.y - 1
			fill(level, o, Blocks.STONE, 0, 60, 0, 13, 0, 160);          // the bench, 14 m, its face on z = 0 facing north
			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 5; c++) {
					BlockPos collar = o.offset(14 + 8 * c + (r % 2) * 4, 13, 6 + 7 * r);
					String toe = "minecraft:stone";
					for (int m = 0; m < 16; m++) {
						BlockPos at = collar.below(m);
						level.setBlock(at, TailingsBlocks.BLASTHOLE.defaultBlockState().setValue(BlastholeBlock.COLLAR, m == 0), Block.UPDATE_ALL);
						((BlastholeBlockEntity) level.getBlockEntity(at)).drilled(toe, collar);
					}
					BlastholeBlockEntity hole = (BlastholeBlockEntity) level.getBlockEntity(collar);
					hole.pattern(r, c, Direction.NORTH, Blasting.BURDEN, Blasting.SPACING, Blasting.SUBDRILL);
					hole.finish(16, false, toe);
					hole.addBooster();
					hole.addDetonator(1000 + r * 100 + c);
					hole.charge(828, level.getGameTime() - Blasting.SLEEP_TICKS);
					hole.stem(4);
					collars.add(collar);
				}
			}
			var villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			villager.setNoAi(true);
			villager.setPos(o.getX() + 30.5, o.getY() + 14, o.getZ() + 90.5);
			level.addFreshEntity(villager);
			BlockPos m = o.offset(30, 14, 140);
			level.setBlock(m, TailingsBlocks.BLASTING_MACHINE.defaultBlockState(), Block.UPDATE_ALL);
			ItemStack logger = new ItemStack(TailingsItems.BLAST_LOGGER);
			for (BlockPos c : collars) {
				BlastLoggerItem.log(level, logger, c);
			}
			BlastingMachineBlockEntity machine = (BlastingMachineBlockEntity) level.getBlockEntity(m);
			log("load: " + machine.load(logger));
			machine.setKey(true);
			log("arm: " + machine.arm());
		});
		Cam view = Cam.of(o, -30, 12, -38, 30, 8, 12);
		cam(view);
		settle(60);
		still("blast_00_before");
		server.runOnServer(srv -> {
			var machine = (BlastingMachineBlockEntity) srv.overworld().getBlockEntity(o.offset(30, 14, 140));
			log("fire: " + machine.fire());
			machine.sirenNow();
		});
		int[] at = {3, 8, 14, 22, 34, 50};
		int t = 0;
		for (int i = 0; i < at.length; i++) {
			context.waitTicks(at[i] - t);
			t = at[i];
			still(String.format(Locale.ROOT, "blast_%02d_t%d", i + 1, at[i]));
		}
		context.waitTicks(200);
		server.runOnServer(srv -> {
			var machine = (BlastingMachineBlockEntity) srv.overworld().getBlockEntity(o.offset(30, 14, 140));
			var shot = machine.lastShot();
			log("status: " + machine.status() + (shot == null ? "" : String.format(Locale.ROOT, "; fragments %d, strikes %d, reach %.0f m, slumped %d, flyrock %d",
					shot.fragmentsThrown(), shot.strikes(), shot.reach(), shot.slumped(), shot.flyrock())));
			level(srv.overworld(), o);
		});
		settle(40);
		still("blast_10_muck");
		cam(Cam.of(o, 34, 30, -60, 34, 5, 15));
		settle(40);
		still("blast_11_muck_front");
		cam(Cam.of(o, 120, 12, 15, 30, 5, 15));
		settle(40);
		still("blast_12_muck_side");
	}

	private static void level(ServerLevel level, BlockPos o) {
		var v = level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new net.minecraft.world.phys.AABB(o).inflate(200));
		for (var e : v) {
			log(String.format(Locale.ROOT, "villager at %.0f m behind: health %.1f of %.1f", e.getZ() - o.getZ(), e.getHealth(), e.getMaxHealth()));
		}
		log("muck blocks: " + BuiltInRegistries.BLOCK.getKey(TailingsBlocks.MUCK));
	}
}
