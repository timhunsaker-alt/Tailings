package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.pit.Crest;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitFuel;
import dev.wildcatter.tailings.pit.PitLoad;
import dev.wildcatter.tailings.pit.PitMass;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The track dozer (docs/multiblocks/open-pit-p4-dry-stack.md): a 70 t crawler with a 5 x 2 block semi-U blade that carries 18 blocks.
 * A player drives it; jump steps the blade CARRY, CUT, RAISED; sprint steps the setting for the blade's position (CUT: the grade it
 * cuts to; CARRY: a berm 1 or 2 m high). In CARRY it takes the loose ground and muck in front into the blade and spills the extra off
 * its ends; in CUT it also cuts the layer under its front, a block down each N metres for the grade; reversing it leaves the load; at a
 * crest it pushes the load over; along a crest it drops a berm. Standing hard rock stops it.
 */
public class TrackDozerEntity extends PitVehicleEntity {
	public enum Blade {
		CARRY, CUT, RAISED;

		Blade next() {
			return values()[(ordinal() + 1) % values().length];
		}

		/** The blade's height for the model: cut low, carry at the tracks, raised. */
		float lift() {
			return switch (this) {
				case CUT -> 0F;
				case CARRY -> 0.5F;
				case RAISED -> 1F;
			};
		}
	}

	/** The blade: 5 blocks wide, 18 blocks of load. */
	public static final int BLADE_HALF = 2;
	public static final int BLADE_BLOCKS = 18;

	private Blade blade = Blade.CARRY;
	private DryStack.Grade grade = DryStack.Grade.LEVEL;
	private int bermHigh = 1;
	private int bladeBlocks;
	/** CUT: the lowest y it cuts to, where it started cutting, and the metres cut since. */
	private int cutLevel = Integer.MIN_VALUE;
	private int cutStart;
	private double cutRun;
	private double bermRun;
	private boolean dozing;
	private boolean lastSprint;
	private int spillSide = 1;

	public TrackDozerEntity(EntityType<?> type, Level level) {
		super(type, level);
		setLift(blade.lift());
	}

	/** Parked with its engine off it is silent, whatever its blade's height. */
	@Override
	public net.minecraft.sounds.@Nullable SoundEvent loop() {
		if (!isVehicle() && !engineSynced() && Math.abs(syncedSpeed()) < 0.05F) {
			return null;
		}
		return super.loop() == null ? dev.wildcatter.WildcatterSounds.DIESEL_IDLE : super.loop();
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.DOZER;
	}

	@Override
	protected Item item() {
		return TailingsItems.TRACK_DOZER;
	}

	@Override
	public double tank() {
		return PitFuel.DOZER_TANK_L;
	}

	@Override
	protected double workFraction() {
		return dozing ? PitFuel.DOZING : 0;
	}

	@Override
	public boolean busy() {
		return false;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(0, 3.0, -0.6);
	}

	@Override
	protected double speedCap() {
		return blade != Blade.RAISED && (bladeBlocks > 0 || blade == Blade.CUT) ? PitDrive.DOZING_SPEED : Double.MAX_VALUE;
	}

	public Blade blade() {
		return blade;
	}

	public DryStack.Grade grade() {
		return grade;
	}

	public int bermHigh() {
		return bermHigh;
	}

	public int bladeBlocks() {
		return bladeBlocks;
	}

	/** Test hooks: the blade, its grade and the berm height, as the keys set them. */
	public void setBlade(Blade blade) {
		this.blade = blade;
		cutLevel = Integer.MIN_VALUE;
		setLift(blade.lift());
	}

	public void setGrade(DryStack.Grade grade) {
		this.grade = grade;
	}

	/** Test hook: a blade already carrying {@code blocks} of {@code id}. */
	public void loadBlade(String id, int blocks) {
		load.add(id, (long) blocks * PitMass.kg(id));
		bladeBlocks = blocks;
		refreshLoad();
	}

	public void setBermHigh(int high) {
		this.bermHigh = Math.max(1, Math.min(2, high));
	}

	private void say(@Nullable Player driver, String what) {
		status = what;
		if (driver != null) {
			driver.sendOverlayMessage(Component.literal("Track Dozer: " + what));
		}
	}

