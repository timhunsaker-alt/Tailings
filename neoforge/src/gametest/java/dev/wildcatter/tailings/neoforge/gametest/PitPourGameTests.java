package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.Pour;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Flowing dumps (docs/multiblocks/open-pit.md, Pouring): a haul truck's load pours out of its tail as the body rises and sets block
 * by block into the pile an instant tip leaves, onto the ground, onto a pile and into a hopper; cut short, it loses nothing.
 */
final class PitPourGameTests {
	static final String[] SCENES = {"pit_pour_ground", "pit_pour_pile", "pit_pour_hopper", "pit_pour_cut_short", "pit_pour_dozer", "pit_pour_dragline", "pit_pour_loader"};

	private static final Input BACK = new Input(false, true, false, false, false, false, false);
	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);

	private PitPourGameTests() {
	}

	static int ticks(String scene, int otherwise) {
		return java.util.Arrays.asList(SCENES).contains(scene) ? 1600 : otherwise;
	}

	private static void floor(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					helper.setBlock(x, y, z, Blocks.STONE);
				}
			}
		}
	}

	private static <T extends HaulTruckEntity> T truck(GameTestHelper helper, EntityType<T> type, double x, int y, double z, float yaw) {
		T v = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		v.setPos(origin.getX() + x, helper.absolutePos(new BlockPos(0, y, 0)).getY(), origin.getZ() + z);
		v.setYRot(yaw);
		helper.getLevel().addFreshEntity(v);
		return v;
	}

	private static int count(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
						n++;
					}
				}
			}
		}
		return n;
	}

	/** The quarters of each item in the muck in a box. */
	private static Map<String, Integer> quarters(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1) {
		Map<String, Integer> out = new java.util.TreeMap<>();
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					if (helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, y, z))) instanceof MuckBlockEntity muck) {
						muck.quarters().forEach((id, q) -> out.merge(id, q, Integer::sum));
					}
				}
			}
		}
		return out;
	}

	private static int top(GameTestHelper helper, int x, int z, int y) {
		while (!helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
			y++;
		}
		return y;
	}

	/** No step of 2 or more between neighbouring columns over the box: the pile stands at no more than 1 in 1. */
	private static void standing(GameTestHelper helper, int x0, int x1, int z0, int z1, int y, String what) {
		for (int x = x0; x < x1; x++) {
			for (int z = z0; z < z1; z++) {
				int h = top(helper, x, z, y);
				helper.assertTrue(Math.abs(h - top(helper, x + 1, z, y)) <= 1 && Math.abs(h - top(helper, x, z + 1, y)) <= 1,
						Component.literal(what + ": a step of 2 or more in the pile at " + x + ", " + z));
			}
		}
	}

	/**
	 * On flat ground: 30 t of sand and 27 t of cobble pour out from a third of the way up, over a few seconds, and set to the same 19
	 * sand and 14 muck blocks (40 quarters) as the same load tipped at once beside it, at no more than 1 in 1.
	 */
	static void ground(GameTestHelper helper) {
		floor(helper, 0, 30, 58, 59, 0, 62);
		HaulTruckEntity instant = truck(helper, TailingsEntities.HAUL_TRUCK, 15.5, 60, 50.5, 0);
		instant.load().add("minecraft:sand", 30_000);
		instant.load().add("minecraft:cobblestone", 27_000);
		instant.tip(helper.getLevel());
		int instantSand = count(helper, Blocks.SAND, 0, 30, 60, 75, 32, 62), instantMuck = count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 32, 62);
		Map<String, Integer> instantQ = quarters(helper, 0, 30, 60, 75, 32, 62);
		helper.assertTrue(instantSand == 19 && instantMuck == 14 && instantQ.equals(Map.of("minecraft:cobblestone", 40)),
				Component.literal("the instant tip: 19 sand and 14 muck of 40 quarters, got " + instantSand + ", " + instantMuck + ", " + instantQ));
		HaulTruckEntity truck = truck(helper, TailingsEntities.HAUL_TRUCK, 15.5, 60, 18.5, 0);
		truck.load().add("minecraft:sand", 30_000);
		truck.load().add("minecraft:cobblestone", 27_000);
		truck.refreshLoad();
		truck.pressJump(helper.getLevel());
		long[] firstSet = {-1}, lastSet = {-1}, started = {-1};
		int[] seen = {0};
		float[] liftAtStart = {0};
		helper.onEachTick(() -> {
			long now = helper.getLevel().getGameTime();
			if (started[0] < 0 && truck.pouring() > 0) {
				started[0] = now;
				liftAtStart[0] = truck.lift();
			}
			int placed = count(helper, Blocks.SAND, 0, 30, 60, 75, 0, 30) + count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30);
			if (placed > seen[0]) {
				if (firstSet[0] < 0) {
					firstSet[0] = now;
				}
				lastSet[0] = now;
				seen[0] = placed;
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.pouring() > 0, Component.literal("the load should start to pour: lift " + truck.lift())))
				.thenExecute(() -> {
					helper.assertTrue(liftAtStart[0] >= Pour.FROM && liftAtStart[0] < Pour.FROM + 0.02F, Component.literal("it pours from a third up, at lift " + liftAtStart[0]));
					helper.assertTrue(truck.load().empty(), Component.literal("all of it is on its way out: " + truck.load().save()));
					helper.assertTrue(seen[0] == 0, Component.literal("nothing has set the moment it starts: " + seen[0]));
				})
				.thenWaitUntil(() -> helper.assertTrue(seen[0] > 3 && seen[0] < 30, Component.literal("the pile should grow block by block: " + seen[0])))
				.thenWaitUntil(() -> helper.assertTrue(truck.pouring() == 0, Component.literal("the pour should finish: " + truck.pouring() + " on their way")))
				.thenExecute(() -> {
					int sand = count(helper, Blocks.SAND, 0, 30, 60, 75, 0, 30), muck = count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30);
					Map<String, Integer> q = quarters(helper, 0, 30, 60, 75, 0, 30);
					helper.assertTrue(sand == instantSand && muck == instantMuck && q.equals(instantQ),
							Component.literal("poured, the same as tipped at once: " + sand + " sand, " + muck + " muck, " + q));
					helper.assertTrue(truck.load().empty(), Component.literal("the truck comes away empty: " + truck.load().save()));
					long spread = lastSet[0] - firstSet[0];
					System.out.printf(java.util.Locale.ROOT, "[tailings-test] pit_pour_ground: started at lift %.2f; first set %d ticks in, last %d (%d ticks of setting)%n",
							liftAtStart[0], firstSet[0] - started[0], lastSet[0] - started[0], spread);
					helper.assertTrue(spread >= 40 && lastSet[0] - started[0] <= 320, Component.literal("set over a few seconds: " + spread + " ticks, done "
							+ (lastSet[0] - started[0]) + " after the start"));
					standing(helper, 2, 28, 0, 28, 60, "poured");
					// none of either pile under its truck's body (x 12.4 to 18.6, z from its tail to its front)
					int under = count(helper, Blocks.SAND, 12, 19, 60, 66, 14, 23) + count(helper, TailingsBlocks.MUCK, 12, 19, 60, 66, 14, 23)
							+ count(helper, Blocks.SAND, 12, 19, 60, 66, 46, 55) + count(helper, TailingsBlocks.MUCK, 12, 19, 60, 66, 46, 55);
					helper.assertTrue(under == 0, Component.literal("no block of the pile under a truck's body: " + under));
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.lift() == 0 && !truck.busy(), Component.literal("the body should come back down")))
				.thenSucceed();
	}

	/** Onto a standing pile: 54 t of cobble runs over the pile's top and sets as 27 more muck blocks (80 quarters), the pile at 1 in 1. */
	static void pile(GameTestHelper helper) {
		floor(helper, 0, 30, 58, 59, 0, 30);
		HaulTruckEntity truck = truck(helper, TailingsEntities.HAUL_TRUCK, 15.5, 60, 22.5, 0);
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < 60; i++) {
			Loose.muck(level, truck.tipStart(27), Map.of("minecraft:andesite", 3));   // where its 27 blocks will go
		}
		int before = count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30);
		int peak = 60;
		for (int x = 0; x <= 30; x++) {
			for (int z = 0; z <= 30; z++) {
				peak = Math.max(peak, top(helper, x, z, 60));
			}
		}
		int pileTop = peak;
		helper.assertTrue(before == 60 && pileTop >= 62, Component.literal("a pile of 60 muck blocks stands behind the truck, " + (pileTop - 60) + " high"));
		truck.load().add("minecraft:cobblestone", 54_000);
		truck.refreshLoad();
		truck.pressJump(level);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.pouring() > 0, Component.literal("the load should start to pour")))
				.thenWaitUntil(() -> helper.assertTrue(truck.pouring() == 0, Component.literal("the pour should finish: " + truck.pouring() + " on their way")))
				.thenExecute(() -> {
					int muck = count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30);
					Map<String, Integer> q = quarters(helper, 0, 30, 60, 75, 0, 30);
					helper.assertTrue(muck == 60 + 27, Component.literal("54 t of cobble is 20 bank blocks, 80 quarters, 27 more muck blocks: " + (muck - 60)));
					helper.assertTrue(q.getOrDefault("minecraft:cobblestone", 0) == 80 && q.getOrDefault("minecraft:andesite", 0) == 180,
							Component.literal("every quarter is in the pile: " + q));
					int onPile = 0;
					for (int x = 0; x <= 30; x++) {
						for (int z = 0; z <= 30; z++) {
							for (int y = 61; y <= 75; y++) {
								if (level.getBlockEntity(helper.absolutePos(new BlockPos(x, y, z))) instanceof MuckBlockEntity m && m.quarters().containsKey("minecraft:cobblestone")) {
									onPile++;
								}
							}
						}
					}
					helper.assertTrue(onPile > 0, Component.literal("some of the load sets up on the pile, not only on the ground round it"));
					helper.assertTrue(truck.load().empty(), Component.literal("the truck comes away empty"));
					standing(helper, 1, 29, 0, 29, 60, "onto the pile");
				})
				.thenSucceed();
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T part(GameTestHelper helper, Block block, BlockPos pos, Class<T> entity) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		helper.setBlock(pos, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)));
		return entity == null ? null : helper.getBlockEntity(pos, entity);
	}

	/**
	 * Into the hopper: a truck backed to the bumper pours 55 t of cobble in from a third of the way up; the hopper takes 20 bank blocks
	 * and keeps the 1 t, and the pour is drawn for a few seconds. Then 16 t of sand: refused, and none of it leaves the body.
	 */
	static void hopper(GameTestHelper helper) {
		floor(helper, 0, 14, 57, 58, 12, 34);
		DumpBlockEntity hopper = part(helper, TailingsBlocks.PIT_HOPPER, new BlockPos(2, 53, 4), DumpBlockEntity.class);
		part(helper, TailingsBlocks.TRUCK_DUMP_WALL, new BlockPos(1, 56, 10), PlantBlockEntity.class);
		HaulTruckEntity truck = truck(helper, TailingsEntities.HAUL_TRUCK, 7.0, 59, 20.0, 0);
		truck.load().add("minecraft:cobblestone", 55_000);
		truck.refreshLoad();
		truck.testDrive(BACK);
		int[] drawn = {0};
		helper.onEachTick(() -> drawn[0] = Math.max(drawn[0], truck.shownPour().globs().size()));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.speed() == 0 && truck.getZ() - helper.absolutePos(BlockPos.ZERO).getZ() < 17.5,
						Component.literal("the truck should back up to the bumper")))
				.thenExecute(() -> {
					truck.testDrive(Input.EMPTY);
					helper.assertTrue(truck.hopperBehind(helper.getLevel()) == hopper, Component.literal("the hopper should be under the truck's tail"));
					truck.pressJump(helper.getLevel());
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.lift() > 0.2F, Component.literal("the body rises")))
				.thenExecute(() -> helper.assertTrue(hopper.pocket() == 0 && truck.load().total() == 55_000, Component.literal("nothing goes in below a third up")))
				.thenWaitUntil(() -> helper.assertTrue(hopper.pocket() == 20, Component.literal("55 t of cobble is 20 bank blocks in the hopper, got " + hopper.pocket())))
				.thenExecute(() -> {
					helper.assertTrue(truck.lift() >= Pour.FROM && truck.lift() < Pour.FROM + 0.02F, Component.literal("it pours from a third up: lift " + truck.lift()));
					helper.assertTrue(truck.load().empty() && truck.pouring() >= 20, Component.literal("its load is on its way in, drawn as " + truck.pouring() + " lumps"));
					helper.assertTrue(hopper.part().getOrDefault("minecraft:cobblestone", 0L) == 1_000, Component.literal("the hopper keeps the 1 t: " + hopper.part()));
				})
				.thenWaitUntil(() -> helper.assertTrue(!truck.busy() && truck.pouring() == 0, Component.literal("the pour ends and the body comes down")))
				.thenExecute(() -> {
					helper.assertTrue(drawn[0] >= 20, Component.literal("the client is sent the pour to draw: " + drawn[0] + " lumps"));
					truck.load().add("minecraft:sand", 16_000);
					truck.refreshLoad();
					truck.pressJump(helper.getLevel());
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.lift() > 0.5F, Component.literal("the body rises again")))
				.thenWaitUntil(() -> helper.assertTrue(!truck.busy(), Component.literal("and comes down")))
				.thenExecute(() -> {
					helper.assertTrue(truck.status().contains("not rock") && hopper.pocket() == 20, Component.literal("the hopper refuses soil: " + truck.status()));
					helper.assertTrue(truck.load().kg("minecraft:sand") == 16_000 && truck.pouring() == 0, Component.literal("the refused load stays in the body: " + truck.load().save()));
				})
				.thenSucceed();
	}

	/**
	 * Cut short: one truck is broken up mid-pour, and its load still on its way sets at once; another is saved and unloaded mid-pour
	 * and loaded again after its pour's time has passed, and it all sets on its first tick back. Nothing is lost, nothing left on its
	 * way, and the piles stand at 1 in 1. The ultra pours the same way.
	 */
	static void cutShort(GameTestHelper helper) {
		floor(helper, 0, 62, 58, 59, 0, 70);
		ServerLevel level = helper.getLevel();
		HaulTruckEntity broken = truck(helper, TailingsEntities.HAUL_TRUCK, 15.5, 60, 22.5, 0);
		broken.load().add("minecraft:cobblestone", 54_000);
		broken.refreshLoad();
		broken.pressJump(level);
		HaulTruckEntity saved = truck(helper, TailingsEntities.HAUL_TRUCK, 45.5, 60, 22.5, 0);
		saved.load().add("minecraft:cobblestone", 54_000);
		saved.refreshLoad();
		saved.pressJump(level);
		UltraHaulTruckEntity ultra = truck(helper, TailingsEntities.ULTRA_HAUL_TRUCK, 30.5, 60, 58.5, 0);
		ultra.load().add("minecraft:gravel", 95_000);   // 50 blocks of gravel
		ultra.load().add("minecraft:granite", 108_000);   // 40 bank blocks, 160 quarters, 54 muck blocks (53 full, one of 1)
		ultra.refreshLoad();
		ultra.pressJump(level);
		Entity[] back = new Entity[1];
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(broken.pouring() > 0 && count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30) > 2,
						Component.literal("the pour is under way: " + broken.pouring() + " on their way")))
				.thenExecute(() -> {
					int onTheirWay = broken.pouring();
					helper.assertTrue(onTheirWay > 5, Component.literal("most of it still on its way: " + onTheirWay));
					broken.discard();
					helper.assertTrue(count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30) == 27 && quarters(helper, 0, 30, 60, 75, 0, 30).equals(Map.of("minecraft:cobblestone", 80)),
							Component.literal("broken mid-pour, all 80 quarters set at once: " + quarters(helper, 0, 30, 60, 75, 0, 30)));
					standing(helper, 1, 29, 0, 29, 60, "broken mid-pour");
					// the other truck goes with its chunk, its pour saved with it
					helper.assertTrue(saved.pouring() > 5, Component.literal("the other is mid-pour: " + saved.pouring()));
					TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
					saved.saveWithoutId(out);
					saved.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
					int setSoFar = count(helper, TailingsBlocks.MUCK, 32, 62, 60, 75, 0, 30);
					helper.assertTrue(setSoFar < 27, Component.literal("unloaded, the rest waits with it: " + setSoFar + " set"));
					back[0] = new Object() {
						Entity make() {
							HaulTruckEntity fresh = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.LOAD);
							fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
							fresh.setUUID(java.util.UUID.randomUUID());
							return fresh;
						}
					}.make();
				})
				.thenIdle(200)   // its chunk unloaded while the pour's time passes
				.thenExecute(() -> {
					helper.assertTrue(((HaulTruckEntity) back[0]).pouring() > 5, Component.literal("loaded again, its pour is still with it: " + ((HaulTruckEntity) back[0]).pouring()));
					level.addFreshEntity(back[0]);
				})
				.thenIdle(2)
				.thenExecute(() -> {
					HaulTruckEntity t = (HaulTruckEntity) back[0];
					helper.assertTrue(t.pouring() == 0, Component.literal("its pour sets on its first tick back: " + t.pouring() + " left"));
					helper.assertTrue(count(helper, TailingsBlocks.MUCK, 32, 62, 60, 75, 0, 30) == 27 && quarters(helper, 32, 62, 60, 75, 0, 30).equals(Map.of("minecraft:cobblestone", 80)),
							Component.literal("reloaded, all 80 quarters set: " + quarters(helper, 32, 62, 60, 75, 0, 30)));
					helper.assertTrue(t.load().empty(), Component.literal("and its body is empty"));
					standing(helper, 33, 61, 0, 29, 60, "reloaded");
				})
				.thenWaitUntil(() -> helper.assertTrue(ultra.pouring() == 0 && ultra.lift() >= Pour.TO, Component.literal("the ultra's pour should finish: " + ultra.pouring())))
				.thenExecute(() -> {
					int gravel = count(helper, Blocks.GRAVEL, 0, 62, 60, 80, 32, 70), muck = count(helper, TailingsBlocks.MUCK, 0, 62, 60, 80, 32, 70);
					helper.assertTrue(gravel == 50 && muck == 54 && quarters(helper, 0, 62, 60, 80, 32, 70).equals(Map.of("minecraft:granite", 160)),
							Component.literal("the ultra's pour: 50 gravel and 54 muck of 160 quarters, got " + gravel + ", " + muck + ", " + quarters(helper, 0, 62, 60, 80, 32, 70)));
					helper.assertTrue(ultra.load().empty(), Component.literal("the ultra comes away empty"));
					standing(helper, 1, 61, 32, 69, 60, "the ultra's pour");
					int under = count(helper, Blocks.GRAVEL, 25, 35, 60, 70, 51, 66) + count(helper, TailingsBlocks.MUCK, 25, 35, 60, 70, 51, 66);
					helper.assertTrue(under == 0, Component.literal("none of the ultra's pile under its body: " + under));
				})
				.thenSucceed();
	}

	private static <T extends dev.wildcatter.tailings.entity.PitVehicleEntity> T machine(GameTestHelper helper, EntityType<T> type, double x, int y, double z, float yaw) {
		T v = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		v.setPos(origin.getX() + x, helper.absolutePos(new BlockPos(0, y, 0)).getY(), origin.getZ() + z);
		v.setYRot(yaw);
		helper.getLevel().addFreshEntity(v);
		return v;
	}

	/**
	 * The dozer (open-pit.md, Pouring): reversing with 18 blocks of sand on its blade, it leaves them in front as a short pour off the
	 * blade that sets clear of its tracks, all 18 at no more than 1 in 1; a second dozer broken up mid-spill loses none of its 18.
	 */
	static void dozer(GameTestHelper helper) {
		floor(helper, 0, 40, 58, 59, 0, 40);
		dev.wildcatter.tailings.entity.TrackDozerEntity dozer = machine(helper, TailingsEntities.TRACK_DOZER, 10.5, 60, 10.5, 0);
		dozer.setBlade(dev.wildcatter.tailings.entity.TrackDozerEntity.Blade.CARRY);
		dozer.loadBlade("minecraft:sand", 18);
		dozer.setFuel(dozer.tank());
		dev.wildcatter.tailings.entity.TrackDozerEntity other = machine(helper, TailingsEntities.TRACK_DOZER, 30.5, 60, 10.5, 0);
		other.setBlade(dev.wildcatter.tailings.entity.TrackDozerEntity.Blade.CARRY);
		other.loadBlade("minecraft:sand", 18);
		other.setFuel(other.tank());
		dozer.testDrive(BACK);
		other.testDrive(BACK);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(dozer.pouring() > 0 && other.pouring() > 0, Component.literal("reversing, both blades spill: " + dozer.status())))
				.thenExecute(() -> {
					dozer.testDrive(Input.EMPTY);
					other.testDrive(Input.EMPTY);
					helper.assertTrue(count(helper, Blocks.SAND, 0, 40, 60, 70, 0, 40) == 0, Component.literal("nothing set the moment it spills"));
					other.discard();   // broken up mid-spill
					helper.assertTrue(count(helper, Blocks.SAND, 21, 40, 60, 70, 0, 40) == 18, Component.literal("broken mid-spill, all 18 set at once: "
							+ count(helper, Blocks.SAND, 21, 40, 60, 70, 0, 40)));
				})
				.thenWaitUntil(() -> helper.assertTrue(dozer.pouring() == 0, Component.literal("the spill sets: " + dozer.pouring() + " on their way")))
				.thenExecute(() -> {
					int sand = count(helper, Blocks.SAND, 0, 20, 60, 70, 0, 40);
					helper.assertTrue(sand == 18 && dozer.load().empty(), Component.literal("all 18 left in front: " + sand + ", blade " + dozer.load().save()));
					standing(helper, 1, 19, 1, 39, 60, "the dozer's spill");
					net.minecraft.world.phys.AABB box = dozer.getBoundingBox();
					int under = 0;
					for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX - 1e-6, box.maxY - 1e-6, box.maxZ - 1e-6))) {
						if (helper.getLevel().getBlockState(p).is(Blocks.SAND)) {
							under++;
						}
					}
					helper.assertTrue(under == 0, Component.literal("none of it under the dozer: " + under));
				})
				.thenSucceed();
	}

	/**
	 * The dragline (open-pit.md, Pouring): its bucket opens over the spoil and the 25 bank blocks pour from height and set, none
	 * floating; broken up mid-pour, it sets them at once, and none is lost.
	 */
	static void dragline(GameTestHelper helper) {
		DraglineGameTests.bench(helper, 90, 70);
		DraglineGameTests.strip(helper, 30, 10, 5, Blocks.SAND, 2);
		dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity house = DraglineGameTests.house(helper);
		dev.wildcatter.tailings.entity.DraglineEntity d = DraglineGameTests.dragline(helper, house, 30, 10, 5, 4);
		int before = count(helper, Blocks.SAND, 33, 62, 5, 9, 30, 39);
		int bench = DraglineGameTests.BENCH;
		int all = count(helper, Blocks.SAND, -10, 110, -30, bench + 20, -10, 80);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(d.pouring() > 0, Component.literal("the bucket should open over the spoil: " + d.status())))
				.thenExecute(() -> {
					int dug = before - count(helper, Blocks.SAND, 33, 62, 5, 9, 30, 39);
					helper.assertTrue(dug == 25 && count(helper, Blocks.SAND, 63, 90, bench, bench + 20, 0, 70) == 0,
							Component.literal("25 dug, none set yet: " + dug + ", " + count(helper, Blocks.SAND, 63, 90, bench, bench + 20, 0, 70)));
				})
				.thenIdle(8)
				.thenExecute(() -> {
					d.stop(helper.getLevel(), false);
					int set = count(helper, Blocks.SAND, 63, 90, bench, bench + 20, 0, 70);
					helper.assertTrue(set < 25 && d.pouring() > 0, Component.literal("mid-pour: " + set + " set, " + d.pouring() + " on their way"));
					System.out.println("[tailings-test] pit_pour_dragline: casting at " + d.dumpAt() + " (the scene from " + helper.absolutePos(BlockPos.ZERO) + ")");
					d.discard();   // broken up mid-pour
					int cast = count(helper, Blocks.SAND, 63, 90, bench, bench + 20, 0, 70);
					int floating = 0;
					for (int x = 63; x <= 90; x++) {
						for (int y = bench; y <= bench + 20; y++) {
							for (int z = 0; z <= 70; z++) {
								if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.SAND) && helper.getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
									floating++;
								}
							}
						}
					}
					int now = count(helper, Blocks.SAND, -10, 110, -30, bench + 20, -10, 80);
					int items = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).inflate(120)).size();
					helper.assertTrue(now == all && items == 0, Component.literal("no sand lost: " + now + " of " + all + ", " + items + " dropped as items"));
					helper.assertTrue(cast >= 24 && floating == 0, Component.literal("set on the spoil, none floating: " + cast + ", " + floating));
				})
				.thenSucceed();
	}

	/**
	 * The wheel loader into a truck (open-pit.md, Pouring): each bucket is drawn pouring down into the body; the body's accounting is
	 * as it was (the truck fills), and nothing of the stream is left in the world.
	 */
	static void loader(GameTestHelper helper) {
		floor(helper, 0, 20, 57, 58, 0, 34);
		for (int x = 0; x <= 6; x++) {
			for (int z = 20; z <= 28; z++) {
				for (int y = 59; y <= 64; y++) {
					helper.setBlock(x, y, z, Blocks.SAND);
				}
			}
		}
		dev.wildcatter.tailings.entity.WheelLoaderEntity loader = machine(helper, TailingsEntities.WHEEL_LOADER, 3.5, 59, 12.3, 0);
		HaulTruckEntity truck = truck(helper, TailingsEntities.HAUL_TRUCK, 10.0, 59, 16.0, 0);
		loader.fastPasses(25);
		int sandBefore = count(helper, Blocks.SAND, 0, 20, 59, 70, 0, 34);
		int[] drawn = {0};
		helper.onEachTick(() -> {
			drawn[0] = Math.max(drawn[0], loader.shownPour().globs().size());
			if (!loader.busy() && truck.load().room() > 0) {
				loader.pressJump(helper.getLevel());
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.load().room() == 0, Component.literal("the truck should fill: " + truck.load().tonnes())))
				.thenWaitUntil(() -> helper.assertTrue(loader.pouring() == 0, Component.literal("its last stream ends: " + loader.pouring())))
				.thenExecute(() -> {
					helper.assertTrue(drawn[0] >= 3, Component.literal("each bucket is drawn pouring into the body: " + drawn[0]));
					long sandIn = truck.load().kg("minecraft:sand");
					int dug = sandBefore - count(helper, Blocks.SAND, 0, 20, 59, 70, 0, 34);
					helper.assertTrue(dug * 1_600L >= sandIn && sandIn > 85_000, Component.literal("the body holds what was dug, and the stream placed nothing: dug " + dug
							+ " blocks, " + sandIn + " kg in the body"));
				})
				.thenSucceed();
	}

}
