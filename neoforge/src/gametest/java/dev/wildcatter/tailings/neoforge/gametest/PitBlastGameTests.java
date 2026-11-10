package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.part;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.vehicle;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlock;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.BlastingMachineBlockEntity;
import dev.wildcatter.tailings.block.MatrixFillPointBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.entity.BlastholeDrillEntity;
import dev.wildcatter.tailings.entity.EmulsionTruckEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.WheelLoaderEntity;
import dev.wildcatter.tailings.item.BlastLoggerItem;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.Shot;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The open pit's P3 (docs/multiblocks/open-pit-p3-blasting.md): the blasthole drill, priming, charging from a stock tank through the
 * matrix fill point, stemming, logging and the blasting machine, and the blast: muck at 4 for 3, a blowout, a box cut, harm, misfires,
 * the guard and the PLC.
 */
final class PitBlastGameTests {
	static final String[] SCENES = {"pit_drill_pattern", "pit_drill_short", "pit_charge", "pit_prime_stem", "pit_blast", "pit_blast_blowout",
			"pit_blast_boxcut", "pit_misfire", "pit_blast_guard", "pit_blast_plc"};
	static final Map<String, Integer> TICKS = Map.of("pit_drill_pattern", 6000, "pit_drill_short", 1200, "pit_charge", 2400, "pit_blast", 1200,
			"pit_blast_blowout", 1200, "pit_blast_boxcut", 1200, "pit_misfire", 1200, "pit_blast_plc", 1200);
	static final Map<String, Integer> PADDING = Map.of("pit_drill_pattern", 80, "pit_blast", 160, "pit_blast_blowout", 140, "pit_blast_boxcut", 100,
			"pit_misfire", 120, "pit_blast_guard", 120, "pit_blast_plc", 120, "pit_charge", 60);

