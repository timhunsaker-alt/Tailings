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
 * another onto the pile it left, filmed from behind and to the side in daylight with the HUD hidden. Stills every second or so of each pour go to
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
		int[] shots = {92, 100, 108, 118, 130, 145, 160, 180, 200, 230, 280};
		int t = 0;
		for (int at : shots) {
			context.waitTicks(at - t);
			t = at;
			int n = t;
			String state = server.computeOnServer(srv -> String.format(Locale.ROOT, "lift %.2f, %d on their way, %s, %d blocks set", truck[0].lift(), truck[0].pouring(),
					truck[0].load().tonnes(), count(srv.overworld())));
			String seen = context.computeOnClient(client -> {
				var ts = client.level.getEntitiesOfClass(HaulTruckEntity.class, new net.minecraft.world.phys.AABB(b).inflate(60));
				return "client time " + client.level.getGameTime() + ", drawn" + ts.stream().map(x -> " " + dev.wildcatter.tailings.client.PourRenderer.lumps(x, 0, x.getX(), x.getY(), x.getZ()).length + " lumps of "
						+ x.shownPour().globs().size()).toList();
			});
			long serverTime = server.computeOnServer(srv -> srv.overworld().getGameTime());
			log(name + " t" + n + ": " + state + "; server time " + serverTime + "; " + seen);
			still(String.format(Locale.ROOT, "%s_t%03d", name, n));
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
}