	/** Jump steps the blade. */
	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		setBlade(blade.next());
		say(driver, "blade " + blade.name() + describeSetting());
	}

	private String describeSetting() {
		return switch (blade) {
			case CUT -> ", cutting to " + grade.label();
			case CARRY -> ", berm " + bermHigh + " m";
			case RAISED -> "";
		};
	}

	/** Sprint steps the setting for the blade's position: the grade in CUT, the berm's height in CARRY. */
	private void setting(@Nullable Player driver) {
		if (blade == Blade.CUT) {
			grade = grade.next();
			cutStart = cutLevel;
			cutRun = 0;
		} else if (blade == Blade.CARRY) {
			bermHigh = bermHigh == 1 ? 2 : 1;
		}
		say(driver, "blade " + blade.name() + describeSetting());
	}

	@Override
	protected void beforeMove(ServerLevel level) {
		Input keys = keys();
		Player driver = getFirstPassenger() instanceof Player p ? p : null;
		if (keys.sprint() && !lastSprint) {
			setting(driver);
		}
		lastSprint = keys.sprint();
		dozing = false;
		int trackY = (int) Math.floor(getY() + 0.01);
		if (speed() < -0.05 && blade == Blade.CARRY && !load.empty()) {
			// reversing, it leaves the load in front where it stood
			Loose.tip(level, BlockPos.containing(local(0, 0.5, spec().length() / 2 + 1.0)), load);
			emptied();
			say(driver, "load left in front");
			return;
		}
		if (speed() <= 0.05) {
			return;
		}
		Crest.Ground ground = HaulTruckEntity.crestGround(level);
		// at a crest: the ground falls away under its front; it pushes the load over and goes no further
		Vec3 nose = local(0, 0, spec().length() / 2 + 1.0);
		if (!ground.solid((int) Math.floor(nose.x), trackY - 1, (int) Math.floor(nose.z)) && !ground.solid((int) Math.floor(nose.x), trackY - 2, (int) Math.floor(nose.z))) {
			if (!load.empty() && blade != Blade.RAISED) {
				Loose.tip(level, BlockPos.containing(nose.x, trackY + 0.5, nose.z), load);
				emptied();
				say(driver, "at the crest: pushed the load over");
			} else if (!status.startsWith("at the crest")) {
				say(driver, "at the crest");
			}
			stop();
			return;
		}
		if (blade == Blade.RAISED) {
			status = "travelling, blade raised";
			return;
		}
		if (blade == Blade.CUT) {
			if (cutLevel == Integer.MIN_VALUE) {
				cutStart = trackY - 1;   // the blade a block below its tracks
				cutRun = 0;
			}
			cutRun += speed() * TICK;
			cutLevel = cutStart - grade.stepsAfter(cutRun);
		}
		int low = blade == Blade.CUT ? Math.min(cutLevel, trackY) : trackY;
		String hard = null;
		Vec3 front = local(0, 0, spec().length() / 2 + 0.5);
		Vec3 across = right();
		for (int side = -BLADE_HALF; side <= BLADE_HALF && hard == null; side++) {
			int x = (int) Math.floor(front.x + across.x * side), z = (int) Math.floor(front.z + across.z * side);
			for (int y = low; y <= trackY + 3 && hard == null; y++) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || !state.getFluidState().isEmpty() || state.getBlock() instanceof dev.wildcatter.tailings.block.SurveyStakeBlock) {
					continue;
				}
				if (take(level, pos, state)) {
					dozing = true;
				} else if (state.canBeReplaced()) {
					level.destroyBlock(pos, false);
				} else if (y <= trackY + 1) {
					hard = state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? "hard rock: blast it first" : "blocked by " + state.getBlock().getName().getString();
				}
			}
		}
		if (hard != null) {
			stop();
			say(driver, hard);
			return;
		}
		if (blade == Blade.CUT) {
			dozing = true;
			status = String.format(java.util.Locale.ROOT, "cutting to %s: %d down, %.0f m; blade %d of %d", grade.label(), trackY - cutLevel, cutRun, bladeBlocks, BLADE_BLOCKS);
		} else if (!berm(level)) {
			status = bladeBlocks > 0 ? "pushing " + bladeBlocks + " blocks" : "blade at the tracks";
		}
	}

	/** Takes a loose block in the blade's way into the load, or spills it off the blade's end when the blade is full. */
	private boolean take(ServerLevel level, BlockPos pos, BlockState state) {
		PitLoad one = new PitLoad(Long.MAX_VALUE);
		if (state.getBlock() == TailingsBlocks.MUCK && level.getBlockEntity(pos) instanceof MuckBlockEntity muck) {
			muck.kilograms().forEach(one::add);
		} else {
			String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
			if (!PitMass.soft(id)) {
				return false;
			}
			one.add(id, PitMass.kg(id));
		}
		level.removeBlock(pos, false);
		if (bladeBlocks < BLADE_BLOCKS) {
			one.contents().forEach(load::add);
			bladeBlocks++;
			refreshLoad();
		} else {
			// past 18 the extra spills off the blade's ends, as two windrows
			spillSide = -spillSide;
			Loose.tip(level, BlockPos.containing(local(spillSide * (spec().width() / 2 + 2.0), 0.5, spec().length() / 2 + 1.0)), one);
		}
		return true;
	}

	/** Along a crest, with the crest on its right or left, it drops a berm 1 or 2 high at a block a metre until the load is spent. */
	private boolean berm(ServerLevel level) {
		if (load.empty()) {
			return false;
		}
		Crest.Ground ground = HaulTruckEntity.crestGround(level);
		int y = (int) Math.floor(getY() + 0.01);
		for (int side : new int[] {1, -1}) {
			for (int d = 2; d <= 5; d++) {
				Vec3 at = local(side * (spec().width() / 2 + d), 0, 0);
				if (Crest.drop(ground, (int) Math.floor(at.x), y, (int) Math.floor(at.z))) {
					bermRun += speed() * TICK;
					Vec3 b = local(side * (spec().width() / 2 + d - 1), 0, 0);
					BlockPos base = BlockPos.containing(b.x, y, b.z);
					if (bermRun >= 1) {
						bermRun = Math.max(0, bermRun - 1);
						for (int up = 0; up < bermHigh && !load.empty(); up++) {
							BlockPos pos = base.above(up);
							if (level.getBlockState(pos).canBeReplaced()) {
								placeOne(level, pos);
							}
						}
					}
					dozing = true;
					status = "pushing up a berm " + bermHigh + " m along the crest: " + bladeBlocks + " blocks left";
					if (load.empty()) {
						emptied();
					}
					return true;
				}
			}
		}
		return false;
	}

	/** Places one block from the load: soil and dry tails as themselves, rock as a block of muck. */
	private void placeOne(ServerLevel level, BlockPos pos) {
		Map.Entry<String, Long> first = load.contents().entrySet().iterator().next();
		String id = first.getKey();
		if (PitMass.ownBlock(id)) {
			Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
			level.setBlock(pos, (block == null || block == Blocks.AIR ? Blocks.DIRT : block).defaultBlockState(), Block.UPDATE_ALL);
			load.take(id, PitMass.kg(id));
		} else {
			Map<String, Integer> quarters = new LinkedHashMap<>();
			int q = 0;
			for (Map.Entry<String, Long> e : Map.copyOf(load.contents()).entrySet()) {
				while (q < PitMass.MUCK_QUARTERS && load.kg(e.getKey()) >= PitMass.kg(e.getKey()) / 4) {
					quarters.merge(e.getKey(), 1, Integer::sum);
					load.take(e.getKey(), PitMass.kg(e.getKey()) / 4);
					q++;
				}
				if (q >= PitMass.MUCK_QUARTERS) {
					break;
				}
			}
			if (quarters.isEmpty()) {
				load.take(id, Long.MAX_VALUE);
				return;
			}
			level.setBlock(pos, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(pos) instanceof MuckBlockEntity muck) {
				muck.set(quarters);
			}
		}
		bladeBlocks = Math.max(0, bladeBlocks - 1);
		refreshLoad();
	}

	private void emptied() {
		load.clear();
		bladeBlocks = 0;
		refreshLoad();
	}

	@Override
	public Component readout() {
		return Component.literal(super.readout().getString() + "; blade " + blade.name() + describeSetting() + ", " + bladeBlocks + " of " + BLADE_BLOCKS + " blocks");
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		try {
			blade = Blade.valueOf(input.getStringOr("blade", "CARRY"));
			grade = DryStack.Grade.valueOf(input.getStringOr("grade", "LEVEL"));
		} catch (IllegalArgumentException ex) {
			blade = Blade.CARRY;
			grade = DryStack.Grade.LEVEL;
		}
		bermHigh = input.getIntOr("berm_high", 1);
		bladeBlocks = input.getIntOr("blade_blocks", 0);
		setLift(blade.lift());
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("blade", blade.name());
		output.putString("grade", grade.name());
		output.putInt("berm_high", bermHigh);
		output.putInt("blade_blocks", bladeBlocks);
	}
}
