package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Flowing dumps (TAILINGS_ONLY=pit_pour; docs/multiblocks/open-pit.md, Pouring): a loaded 100 t truck tips onto flat ground, then
 * another onto the pile it left, then a third backs to a truck dump and pours into its pit hopper, filmed from behind and to the side in daylight with the HUD hidden. Stills every second or so of each pour go to
 * POUR_OUT (default {@code pour-shots} in the run folder), films to {@code fabric/build/scene-videos/pit_pour_*.mov}.
 */
final class PourScene {
	private static final Path OUT = Path.of(System.getenv().getOrDefault("POUR_OUT", "pour-shots")).toAbsolutePath();
	private static final int W = 1600, H = 900;

	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final BlockPos b;

	PourScene(ClientGameTestContext context, TestServerContext server, int y) {
		this.context = context;
		this.server = server;
		this.b = new BlockPos(-600, y + 4, 900);
	}

	private static void log(String line) {
		System.out.println("[tailings-test] pit_pour: " + line);
	}

	void run() {
		context.runOnClient(client -> {
			client.options.particles().set(ParticleStatus.ALL);
			if (!client.gui.hud.isHidden()) {
				client.gui.hud.toggle();
			}
		});
		server.runCommand("time set noon");
		server.runCommand("gamerule advance_time false");
		server.runCommand("weather clear");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = -20; x <= 40; x++) {
				for (int z = -20; z <= 50; z++) {
					for (int yy = -4; yy <= 20; yy++) {
						level.setBlock(b.offset(x, yy, z), (yy < 0 ? (yy == -1 ? Blocks.COARSE_DIRT : Blocks.STONE) : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			level.getEntitiesOfClass(HaulTruckEntity.class, new net.minecraft.world.phys.AABB(b).inflate(80)).forEach(e -> e.discard());
		});
		try {
			pour("ground", 28.5, Map.of("minecraft:cobblestone", 50_000L, "minecraft:copper_ore", 20_000L, "minecraft:sand", 20_000L));
			pour("pile", 31.5, Map.of("minecraft:granite", 91_000L));   // backed up to the first load's pile, as on a dump
			hopper();
		} finally {
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			server.runCommand("gamerule advance_time true");
		}
	}

	/** A truck with {@code load} stands with its tail over the pad's middle and tips, filmed from behind and to the side. */
	private void pour(String name, double z, Map<String, Long> load) {
		// the camera first, so the truck is spawned in loaded chunks and the one held here is the one in the world
		BlockPos tail = BlockPos.containing(b.getX() + 10.5, b.getY() + 1, b.getZ() + z - 6.5);
		look(tail.getX() + 8.5, b.getY() + 4.5, tail.getZ() - 6.5, tail.getX() + 0.5, b.getY() + 1.0, tail.getZ() + 1.5);
		context.waitTicks(40);
		HaulTruckEntity[] truck = new HaulTruckEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			HaulTruckEntity t = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			t.setPos(b.getX() + 10.5, b.getY(), b.getZ() + z);
			t.setYRot(0);
			t.setFuel(t.tank());
			load.forEach((id, kg) -> t.load().add(id, kg));
			t.refreshLoad();
			level.addFreshEntity(t);
			truck[0] = t;
			log(name + ": " + count(level) + " blocks on the pad before, tail at " + t.tipStart());
		});
		context.waitTicks(20);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 400);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		still(name + "_0before");
		Filming film = context.computeOnClient(client -> Filming.start(client, "pit_pour_" + name));
		server.runOnServer(srv -> truck[0].pressJump(srv.overworld()));
		int[] shots = {92, 100, 110, 125, 140, 160, 180, 200, 220, 240, 260, 290, 330};
		int t = 0;
		for (int at : shots) {
			context.waitTicks(at - t);
			t = at;
			int n = t;
			String state = server.computeOnServer(srv -> String.format(Locale.ROOT, "lift %.2f, %d on their way, %s, %d blocks set", truck[0].lift(), truck[0].pouring(),
					truck[0].load().tonnes(), count(srv.overworld())));
			String seen = context.computeOnClient(client -> {
				var ts = client.level.getEntitiesOfClass(HaulTruckEntity.class, new net.minecraft.world.phys.AABB(b).inflate(60));
				return "client time " + client.level.getGameTime() + ", drawn" + ts.stream().map(x -> " " + dev.wildcatter.tailings.client.PourRenderer.mesh(x, 0, x.getX(), x.getY(), x.getZ()).length + " quads for "
						+ x.shownPour().globs().size()).toList();
			});
			long serverTime = server.computeOnServer(srv -> srv.overworld().getGameTime());
			log(name + " t" + n + ": " + state + "; server time " + serverTime + "; " + seen);
			still(String.format(Locale.ROOT, "%s_t%03d", name, n));
		}
		for (int i = 0; i < 20 && server.computeOnServer(srv -> truck[0].pouring()) > 0; i++) {
			context.waitTicks(10);
		}
		film.stop();
		int left = server.computeOnServer(srv -> truck[0].pouring());
		if (left != 0) {
			throw new AssertionError("[tailings-test] pit_pour " + name + ": " + left + " blocks still on their way");
		}
		// a second look from the other side, the pile set
		look(tail.getX() - 10.0, b.getY() + 7.0, tail.getZ() - 9.0, tail.getX() + 0.5, b.getY() + 1.0, tail.getZ() + 0.5);
		context.waitTicks(20);
		still(name + "_9after");
		server.runOnServer(srv -> truck[0].discard());
		context.waitTicks(5);
	}

	private int count(ServerLevel level) {
		int n = 0;
		for (int x = -20; x <= 40; x++) {
			for (int z = -20; z <= 50; z++) {
				for (int yy = 0; yy <= 12; yy++) {
					var s = level.getBlockState(b.offset(x, yy, z));
					if (s.is(TailingsBlocks.MUCK) || s.is(Blocks.SAND)) {
						n++;
					}
				}
			}
		}
		return n;
	}

	private void look(double x, double y, double z, double tx, double ty, double tz) {
		double dx = tx - x, dy = ty - y - 1.62, dz = tz - z;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", x, y, z, yaw, pitch));
	}

	private void still(String name) {
		Path at = context.takeScreenshot(TestScreenshotOptions.of("pour_" + name).withSize(W, H).withDestinationDir(OUT).disableCounterPrefix());
		log("still " + at);
	}

	/** A truck backs to a truck dump's bumper and pours 55 t of rock into the pit hopper below (the pit scene's dump). */
	private void hopper() {
		BlockPos h = b.offset(80, 0, 0);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = -12; x <= 30; x++) {
				for (int z = -12; z <= 40; z++) {
					for (int yy = -9; yy <= 16; yy++) {
						boolean cut = x >= 2 && x <= 17 && z >= -9 && z <= 9 && yy >= -7;
						level.setBlock(h.offset(x, yy, z), (yy < 0 && !cut ? (yy == -1 ? Blocks.COARSE_DIRT : Blocks.STONE) : Blocks.AIR).defaultBlockState(),
								Block.UPDATE_CLIENTS);
					}
				}
			}
			part(level, dev.wildcatter.tailings.TailingsBlocks.PIT_HOPPER, h.offset(5, -6, 4));
			part(level, dev.wildcatter.tailings.TailingsBlocks.TRUCK_DUMP_WALL, h.offset(4, -3, 10));
		});
		look(h.getX() + 19.0, h.getY() + 4.0, h.getZ() - 4.0, h.getX() + 9.0, h.getY() - 2.0, h.getZ() + 9.0);
		context.waitTicks(40);
		HaulTruckEntity[] truck = new HaulTruckEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			HaulTruckEntity t = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			t.setPos(h.getX() + 10.0, h.getY(), h.getZ() + 20.0);
			t.setYRot(0);
			t.setFuel(t.tank());
			t.load().add("minecraft:cobblestone", 40_000L);
			t.load().add("minecraft:copper_ore", 15_000L);
			t.refreshLoad();
			level.addFreshEntity(t);
			t.testDrive(new net.minecraft.world.entity.player.Input(false, true, false, false, false, false, false));
			truck[0] = t;
		});
		for (int i = 0; i < 40 && !server.computeOnServer(srv -> Math.abs(truck[0].speed()) < 0.01 && truck[0].getZ() < h.getZ() + 17.5); i++) {
			context.waitTicks(10);
		}
		server.runOnServer(srv -> truck[0].testDrive(net.minecraft.world.entity.player.Input.EMPTY));
		boolean over = server.computeOnServer(srv -> truck[0].hopperBehind(srv.overworld()) != null);
		log("hopper: backed to z " + server.computeOnServer(srv -> truck[0].getZ() - h.getZ()) + ", hopper behind " + over);
		if (!over) {
			throw new AssertionError("[tailings-test] pit_pour hopper: the hopper should be under the truck's tail");
		}
		context.waitTicks(20);
		still("hopper_0before");
		Filming film = context.computeOnClient(client -> Filming.start(client, "pit_pour_hopper"));
		server.runOnServer(srv -> truck[0].pressJump(srv.overworld()));
		int t = 0;
		for (int at : new int[] {92, 100, 110, 125, 140, 160, 180, 220}) {
			context.waitTicks(at - t);
			t = at;
			int n = t;
			String state = server.computeOnServer(srv -> String.format(Locale.ROOT, "lift %.2f, %d on their way, %s", truck[0].lift(), truck[0].pouring(),
					truck[0].load().tonnes()));
			log("hopper t" + n + ": " + state);
			still(String.format(Locale.ROOT, "hopper_t%03d", n));
		}
		film.stop();
		server.runOnServer(srv -> truck[0].discard());
		context.waitTicks(5);
	}

	private static void part(ServerLevel level, Block block, BlockPos at) {
		net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState().setValue(dev.wildcatter.block.BigPartBlock.FACING, net.minecraft.core.Direction.NORTH);
		level.setBlock(at, state, Block.UPDATE_ALL);
		dev.wildcatter.multiblock.BigParts.grow(level, at, level.getBlockState(at));
	}

}