	private PitBlastGameTests() {
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "pit_drill_pattern" -> new int[] {-1, 3, -1, 3};
			case "pit_blast" -> new int[] {-1, 5, -1, 8};
			case "pit_blast_blowout" -> new int[] {-2, 4, -2, 6};
			case "pit_blast_boxcut", "pit_misfire", "pit_blast_guard", "pit_blast_plc" -> new int[] {-1, 4, -1, 6};
			default -> PitBigGameTests.reach(scene);
		};
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	private static void fill(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		ServerLevel level = helper.getLevel();
		BlockState state = block.defaultBlockState();
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), state, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** A hole as the drill leaves it: a column of blastholes from the collar down, each remembering its rock. */
	static BlastholeBlockEntity hole(GameTestHelper helper, int x, int collarY, int z, int depth, int row, int col, Direction face) {
		ServerLevel level = helper.getLevel();
		BlockPos collar = helper.absolutePos(new BlockPos(x, collarY, z));
		String toe = "";
		for (int m = 0; m < depth; m++) {
			BlockPos at = collar.below(m);
			toe = BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).toString();
			level.setBlock(at, TailingsBlocks.BLASTHOLE.defaultBlockState().setValue(BlastholeBlock.COLLAR, m == 0), Block.UPDATE_ALL);
			((BlastholeBlockEntity) level.getBlockEntity(at)).drilled(toe, collar);
		}
		BlastholeBlockEntity hole = (BlastholeBlockEntity) level.getBlockEntity(collar);
		hole.pattern(row, col, face, Blasting.BURDEN, Blasting.SPACING, Blasting.SUBDRILL);
		hole.finish(depth, false, toe);
		return hole;
	}

	/** Primed, charged to 12 m long enough ago to have gassed, and stemmed. */
	static void load(GameTestHelper helper, BlastholeBlockEntity hole, boolean booster, boolean detonator, double kg, double stemming) {
		if (booster) {
			hole.addBooster();
		}
		if (detonator) {
			hole.addDetonator(1000 + hole.row() * 100 + hole.col());
		}
		if (kg > 0) {
			hole.charge(kg, helper.getLevel().getGameTime() - Blasting.SLEEP_TICKS);
		}
		if (stemming > 0) {
			hole.stem(stemming);
		}
	}

	private static BlastingMachineBlockEntity machine(GameTestHelper helper, int x, int y, int z) {
		helper.setBlock(new BlockPos(x, y, z), TailingsBlocks.BLASTING_MACHINE);
		return helper.getBlockEntity(new BlockPos(x, y, z), BlastingMachineBlockEntity.class);
	}

	private static ItemStack logged(GameTestHelper helper, List<BlastholeBlockEntity> holes) {
		ItemStack logger = new ItemStack(TailingsItems.BLAST_LOGGER);
		for (BlastholeBlockEntity hole : holes) {
			BlastLoggerItem.log(helper.getLevel(), logger, hole.getBlockPos());
		}
		return logger;
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

	/** Quarters of bank rock held by all the muck in a region. */
	private static long quarters(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1) {
		long n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					if (helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, y, z))) instanceof MuckBlockEntity muck) {
						n += muck.held();
					}
				}
			}
		}
		return n;
	}

	private static void check(GameTestHelper helper, boolean ok, String why) {
		helper.assertTrue(ok, Component.literal(why));
	}

	/** Arms and fires a loaded machine, skipping the siren, and waits for the shot. */
	private static void fireAndWait(GameTestHelper helper, BlastingMachineBlockEntity machine, Runnable after) {
		machine.setKey(true);
		String armed = machine.arm();
		check(helper, armed.startsWith("ARMED"), "the machine should arm: " + armed);
		String fired = machine.fire();
		check(helper, fired.startsWith("SIREN"), "FIRE should sound the siren: " + fired);
		machine.sirenNow();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, !machine.firing() && machine.lastShot() != null, "the shot should fire: " + machine.status()))
				.thenExecute(after)
				.thenSucceed();
	}

	// ---- the drill ---------------------------------------------------------------------------------------------------------------

	/** A 3 x 3 staggered pattern drills 9 holes, each 16 m deep, each blasthole block remembering its rock (a granite layer at y 50). */
	static void drillPattern(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 40, 40, 59, 0, 40);
		fill(helper, Blocks.GRANITE, 0, 40, 50, 50, 0, 40);
		BlastholeDrillEntity drill = vehicle(helper, TailingsEntities.BLASTHOLE_DRILL, 30.5, 60, 10.5, 0);
		drill.fast(20);
		drill.setFuel(1_000);
		String go = drill.go();
		check(helper, go.startsWith("GO"), "GO should start the pattern: " + go);
		BlockPos origin = drill.holeColumn();
		double fuelAtStart = drill.fuel();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, !drill.running(), "the drill should walk the pattern: " + drill.status()))
				.thenExecute(() -> {
					check(helper, drill.holesDrilled() == 9 && drill.status().startsWith("DONE"), "9 holes, DONE: " + drill.status());
					// facing south, the rows run south 7 m apart and along a row west 8 m apart; row 2 is offset 4 m
					int[][] spots = {{0, 0}, {-8, 0}, {-16, 0}, {-4, 7}, {-12, 7}, {-20, 7}, {0, 14}, {-8, 14}, {-16, 14}};
					for (int[] s : spots) {
						BlockPos collar = new BlockPos(origin.getX() + s[0], 59 + helper.absolutePos(BlockPos.ZERO).getY(), origin.getZ() + s[1]);
						BlockState state = helper.getLevel().getBlockState(collar);
						check(helper, state.getBlock() instanceof BlastholeBlock && state.getValue(BlastholeBlock.COLLAR), "a collar at " + collar.toShortString() + ": " + state);
						BlastholeBlockEntity hole = (BlastholeBlockEntity) helper.getLevel().getBlockEntity(collar);
						check(helper, hole.depth() == 16 && !hole.shortHole(), "16 m deep: " + hole.readout());
						check(helper, hole.stage() == BlastholeBlock.Stage.DRILLED, "it reads DRILLED: " + hole.readout());
						for (int m = 0; m < 16; m++) {
							BlockPos at = collar.below(m);
							check(helper, helper.getLevel().getBlockEntity(at) instanceof BlastholeBlockEntity b
									&& b.replaced().equals(at.getY() == helper.absolutePos(new BlockPos(0, 50, 0)).getY() ? "minecraft:granite" : "minecraft:stone"),
									"each block remembers its rock, at " + at.toShortString());
						}
						check(helper, !(helper.getLevel().getBlockState(collar.below(16)).getBlock() instanceof BlastholeBlock), "and no deeper than 16 m");
					}
					check(helper, drill.fuel() < fuelAtStart, "it burns diesel: " + drill.fuel());
				})
				.thenSucceed();
	}

	/** A hole over a cave stops SHORT at the cave. */
	static void drillShort(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 24, 40, 59, 0, 24);
		fill(helper, Blocks.AIR, 8, 16, 50, 52, 0, 8);   // a cave under the mast
		BlastholeDrillEntity drill = vehicle(helper, TailingsEntities.BLASTHOLE_DRILL, 12.5, 60, 10.5, 0);
		drill.fast(20);
		drill.pressJump(helper.getLevel());
		BlockPos column = drill.holeColumn();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, drill.holesDrilled() == 1, "it should drill one hole: " + drill.status()))
				.thenExecute(() -> {
					BlockPos collar = new BlockPos(column.getX(), helper.absolutePos(new BlockPos(0, 59, 0)).getY(), column.getZ());
					BlastholeBlockEntity hole = (BlastholeBlockEntity) helper.getLevel().getBlockEntity(collar);
					check(helper, hole != null && hole.shortHole() && hole.depth() == 7, "SHORT at the cave, 7 m (y 59 to 53): " + (hole == null ? "no hole" : hole.readout()));
					check(helper, drill.status().contains("SHORT") && drill.status().contains("cave"), "it reads why: " + drill.status());
				})
				.thenSucceed();
	}

	// ---- charging, priming, stemming ---------------------------------------------------------------------------------------------

	/**
	 * A Wildcatter stock tank of emulsion matrix fills the truck through the matrix fill point at 1 t/min; the truck charges 3 primed
	 * holes to 12 m, 828 kg each, at 20 kg/s, and skips an unprimed one.
	 */
	static void charge(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 30, 40, 58, 0, 30);
		Block tankBlock = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "stock_tank"));
		var tank = part(helper, tankBlock, new BlockPos(0, 59, 0), dev.wildcatter.block.StorageTankBlockEntity.class);
		tank.testFill(Product.EMULSION_MATRIX, 5_000);
		helper.setBlock(new BlockPos(4, 59, 1), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "gas_line")));
		MatrixFillPointBlockEntity point = part(helper, TailingsBlocks.MATRIX_FILL_POINT, new BlockPos(5, 59, 1), MatrixFillPointBlockEntity.class);
		EmulsionTruckEntity truck = vehicle(helper, TailingsEntities.EMULSION_TRUCK, 9.5, 59, 4.5, 180);   // its hose reel toward the holes
		truck.setFuel(200);
		// four holes behind the truck, within its 12 m hose; one not primed
		BlastholeBlockEntity[] holes = new BlastholeBlockEntity[4];
		for (int i = 0; i < 4; i++) {
			holes[i] = hole(helper, 6 + 3 * i, 58, 12, 16, 0, i, Direction.NORTH);
			if (i < 3) {
				holes[i].addBooster();
				holes[i].addDetonator(500 + i);
			}
		}
		double tankBefore = tank.amount();
		helper.startSequence()
				.thenIdle(100)
				.thenExecute(() -> {
					// 5 s at 1 t/min is 83 kg
					check(helper, truck.matrixKg() > 70 && truck.matrixKg() < 90, "the fill point should give about 83 kg in 5 s: " + truck.matrixKg() + " (" + point.status() + ")");
					check(helper, tank.amount() < tankBefore - 70, "the matrix comes out of the stock tank: " + tank.amount());
					helper.setBlock(new BlockPos(5, 60, 1), Blocks.AIR);   // the fill point taken away, so it does not top the truck up while it pumps
					helper.setBlock(new BlockPos(5, 59, 1), Blocks.AIR);
					truck.setMatrix(Blasting.TRUCK_MATRIX_KG);
					String said = truck.chargeAll(helper.getLevel());
					check(helper, said.contains("charging 3 holes") && said.contains("NOT PRIMED (1)"), "3 holes to charge, 1 skipped: " + said);
				})
				.thenIdle(100)
				.thenExecute(() -> {
					double charged = 0;
					for (BlastholeBlockEntity h : holes) {
						charged += h.chargeKg();
					}
					check(helper, Math.abs(charged - 100) <= 1.01, "20 kg/s for 5 s is 100 kg: " + charged);
					check(helper, truck.busy(), "it stands while it pumps");
					truck.fastPump(20);
				})
				.thenWaitUntil(() -> check(helper, !truck.busy(), "it should charge all three: " + truck.status()))
				.thenExecute(() -> {
					for (int i = 0; i < 3; i++) {
						check(helper, Math.abs(holes[i].chargeKg() - 828) < 0.01 && Math.abs(holes[i].column() - 12) < 0.01,
								"hole " + i + " should hold 828 kg, 12 m: " + holes[i].readout());
						check(helper, holes[i].stage() == BlastholeBlock.Stage.CHARGED, "it reads CHARGED: " + holes[i].readout());
						check(helper, holes[i].readout().contains("sleeping"), "and sleeping while it gasses: " + holes[i].readout());
					}
					check(helper, holes[3].chargeKg() == 0, "the unprimed hole is skipped");
					check(helper, Math.abs(truck.matrixKg() - (Blasting.TRUCK_MATRIX_KG - 3 * 828)) < 0.01, "12 t less 2,484 kg: " + truck.matrixKg());
				})
				.thenSucceed();
	}

	/** Priming in either order; stemming from the cuttings; gravel; washing out; the logger's delays; the collar's tags. */
	static void primeStem(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 20, 40, 59, 0, 20);
		ServerLevel level = helper.getLevel();
		BlastholeBlockEntity a = hole(helper, 4, 59, 4, 16, 1, 2, Direction.NORTH);
		BlastholeBlockEntity b = hole(helper, 12, 59, 4, 16, 0, 0, Direction.NORTH);
		check(helper, a.addDetonator(77).took() && a.stage() == BlastholeBlock.Stage.DRILLED, "a detonator alone does not prime it");
		check(helper, a.addBooster().took() && a.stage() == BlastholeBlock.Stage.PRIMED, "with its booster it is PRIMED: " + a.readout());
		check(helper, helper.getBlockState(new BlockPos(4, 59, 4)).getValue(BlastholeBlock.STAGE) == BlastholeBlock.Stage.PRIMED, "its tag shows PRIMED");
		a.charge(828, level.getGameTime());
		check(helper, !a.addBooster().took(), "a second booster is refused once charged");
		BlastholeBlockEntity.Said stem = a.stemWithCuttings();
		check(helper, Math.abs(a.stemming() - 4) < 0.01 && a.stage() == BlastholeBlock.Stage.STEMMED, "its cuttings stem the 4 m above the charge: " + stem.text());
		check(helper, Math.abs(a.cuttings() - 8) < 0.01, "12 m of cuttings less the 4 m used: " + a.cuttings());
		check(helper, !a.washOut(level, null).took(), "a stemmed hole cannot be washed out");
		ItemStack logger = new ItemStack(TailingsItems.BLAST_LOGGER);
		String said = BlastLoggerItem.log(level, logger, a.getBlockPos());
		check(helper, a.logged() && a.delayMs() == 65 + 2 * 25 && a.stage() == BlastholeBlock.Stage.LOGGED, "row 2, hole 3 logs 115 ms: " + said);
		BlastLoggerItem.setDelays(logger, 42, 17);
		BlastLoggerItem.log(level, logger, a.getBlockPos());
		check(helper, a.delayMs() == 42 + 2 * 17 && BlastLoggerItem.holes(logger).size() == 1, "re-logged with new delays, once: " + a.delayMs());
		// the second hole: charged, no cuttings left (used elsewhere), stemmed with gravel; then a third washed out
		b.addBooster();
		b.addDetonator(78);
		b.charge(828, level.getGameTime());
		check(helper, b.stemWithGravel().took() && Math.abs(b.stemming() - 4) < 0.01, "a block of gravel stems the 4 m: " + b.stemming());
		BlastholeBlockEntity c = hole(helper, 8, 59, 14, 16, 0, 1, Direction.NORTH);
		c.addBooster();
		c.addDetonator(79);
		c.charge(500, level.getGameTime());
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		check(helper, c.washOut(level, player).took() && c.chargeKg() == 0 && !c.primed(), "an unstemmed hole washes out");
		check(helper, player.getInventory().countItem(TailingsItems.CAST_BOOSTER) == 1 && player.getInventory().countItem(TailingsItems.DETONATOR) == 1,
				"the booster and detonator come back");
		check(helper, c.stage() == BlastholeBlock.Stage.DRILLED, "and it reads DRILLED again");
		helper.succeed();
	}

	// ---- the blast ---------------------------------------------------------------------------------------------------------------

	/** A bench 14 m high, its face to the north at z 43, its top at y 59, its floor y 46, the pit floor y 45. */
	private static void bench(GameTestHelper helper, int x0, int x1, int faceZ, int z1) {
		fill(helper, Blocks.STONE, x0 - 16, x1 + 16, 40, 45, faceZ - 40, z1);
		fill(helper, Blocks.STONE, x0, x1, 46, 59, faceZ + 1, z1);
	}

	/**
	 * The spec's shot: a charged, primed, stemmed 3 x 3 pattern breaks to muck at 4 for 3, spilled off the free face; the bench floor is
	 * left flat at 14 m; the masses are right; iron golems 20 m and 40 m off take 18 and 6.
	 */
	static void blast(GameTestHelper helper) {
		bench(helper, 6, 49, 43, 110);
		BlastholeBlockEntity[] holes = new BlastholeBlockEntity[9];
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				holes[r * 3 + c] = hole(helper, 20 + 8 * c, 59, 50 + 7 * r, 16, r, c, Direction.NORTH);
				load(helper, holes[r * 3 + c], true, true, 828, 4);
			}
		}
		IronGolem near = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(28, 60, 84));
		IronGolem far = helper.spawn(EntityTypes.IRON_GOLEM, new BlockPos(28, 60, 104));
		near.setNoAi(true);
		far.setNoAi(true);
		BlastingMachineBlockEntity machine = machine(helper, 28, 60, 122);
		String loaded = machine.load(logged(helper, List.of(holes)));
		check(helper, loaded.startsWith("9 holes"), "the logger's 9 holes load: " + loaded);
		String armed = machine.arm();
		check(helper, armed.contains("NO KEY"), "no key, no arming: " + armed);
		fireAndWait(helper, machine, () -> {
			Shot shot = machine.lastShot();
			int bank = 24 * 21 * 14;
			StringBuilder left = new StringBuilder();
			for (int z = 44; z <= 64; z++) {
				int n = count(helper, Blocks.STONE, 16, 39, 46, 59, z, z);
				if (n > 0) {
					left.append(" z").append(z).append(':').append(n);
				}
			}
			for (int x = 16; x <= 39; x++) {
				int n = count(helper, Blocks.STONE, x, x, 46, 59, 44, 64);
				if (n > 0) {
					left.append(" x").append(x).append(':').append(n);
				}
			}
			check(helper, shot.bank() == bank && shot.misfires() == 0 && shot.flyrock() == 0, "the shot breaks 24 x 21 x 14 = 7,056 bank blocks: " + shot.summary() + " " + shot.notes() + "; stone left" + left);
			int muck = count(helper, TailingsBlocks.MUCK, -12, 67, 40, 90, 0, 110);
			check(helper, muck == bank * 4 / 3, "4 for 3: " + bank * 4 / 3 + " muck blocks, got " + muck);
			long q = quarters(helper, -12, 67, 40, 90, 0, 110);
			check(helper, q == 4L * bank, "every bank block's 4 quarters are in the muck: " + q + " of " + 4L * bank);
			check(helper, count(helper, TailingsBlocks.MUCK, -12, 67, 46, 70, 0, 43) > 500, "the swell spills forward off the face onto the pit floor");
			check(helper, count(helper, TailingsBlocks.BLASTHOLE, 0, 65, 40, 60, 40, 110) == 0, "no blasthole is left standing");
			check(helper, count(helper, Blocks.STONE, 16, 39, 45, 45, 44, 70) == 24 * 27, "the floor under the shot is solid at y 45: the subdrill is fractured, not broken");
			check(helper, count(helper, Blocks.STONE, 16, 39, 46, 59, 71, 71) == 24 * 14, "the bench behind the last row stands");
			check(helper, Math.abs(near.getHealth() - (near.getMaxHealth() - 18)) < 0.2, "20 m off takes 18: " + near.getHealth());
			check(helper, Math.abs(far.getHealth() - (far.getMaxHealth() - 6)) < 0.2, "40 m off takes 6: " + far.getHealth());
			check(helper, machine.status().startsWith("FIRED") && machine.readPulses(BlastingMachineBlockEntity.FIRED) == 1, "it reads FIRED: " + machine.status());
		});
	}

	/** An unstemmed hole blows out: the lower half of its box breaks, the top half stands, and it throws 6 blocks of flyrock. */
	static void blowout(GameTestHelper helper) {
		bench(helper, 6, 33, 43, 70);
		BlastholeBlockEntity hole = hole(helper, 20, 59, 50, 16, 0, 0, Direction.NORTH);
		load(helper, hole, true, true, 828, 0);
		BlastingMachineBlockEntity machine = machine(helper, 20, 60, 110);
		machine.load(logged(helper, List.of(hole)));
		machine.setKey(true);
		String armed = machine.arm();
		check(helper, armed.contains("will blow out"), "ARM warns of the short stemming: " + armed);
		machine.fire();
		machine.sirenNow();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, !machine.firing() && machine.lastShot() != null, "the shot should fire: " + machine.status()))
				.thenExecute(() -> {
					Shot shot = machine.lastShot();
					check(helper, shot.blowouts() == 1 && shot.flyrock() == Blasting.BLOWOUT_FLYROCK, "a blowout with 6 flyrock: " + shot.summary() + " " + shot.notes());
					check(helper, shot.bank() == 7 * 8 * 7, "it breaks half its box, 7 x 8 x 7: " + shot.bank());
					check(helper, count(helper, Blocks.STONE, 16, 23, 53, 59, 44, 50) == 8 * 7 * 7, "the top half of its box stands");
					check(helper, count(helper, TailingsBlocks.MUCK, 16, 23, 46, 52, 44, 50) == 8 * 7 * 7, "the lower half is muck");
				})
				.thenSucceed();
	}

	/** With no free face the hole still breaks, centred on itself, and its swell heaves up over the shot. */
	static void boxCut(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 40, 40, 59, 0, 40);
		BlastholeBlockEntity hole = hole(helper, 20, 59, 20, 16, 0, 0, Direction.NORTH);
		load(helper, hole, true, true, 828, 4);
		BlastingMachineBlockEntity machine = machine(helper, 20, 60, 80);
		machine.load(logged(helper, List.of(hole)));
		fireAndWait(helper, machine, () -> {
			Shot shot = machine.lastShot();
			check(helper, shot.bank() == 7 * 8 * 14, "the box cut breaks its 7 x 8 x 14 box: " + shot.summary());
			int above = count(helper, TailingsBlocks.MUCK, 0, 40, 60, 75, 0, 40);
			check(helper, above == (shot.bank() + 2) / 3, "the swell heaves up over the shot: " + above + " muck blocks above the bench");
			check(helper, count(helper, TailingsBlocks.MUCK, 16, 23, 46, 59, 17, 23) == 7 * 8 * 14, "the box is muck, centred on the hole");
		});
	}

	/**
	 * A hole with no booster misfires: it keeps its charge and stands as blastholes, marked red; the next shot waits 30 s; a loader's
	 * bucket stops at it; washing it out clears it.
	 */
	static void misfire(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 40, 40, 59, 0, 70);
		fill(helper, Blocks.AIR, 17, 23, 54, 59, 2, 19);   // a trench in front of the hole, to reach it with a loader
		BlastholeBlockEntity hole = hole(helper, 20, 59, 25, 16, 0, 0, Direction.NORTH);
		load(helper, hole, false, true, 828, 4);
		BlastingMachineBlockEntity machine = machine(helper, 20, 60, 90);
		machine.load(logged(helper, List.of(hole)));
		machine.setKey(true);
		String armed = machine.arm();
		check(helper, armed.contains("0 of 1 holes answer") && armed.contains("no booster"), "the test finds the hole will not fire: " + armed);
		machine.fire();
		machine.sirenNow();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, !machine.firing() && machine.lastShot() != null, "the shot should fire: " + machine.status()))
				.thenExecute(() -> {
					check(helper, machine.lastShot().misfires() == 1 && machine.readPulses(BlastingMachineBlockEntity.MISFIRES) == 1, "one misfire: " + machine.status());
					check(helper, hole.misfired() && hole.chargeKg() == 828 && hole.stage() == BlastholeBlock.Stage.MISFIRED, "it keeps its charge, marked red: " + hole.readout());
					check(helper, count(helper, TailingsBlocks.BLASTHOLE, 20, 20, 44, 59, 25, 25) == 16, "its column stands");
					String again = machine.arm();
					check(helper, again.startsWith("WAIT"), "the next shot waits 30 s: " + again);
					WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 20.5, 54, 16.5, 0);
					loader.pressJump(helper.getLevel());
					check(helper, loader.status().equals("misfire in the muck: stop") && loader.bucket().empty(), "the bucket stops at the misfire: " + loader.status());
					Player player = helper.makeMockPlayer(GameType.SURVIVAL);
					check(helper, hole.washOut(helper.getLevel(), player).took() && !hole.misfired() && hole.chargeKg() == 0, "washing it out clears it: " + hole.readout());
					loader.pressJump(helper.getLevel());
					check(helper, !loader.status().contains("misfire"), "and the loader goes on: " + loader.status());
				})
				.thenSucceed();
	}

	/** A firing point 40 m away reads TOO CLOSE; one 600 m away TOO FAR; a truck in the shot blocks arming. */
	static void guard(GameTestHelper helper) {
		bench(helper, 6, 33, 43, 70);
		BlastholeBlockEntity hole = hole(helper, 20, 59, 50, 16, 0, 0, Direction.NORTH);
		load(helper, hole, true, true, 828, 4);
		ItemStack logger = logged(helper, List.of(hole));
		BlastingMachineBlockEntity close = machine(helper, 20, 60, 90);
		check(helper, close.load(logger).startsWith("TOO CLOSE"), "40 m is inside the danger zone: " + close.status());
		BlastingMachineBlockEntity ok = machine(helper, 20, 60, 101);
		check(helper, ok.load(logger).startsWith("1 holes"), "51 m loads: " + ok.status());
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 20.5, 60, 47.5, 90);
		ok.setKey(true);
		helper.startSequence()
				.thenIdle(5)   // the truck is in the world's entity lists from the next tick
				.thenExecute(() -> {
					String armed = ok.arm();
					check(helper, armed.startsWith("MACHINE IN THE SHOT"), "a truck on the shot blocks arming: " + armed);
					truck.discard();
				})
				.thenIdle(2)
				.thenExecute(() -> check(helper, ok.arm().startsWith("ARMED"), "moved, it arms: " + ok.status()))
				.thenSucceed();
	}

	/** A PLC arms and fires with the key in, and cannot without it. */
	static void plc(GameTestHelper helper) {
		bench(helper, 6, 33, 43, 70);
		BlastholeBlockEntity hole = hole(helper, 20, 59, 50, 16, 0, 0, Direction.NORTH);
		load(helper, hole, true, true, 828, 4);
		BlastingMachineBlockEntity machine = machine(helper, 20, 60, 110);
		machine.load(logged(helper, List.of(hole)));
		long t = helper.getLevel().getGameTime();
		check(helper, Math.abs(machine.readMa(BlastingMachineBlockEntity.HOLES) - 4.08) < 0.01, "HOLES reads 1 of 200: " + machine.readMa(BlastingMachineBlockEntity.HOLES));
		machine.commandPoint(BlastingMachineBlockEntity.ARM, true, t);
		check(helper, !machine.readPoint(BlastingMachineBlockEntity.ARMED) && machine.status().equals("NO KEY"), "without the key the PLC cannot arm: " + machine.status());
		machine.commandPoint(BlastingMachineBlockEntity.ARM, false, t);
		machine.commandPoint(BlastingMachineBlockEntity.FIRE, true, t);
		check(helper, !machine.firing(), "nor fire");
		machine.commandPoint(BlastingMachineBlockEntity.FIRE, false, t);
		machine.setKey(true);
		machine.commandPoint(BlastingMachineBlockEntity.ARM, true, t);
		check(helper, machine.readPoint(BlastingMachineBlockEntity.ARMED), "with the key it arms: " + machine.status());
		machine.commandPoint(BlastingMachineBlockEntity.FIRE, true, t);
		check(helper, machine.firing(), "and FIRE's rising edge fires: " + machine.status());
		machine.sirenNow();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, machine.readPulses(BlastingMachineBlockEntity.FIRED) == 1, "FIRED pulses: " + machine.status()))
				.thenExecute(() -> check(helper, machine.lastShot().bank() > 0, "the shot broke rock: " + machine.status()))
				.thenSucceed();
	}
}
