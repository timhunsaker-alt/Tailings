package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.plant.BeltGrade;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Belts on a grade (crushing-circuit.md, "A belt on a grade"): a run laid a block up or down every 1, 2, 3 or 4 blocks is drawn as one
 * straight belt. Each block works out its belt's heights from its run, every block meets the next, the grade is the steps' spacing,
 * the shape follows the incline, and breaking a block mid-run grades both halves again (and putting it back, the whole run).
 */
final class BeltSlopeGameTests {
	static final String[] SCENES = {"belt_grade_up", "belt_grade_down", "belt_grade_reforms"};
	static final Set<String> LONG = Set.of();
	private static final int FLAT = 3, STEPS = 5;

	private BeltSlopeGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("belt_grade_up", helper -> grades(helper, 1));
		out.accept("belt_grade_down", helper -> grades(helper, -1));
		out.accept("belt_grade_reforms", BeltSlopeGameTests::reforms);
	}

	private static BlockState belt(Block kind) {
		return kind.defaultBlockState().setValue(BeltConveyorBlock.AXIS, Direction.Axis.Z);
	}

	/** A run along +z at column x: FLAT level blocks, a step of {@code way} every {@code every} blocks STEPS times, FLAT more. */
	private static List<BlockPos> lay(GameTestHelper helper, Block kind, int x, int every, int way) {
		List<BlockPos> laid = new ArrayList<>();
		int y = way > 0 ? 2 : 2 + STEPS;
		int length = FLAT * 2 + every * STEPS;
		for (int z = 0; z < length; z++) {
			if (z >= FLAT && z < FLAT + every * STEPS && (z - FLAT) % every == 0) {
				y += way;
			}
			BlockPos at = new BlockPos(x, y, z);
			helper.setBlock(at, belt(kind));
			laid.add(helper.absolutePos(at));
		}
		return laid;
	}

	private static float[] grade(GameTestHelper helper, BlockPos abs) {
		if (!(helper.getLevel().getBlockEntity(abs) instanceof BeltBlockEntity belt)) {
			throw new AssertionError("no belt at " + abs);
		}
		return belt.grade().clone();
	}

	/** Every block of the run meets the next, both ends are flat, and each block's state says whether it is sloped. */
	private static void meets(GameTestHelper helper, List<BlockPos> run, String what) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(BeltConveyorBlock.run(level, run.get(0)).equals(run), Component.literal(what + ": one run of " + run.size()));
		for (int i = 0; i < run.size(); i++) {
			float[] g = grade(helper, run.get(i));
			BlockState state = level.getBlockState(run.get(i));
			helper.assertTrue(state.getValue(BeltConveyorBlock.SLOPED) == BeltGrade.sloped(new double[] {g[0], g[1], g[2]}),
					Component.literal(what + " block " + i + ": its state says sloped as its heights do"));
			if (i + 1 < run.size()) {
				float[] n = grade(helper, run.get(i + 1));
				float here = run.get(i).getY() + g[2], there = run.get(i + 1).getY() + n[0];
				helper.assertTrue(Math.abs(here - there) < 1e-4, Component.literal(what + ": block " + i + " ends at " + here + ", block " + (i + 1) + " starts at " + there));
			}
		}
		helper.assertTrue(Math.abs(grade(helper, run.get(0))[0] - BeltGrade.FLAT) < 1e-4 && Math.abs(grade(helper, run.get(run.size() - 1))[2] - BeltGrade.FLAT) < 1e-4,
				Component.literal(what + ": both ends flat"));
	}

	/**
	 * belt_grade_up / belt_grade_down: 1-in-1 to 1-in-4 runs of both belts going up (down) along +z. Each block meets the next; on the
	 * grade each block climbs (falls) 1 in its spacing, straight through its middle; the flats either side stay flat with their baked
	 * model; and a sloped block's shape rises with it, so a player walks up the incline.
	 */
	private static void grades(GameTestHelper helper, int way) {
		List<List<BlockPos>> runs = new ArrayList<>();
		List<String> names = new ArrayList<>();
		int x = 0;
		for (Block kind : new Block[] {TailingsBlocks.BELT_CONVEYOR, TailingsBlocks.HEAVY_BELT_CONVEYOR}) {
			for (int every = 1; every <= 4; every++) {
				runs.add(lay(helper, kind, x, every, way));
				names.add((kind == TailingsBlocks.BELT_CONVEYOR ? "600 mm" : "heavy") + " 1 in " + every + (way > 0 ? " up" : " down"));
				x += 2;
			}
		}
		helper.runAfterDelay(3, () -> {
			ServerLevel level = helper.getLevel();
			for (int r = 0; r < runs.size(); r++) {
				List<BlockPos> run = runs.get(r);
				String what = names.get(r);
				int every = r % 4 + 1;
				meets(helper, run, what);
				for (int i = FLAT; i < FLAT + every * (STEPS - 1); i++) {
					float[] g = grade(helper, run.get(i));
					float half = way / (2f * every);
					helper.assertTrue(Math.abs(g[1] - g[0] - half) < 1e-4 && Math.abs(g[2] - g[1] - half) < 1e-4,
							Component.literal(what + " block " + i + ": climbs " + (g[2] - g[0]) + ", not " + (2 * half)));
					BlockState state = level.getBlockState(run.get(i));
					double top = state.getShape(level, run.get(i), CollisionContext.empty()).max(Direction.Axis.Y);
					double highest = Math.max(g[0], g[2]);
					helper.assertTrue(state.getValue(BeltConveyorBlock.SLOPED) && top > highest - 0.25 && top < highest + 0.1,
							Component.literal(what + " block " + i + ": its shape's top " + top + " follows the belt up to " + highest));
				}
				for (int i : new int[] {0, run.size() - 1}) {
					helper.assertTrue(!level.getBlockState(run.get(i)).getValue(BeltConveyorBlock.SLOPED), Component.literal(what + ": the flat end " + i + " stays flat"));
				}
			}
			helper.succeed();
		});
	}

	/**
	 * belt_grade_reforms: a 1-in-2 run; breaking a block on the grade leaves two runs, each graded again to end flat at the gap;
	 * putting the block back grades the whole run as it was.
	 */
	private static void reforms(GameTestHelper helper) {
		List<BlockPos> run = lay(helper, TailingsBlocks.BELT_CONVEYOR, 0, 2, 1);
		int gap = FLAT + 4;
		float[][] before = new float[run.size()][];
		helper.runAfterDelay(3, () -> {
			meets(helper, run, "the whole run");
			for (int i = 0; i < run.size(); i++) {
				before[i] = grade(helper, run.get(i));
			}
			helper.getLevel().destroyBlock(run.get(gap), false);
		});
		helper.runAfterDelay(6, () -> {
			List<BlockPos> tail = run.subList(0, gap), head = run.subList(gap + 1, run.size());
			meets(helper, tail, "the tail half");
			meets(helper, head, "the head half");
			helper.assertTrue(Math.abs(grade(helper, run.get(gap - 1))[2] - before[gap - 1][2]) > 0.1 || Math.abs(grade(helper, run.get(gap + 1))[0] - before[gap + 1][0]) > 0.1,
					Component.literal("the blocks beside the gap are graded again"));
			helper.getLevel().setBlock(run.get(gap), belt(TailingsBlocks.BELT_CONVEYOR), Block.UPDATE_ALL);
		});
		helper.runAfterDelay(9, () -> {
			meets(helper, run, "the run put back");
			for (int i = 0; i < run.size(); i++) {
				float[] g = grade(helper, run.get(i));
				for (int k = 0; k < 3; k++) {
					helper.assertTrue(Math.abs(g[k] - before[i][k]) < 1e-4, Component.literal("block " + i + " is graded as before"));
				}
			}
			helper.succeed();
		});
	}
}
