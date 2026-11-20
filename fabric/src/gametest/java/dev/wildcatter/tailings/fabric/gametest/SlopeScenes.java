package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.Slopes;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The dragline by hand and its slopes failing (docs/multiblocks/dragline-hand-control.md), filmed: TAILINGS_ONLY=dragline_hand,
 * highwall_slide, spoil_slide, dragline_wrecked. Films to {@code fabric/build/scene-videos/<scene>.mov}, stills in the run's screenshots.
 */
final class SlopeScenes {
	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;

	SlopeScenes(ClientGameTestContext context, TestServerContext server, int y) {
		this.context = context;
		this.server = server;
		this.y = y;
	}

	private static void log(String scene, String line) {
		System.out.println("[tailings-test] " + scene + ": " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[tailings-test] " + what);
		}
	}

	private Filming film(String scene) {
		context.waitTicks(40);
		return context.computeOnClient(client -> Filming.start(client, scene));
	}

	private static void fill(ServerLevel level, BlockPos o, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int yy = y0; yy <= y1; yy++) {
					level.setBlock(o.offset(x, yy, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Clears a site, lays a stone floor at its y 0 and grass over dirt where asked. */
	private static void site(ServerLevel level, BlockPos o) {
		fill(level, o, Blocks.AIR, -30, 100, 1, 50, -30, 90);
		fill(level, o, Blocks.STONE, -30, 100, -2, 0, -30, 90);
	}

	void run(boolean hand, boolean slide, boolean spoil, boolean wrecked) {
		context.runOnClient(client -> client.options.particles().set(ParticleStatus.ALL));
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamerule advance_time false");
		try {
			if (hand) {
				hand();
			}
			server.runCommand("tailings rule slope_failures true");
			if (slide) {
				highwallSlide();
			}
			if (spoil) {
				spoilSlide();
			}
			if (wrecked) {
				wrecked();
			}
		} finally {
			server.runCommand("tailings rule slope_failures false");
			server.runCommand("gamerule advance_time true");
		}
	}

	private static Input keys(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sprint) {
		return new Input(forward, back, left, right, jump, false, sprint);
	}

	/**
	 * dragline_hand: an operator takes the levers mid-cycle, lowers the bucket onto a bench of dirt ahead, drags it full, hoists it,
	 * swings it right and lets the house coast, and pays out the drag to dump it.
	 */
	private void hand() {
		BlockPos o = new BlockPos(2200, y + 4, -800);
		UUID[] name = new UUID[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.DIRT, -30, 100, 1, 9, -30, 90);
			fill(level, o, Blocks.GRASS_BLOCK, -30, 100, 10, 10, -30, 90);
			BlockPos hs = o.offset(-10, 11, 60);
			level.setBlock(hs, TailingsBlocks.PIT_SWITCH_HOUSE.defaultBlockState().setValue(dev.wildcatter.block.BigPartBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
			dev.wildcatter.multiblock.BigParts.grow(level, hs, level.getBlockState(hs));
			PitSwitchHouseBlockEntity house = (PitSwitchHouseBlockEntity) level.getBlockEntity(hs);
			house.forceSupply(true);
			DraglineEntity d = TailingsEntities.DRAGLINE.create(level, EntitySpawnReason.COMMAND);
			d.setPos(o.getX() + 20.5, o.getY() + 11, o.getZ() + 0.5);
			d.setYRot(0);
			d.job().set(0, 10);
			d.job().set(1, 10);
			d.job().set(2, 2);
			d.job().set(3, 0);
			level.addFreshEntity(d);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE, 4);
			TrailingCableItem.pickUp(reel, house);
			log("dragline_hand", "cable " + TrailingCableItem.couple(level, reel, d) + "; outlet " + house.close(0));
			d.fastCycles(2);
			d.start();
			name[0] = d.getUUID();
		});
		server.runCommand("tp @p " + (o.getX() + 34) + " " + (o.getY() + 62) + " " + (o.getZ() - 34) + " 20 42");
		context.waitTicks(60);
		Filming film = film("dragline_hand");
		Input[] steps = {keys(false, false, false, false, true, false), keys(false, false, false, false, false, true), keys(true, false, false, false, false, false),
				keys(false, false, false, false, true, false), keys(false, false, false, true, false, false), Input.EMPTY, keys(false, true, false, false, false, false)};
		int[] ticks = {10, 60, 180, 120, 120, 120, 60};
		String[] what = {"takes the levers", "lowers", "drags through the bank", "hoists", "swings right", "lets the house coast", "pays out the drag: dump"};
		for (int i = 0; i < steps.length; i++) {
			Input k = steps[i];
			int at = i;
			String said = server.computeOnServer(srv -> {
				if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
					d.testSeat(true);
					d.testDrive(k);
					if (at == 1) {
						d.emptyBucket();
						d.handPose(0, 45, 6);
					}
					return d.status();
				}
				return "gone";
			});
			log("dragline_hand", what[i] + ": " + said);
			context.waitTicks(ticks[i]);
			if (i == 2) {
				context.takeScreenshot("tailings-dragline-hand-dragging");
			}
			if (i == 5) {
				context.takeScreenshot("tailings-dragline-hand-coasting");
			}
		}
		context.waitTicks(60);
		context.takeScreenshot("tailings-dragline-hand-dumped");
		film.stop();
		long casts = server.computeOnServer(srv -> srv.overworld().getEntity(name[0]) instanceof DraglineEntity d ? d.handCasts() : -1L);
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
				d.testSeat(false);
				d.testDrive(Input.EMPTY);
				log("dragline_hand", "out of the cab: " + d.status());
			}
		});
		require(casts == 1, "one bucket cast by hand: " + casts);
	}

	/** A strip in the slope record, from a dragline centre at o + (20.5, crest, 0.5) facing +z, the strip to +x from x 33. */
	private static long strip(ServerLevel level, BlockPos o, int crest, int width) {
		return Slopes.register(level, UUID.randomUUID(), o.getX() + 20.5, o.getZ() + 0.5, 0, 1, 1, 0, o.getY() + crest, width, 10, Dragline.BATTER_VERTICAL);
	}

	/**
	 * highwall_slide: a 9 m vertical dirt highwall with three prisms on its bench and a survey station: FAILING, a crack opens behind
	 * the crest and the prisms accelerate; the toe is dug 3 m deeper, the wall comes down within seconds and runs into the cut.
	 */
	private void highwallSlide() {
		BlockPos o = new BlockPos(2200, y + 4, -500);
		long[] id = new long[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.STONE, -30, 100, 1, 3, -30, 90);
			fill(level, o, Blocks.DIRT, -30, 32, 4, 11, -30, 30);
			fill(level, o, Blocks.GRASS_BLOCK, -30, 32, 12, 12, -30, 30);
			for (int z : new int[] {2, 5, 8}) {
				level.setBlock(o.offset(28, 13, z), TailingsBlocks.SLOPE_PRISM.defaultBlockState(), Block.UPDATE_ALL);
			}
			level.setBlock(o.offset(15, 13, 20), TailingsBlocks.SURVEY_STATION.defaultBlockState(), Block.UPDATE_ALL);
			id[0] = strip(level, o, 13, 30);
			Slopes.checkNow(level, id[0], 0);
			Slopes.Part p = Slopes.strip(srv, id[0]).section(0).wall();
			log("highwall_slide", String.format(java.util.Locale.ROOT, "FS %.2f %s, %.0f m", p.fs(), p.stage(), p.height()));
		});
		server.runCommand("tp @p " + (o.getX() + 60) + " " + (o.getY() + 22) + " " + (o.getZ() - 22) + " 50 18");
		context.waitTicks(40);
		Filming film = film("highwall_slide");
		context.waitTicks(100);
		context.takeScreenshot("tailings-highwall-cracks");
		server.runOnServer(srv -> {
			var station = (dev.wildcatter.tailings.block.SurveyStationBlockEntity) srv.overworld().getBlockEntity(o.offset(15, 13, 20));
			station.read(srv.overworld());
			station.lines().forEach(line -> log("highwall_slide", line));
			// the toe dug 3 m deeper: 12 m of vertical dirt cannot stand
			fill(srv.overworld(), o, Blocks.AIR, 33, 100, 1, 3, -30, 90);
			fill(srv.overworld(), o, Blocks.STONE, 33, 100, -2, 0, -30, 90);
			fill(srv.overworld(), o, Blocks.DIRT, -30, 32, 1, 3, -30, 30);
			Slopes.checkNow(srv.overworld(), id[0], 0);
		});
		String stage = "";
		for (int t = 0; t < 60 && !stage.equals("SLID"); t++) {
			context.waitTicks(10);
			stage = server.computeOnServer(srv -> Slopes.strip(srv, id[0]).section(0).wall().stage().name());
		}
		context.waitTicks(120);
		context.takeScreenshot("tailings-highwall-slid");
		film.stop();
		log("highwall_slide", "stage " + stage);
		require(stage.equals("SLID"), "the highwall should slide: " + stage);
	}

	/** spoil_slide: an 18 m spoil pile at 37 degrees on a clay floor, water ponded at its toe: it slumps to about 1 in 2. */
	private void spoilSlide() {
		BlockPos o = new BlockPos(2200, y + 4, -200);
		long[] id = new long[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.CLAY, -30, 100, 1, 1, -30, 90);
			for (int x = 44; x <= 70; x++) {
				int k = Math.min(18, (int) Math.ceil((x - 43) * 0.75));
				fill(level, o, Blocks.DIRT, x, x, 2, 1 + k, -10, 20);
			}
			fill(level, o, Blocks.WATER, 38, 43, 2, 2, -2, 12);
			id[0] = strip(level, o, 12, 10);
			Slopes.cast(level, id[0], o.offset(50, 10, 5));
			Slopes.checkNow(level, id[0], 0);
			Slopes.Part p = Slopes.strip(srv, id[0]).section(0).spoil();
			log("spoil_slide", String.format(java.util.Locale.ROOT, "FS %.2f %s, %.0f m", p.fs(), p.stage(), p.height()));
		});
		server.runCommand("tp @p " + (o.getX() + 55) + " " + (o.getY() + 26) + " " + (o.getZ() - 40) + " 0 25");
		context.waitTicks(40);
		Filming film = film("spoil_slide");
		context.takeScreenshot("tailings-spoil-before");
		String stage = "";
		for (int t = 0; t < 60 && !stage.equals("SLID"); t++) {
			context.waitTicks(10);
			stage = server.computeOnServer(srv -> Slopes.strip(srv, id[0]).section(0).spoil().stage().name());
		}
		context.waitTicks(120);
		context.takeScreenshot("tailings-spoil-slid");
		film.stop();
		log("spoil_slide", "stage " + stage);
		require(stage.equals("SLID"), "the spoil should slide: " + stage);
	}

	/** dragline_wrecked: a dragline standing on dirt beside a 30 m vertical highwall: the wall slides and takes the dragline with it. */
	private void wrecked() {
		BlockPos o = new BlockPos(2200, y + 4, 100);
		UUID[] name = new UUID[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.DIRT, -30, 32, 1, 29, -30, 30);
			fill(level, o, Blocks.GRASS_BLOCK, -30, 32, 30, 30, -30, 30);
			DraglineEntity d = TailingsEntities.DRAGLINE.create(level, EntitySpawnReason.COMMAND);
			d.setPos(o.getX() + 20.5, o.getY() + 31, o.getZ() + 0.5);
			d.setYRot(0);
			d.job().set(0, 30);
			d.job().set(1, 10);
			d.job().set(2, 1);
			d.job().set(3, 0);
			level.addFreshEntity(d);
			d.start();
			name[0] = d.getUUID();
			Slopes.checkNow(level, d.stripId(), 0);
			Slopes.Part p = Slopes.strip(srv, d.stripId()).section(0).wall();
			log("dragline_wrecked", String.format(java.util.Locale.ROOT, "FS %.2f %s, %.0f m", p.fs(), p.stage(), p.height()));
		});
		server.runCommand("tp @p " + (o.getX() + 85) + " " + (o.getY() + 45) + " " + (o.getZ() - 45) + " 45 22");
		context.waitTicks(60);
		Filming film = film("dragline_wrecked");
		context.takeScreenshot("tailings-dragline-before-slide");
		boolean gone = false;
		for (int t = 0; t < 60 && !gone; t++) {
			context.waitTicks(10);
			gone = server.computeOnServer(srv -> !(srv.overworld().getEntity(name[0]) instanceof DraglineEntity));
		}
		context.waitTicks(140);
		context.takeScreenshot("tailings-dragline-wrecked");
		film.stop();
		log("dragline_wrecked", "gone " + gone);
		require(gone, "the dragline should go down with the slide");
	}
}
