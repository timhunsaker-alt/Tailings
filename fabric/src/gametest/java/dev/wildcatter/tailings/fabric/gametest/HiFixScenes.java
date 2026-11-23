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
