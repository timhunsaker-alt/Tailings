package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.pit.Crest;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.Pour;
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
 * crest it pushes the load over; along a crest it drops a berm. Standing hard rock stops it. Set to SPREAD (CARRY's third setting), it
 * lays its load in a 1 m lift at the level its tracks stand on, filling the ground in front of the blade, a block a column a metre:
 * pushing a tipped load across a stack's lift it spreads it into that lift (heavy-iron-fixes).
 * <p>
 * Tim, 2026-11-23: it runs itself in an area marked by two DOZE AREA stakes (its autopilot: the survey book starts and stops it, a
 * player in its cab takes it over), working lanes a blade wide across the area in SPREAD, back to each lane's start in reverse; and a
 * pass of its tracks compacts the loose cake (dry tails) under it ({@link dev.wildcatter.tailings.block.DryTailsBlock}).
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
	/** CARRY's setting is a berm (1 or 2 m high) or spreading: the berm height 0 means SPREAD. */
	private double spreadRun;
	private boolean dozing;
	private boolean lastSprint;
	private int spillSide = 1;
	/** Loose cake its tracks have compacted (tests, readout). */
	private int compactedBlocks;

	/**
	 * Its autopilot's legs (Tim, 2026-11-23): back down the lane it pushed to its start, over to the next lane's start, squared up to the
	 * push, pushing the lane.
	 */
	public enum Leg {
		BACK, TO_LANE, ALIGN, PUSH
	}

	/** The autopilot's area: its corners' block x and z, inclusive {minX, minZ, maxX, maxZ}, or null; whether it runs. */
	private int @Nullable [] autoArea;
	private boolean autoOn;
	private float pushYaw;
	private int lane;
	/** The lane it is reversing back down. */
	private int backLane;
	private int laneStep = 1;
	private int passes;
	private Leg leg = Leg.TO_LANE;
	private int stuckTicks;
	private boolean autoWaiting;
	/** The autopilot's speed limit now, m/s. */
	private double autoCap = Double.MAX_VALUE;
	/** Pivoting on its tracks, degrees a tick at most; and how close counts as at a lane's start, m. */
	private static final float PIVOT = 3F;
	private static final double ARRIVED = 0.45;
	/** Reversing between lanes it goes no faster than this, m/s (9 km/h), slowing to a crawl over its last 3 m. */
	private static final double BACK_SPEED = 2.5;

	public TrackDozerEntity(EntityType<?> type, Level level) {
		super(type, level);
		setLift(blade.lift());
	}

	/** Parked with its engine off it is silent, whatever its blade's height; moving, its own loop of diesel and track clank. */
	@Override
	public net.minecraft.sounds.@Nullable SoundEvent loop() {
		if (!isVehicle() && !engineSynced() && Math.abs(syncedSpeed()) < 0.05F) {
			return null;
		}
		net.minecraft.sounds.SoundEvent diesel = super.loop();
		if (diesel == dev.wildcatter.WildcatterSounds.DIESEL_LOAD) {
			return dev.wildcatter.tailings.TailingsSounds.TRACK_DOZER;   // moving: its diesel working and its tracks clanking
		}
		return diesel == null ? dev.wildcatter.WildcatterSounds.DIESEL_IDLE : diesel;
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
		double cap = blade != Blade.RAISED && (bladeBlocks > 0 || blade == Blade.CUT) ? PitDrive.DOZING_SPEED : Double.MAX_VALUE;
		return autoOn && !isVehicle() ? Math.min(cap, autoCap) : cap;
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

	/** Test hook: CARRY set to SPREAD. */
	public void setSpread() {
		this.bermHigh = 0;
	}

	public boolean spreading() {
		return bermHigh == 0;
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
			case CARRY -> bermHigh == 0 ? ", spreading a 1 m lift" : ", berm " + bermHigh + " m";
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
			bermHigh = bermHigh == 1 ? 2 : bermHigh == 2 ? 0 : 1;   // berm 1 m, berm 2 m, spread
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
			// reversing, it leaves the load in front where it stood: it slumps off the blade, clear of the tracks
			int n = Loose.blocks(load);
			spill(level, load, BlockPos.containing(local(0, 0.5, spec().length() / 2 + 1.0 + Pour.reach(n))), 0, 12, Math.min(40, 3 * n));
			emptied();
			say(driver, "load left in front");
			return;
		}
		if (speed() <= 0.05) {
			return;
		}
		Crest.Ground ground = HaulTruckEntity.crestGround(level);
		// at a crest: the ground falls away under its front; it pushes the load over and goes no further. Cutting, the floor under its
		// nose is its own cut's: on a grade the cut steps down a block before the tracks have come down into the last step (its box is
		// 8.8 m long, a step 10 m at 10 %), so its own cut, two blocks under the tracks, is not a crest (pit_dozer_cut failed now and then)
		int floor = blade == Blade.CUT && cutLevel != Integer.MIN_VALUE ? Math.min(trackY, cutLevel) : trackY;
		Vec3 nose = local(0, 0, spec().length() / 2 + 1.0);
		if (!ground.solid((int) Math.floor(nose.x), floor - 1, (int) Math.floor(nose.z)) && !ground.solid((int) Math.floor(nose.x), floor - 2, (int) Math.floor(nose.z))) {
			if (!load.empty() && blade != Blade.RAISED) {
				spill(level, load, BlockPos.containing(nose.x, trackY + 0.5, nose.z), 0, 16, Math.min(40, 3 * Loose.blocks(load)));
				emptied();
				say(driver, "at the crest: pushed the load over");
			} else if (!status.startsWith("at the crest")) {
				say(driver, "at the crest");
			}
			stop();
			return;
		}
		if (blade == Blade.RAISED) {
			if (!autoOn) {
				status = "travelling, blade raised";
			}
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
		} else if (bermHigh == 0) {
			if (!spread(level)) {
				status = bladeBlocks > 0 ? "pushing " + bladeBlocks + " blocks to spread" : "spreading: blade empty";
			}
		} else if (!berm(level)) {
			status = bladeBlocks > 0 ? "pushing " + bladeBlocks + " blocks" : "blade at the tracks";
		}
	}

	/**
	 * Lets {@code what} go off the blade by the pour service (open-pit.md, Pouring): a short, small pour, as a dozer spills while it
	 * pushes. {@code end} is -1 or 1 off that end of the blade, 0 off its whole width.
	 */
	private void spill(ServerLevel level, PitLoad what, BlockPos start, int end, int window, int setting) {
		Vec3 lip = local(end * (spec().width() / 2 - 0.3), 1.2, spec().length() / 2 + 0.3);
		Vec3 below = end == 0 ? local(0, 1, spec().length() / 2 + 1.0) : new Vec3(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5);
		Vec3 land = new Vec3(below.x, groundUnder(level, below, 24), below.z);
		pour(level, what, new dev.wildcatter.tailings.pit.Pouring.Spec(start, lip, land, window, Math.max(4, setting), end == 0 ? (float) (BLADE_HALF * 2 + 1) : 1.2F));
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
			BlockPos at = BlockPos.containing(local(spillSide * (spec().width() / 2 + 2.0), 0.5, spec().length() / 2 + 1.0));
			if (blade == Blade.CUT) {
				// cutting, the windrow beside the cut stands at once: poured, it would set after the blade has gone by and settle into the
				// fresh cut behind it
				Loose.tip(level, at, one);
			} else {
				spill(level, one, at, spillSide, 4, 6);
			}
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

	/**
	 * SPREAD: a metre at a time, each of the blade's 5 columns fills the ground its tracks stand on, in front of the blade, where that is
	 * open (a hollow, or the lift's own edge over lower ground) from the load, so the dozer lays its load as a 1 m lift and drives on to
	 * it. A real dry stack is spread in lifts and compacted (open-pit-p4-dry-stack.md): a block is the game's thinnest lift.
	 */
	private boolean spread(ServerLevel level) {
		if (load.empty()) {
			return false;
		}
		spreadRun += speed() * TICK;
		dozing = true;
		if (spreadRun >= 1) {
			spreadRun = Math.max(0, spreadRun - 1);
			int lift = (int) Math.floor(getY() + 0.01) - 1;
			Vec3 front = local(0, 0, spec().length() / 2 + 0.5);
			Vec3 across = right();
			for (int side = -BLADE_HALF; side <= BLADE_HALF && !load.empty(); side++) {
				BlockPos pos = new BlockPos((int) Math.floor(front.x + across.x * side), lift, (int) Math.floor(front.z + across.z * side));
				if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos).getFluidState().isEmpty()) {
					// a deeper hollow is filled from its floor up, a block a pass
					BlockPos at = pos;
					for (int down = 0; down < 3 && level.getBlockState(at.below()).canBeReplaced(); down++) {
						at = at.below();
					}
					placeOne(level, at);
				}
			}
		}
		status = "spreading a 1 m lift: " + bladeBlocks + " blocks left";
		if (load.empty()) {
			emptied();
		}
		return true;
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

	// ---- compaction (Tim, 2026-11-23) ---------------------------------------------------------------------------------------------

	/** Moving, its tracks compact the loose cake under it: each cell of dry tails under its box, a pass. */
	@Override
	protected void work(ServerLevel level) {
		if (Math.abs(speed()) <= 0.05) {
			return;
		}
		int y = (int) Math.floor(getY() + 0.01) - 1;
		net.minecraft.world.phys.AABB box = getBoundingBox();
		Vec3 f = ahead(), r = right();
		double hl = spec().length() / 2 - 0.3, hw = spec().width() / 2 - 0.2;
		for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
			for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
				double dx = x + 0.5 - getX(), dz = z + 0.5 - getZ();
				if (Math.abs(dx * f.x + dz * f.z) <= hl && Math.abs(dx * r.x + dz * r.z) <= hw
						&& dev.wildcatter.tailings.block.DryTailsBlock.compact(level, new BlockPos(x, y, z))) {
					compactedBlocks++;
				}
			}
		}
	}

	/** Loose cake its tracks have compacted since it was made. */
	public int compactedBlocks() {
		return compactedBlocks;
	}

	// ---- the autopilot (Tim, 2026-11-23; open-pit-p4-dry-stack.md, "Dozer autopilot") -------------------------------------------

	public boolean autoRunning() {
		return autoOn;
	}

	public int lane() {
		return lane;
	}

	public int lanes() {
		return autoArea == null ? 0 : laneCount();
	}

	public int passes() {
		return passes;
	}

	public Leg leg() {
		return leg;
	}

	public int @Nullable [] autoArea() {
		return autoArea == null ? null : autoArea.clone();
	}

	/**
	 * The survey book's route as its area: two DOZE AREA stakes at opposite corners. Starts the autopilot, pushing the way it faces,
	 * squared; the same area again stops it. Returns what is wrong, or null (its status says what it did).
	 */
	public @Nullable String setAutoArea(ServerLevel level, java.util.List<BlockPos> stakes) {
		if (stakes.size() != 2 || stakes.stream().anyMatch(p -> dev.wildcatter.tailings.block.SurveyStakeBlock.kind(level.getBlockState(p)) != dev.wildcatter.tailings.pit.StopKind.DOZE_AREA)) {
			return "the book needs just the two DOZE AREA stakes at the area's corners";
		}
		BlockPos a = stakes.get(0), b = stakes.get(1);
		int[] area = {Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ())};
		if (autoOn && autoArea != null && java.util.Arrays.equals(area, autoArea)) {
			stopAuto("autopilot stopped");
			return null;
		}
		float yaw = Math.round(net.minecraft.util.Mth.wrapDegrees(getYRot()) / 90F) * 90F;
		autoArea = area;
		pushYaw = net.minecraft.util.Mth.wrapDegrees(yaw);
		double[] line = laneLine(0);
		if (Math.abs(line[3] - line[1]) + Math.abs(line[2] - line[0]) < 2) {
			autoArea = null;
			return "the area is too short to push across: at least " + (int) Math.ceil(spec().length() + 4) + " m the way it faces";
		}
		autoOn = true;
		lane = 0;
		laneStep = 1;
		passes = 0;
		leg = Leg.TO_LANE;
		stuckTicks = 0;
		status = "AUTO: started, " + laneCount() + " lanes, pushing " + net.minecraft.core.Direction.fromYRot(pushYaw).getName();
		return null;
	}

	/** Stops the autopilot where it stands; its area is kept for the book to start it again. */
	public void stopAuto(String why) {
		autoOn = false;
		autoCap = Double.MAX_VALUE;
		autoWaiting = false;
		status = why;
	}

	@Override
	protected void onSurveyBook(ServerLevel level, Player player, net.minecraft.world.item.ItemStack book) {
		String why = setAutoArea(level, dev.wildcatter.tailings.item.SurveyBookItem.route(book));
		say(player, why == null ? status : why);
	}

	@Override
	public boolean selfRunning() {
		return autoOn;
	}

	@Override
	protected void takenOver() {
		if (autoOn) {
			stopAuto("driven by hand: autopilot off");
		}
	}

	@Override
	protected String routeInfo() {
		if (autoArea == null) {
			return "";
		}
		return autoOn ? String.format(java.util.Locale.ROOT, "AUTO: lane %d of %d, pass %d", lane + 1, laneCount(), passes + 1) : "autopilot off";
	}

	private int laneCount() {
		boolean alongZ = Math.abs(Math.cos(Math.toRadians(pushYaw))) > 0.5;
		int across = alongZ ? autoArea[2] + 1 - autoArea[0] : autoArea[3] + 1 - autoArea[1];
		return Math.max(1, (int) Math.ceil(across / (double) (2 * BLADE_HALF + 1)));
	}

	/** Lane k's start and end for its centre, {startX, startZ, endX, endZ}: its back at the near edge, its blade at the far edge. */
	private double[] laneLine(int k) {
		double yaw = Math.toRadians(pushYaw);
		double fx = Math.round(-Math.sin(yaw)), fz = Math.round(Math.cos(yaw));
		boolean alongZ = Math.abs(fz) > 0.5;
		double aLo = alongZ ? autoArea[1] : autoArea[0], aHi = (alongZ ? autoArea[3] : autoArea[2]) + 1;
		double cLo = alongZ ? autoArea[0] : autoArea[1], cHi = (alongZ ? autoArea[2] : autoArea[3]) + 1;
		double s = alongZ ? fz : fx;
		double half = spec().length() / 2, w = 2 * BLADE_HALF + 1;
		// its tail a metre in from the near edge (it may coast a little past a lane's start, reversing to it)
		double near = s > 0 ? aLo + half + 1.0 : aHi - half - 1.0, far = s > 0 ? aHi - half - 0.5 : aLo + half + 0.5;
		double c = cHi - cLo <= w ? (cLo + cHi) / 2 : Math.min(cLo + w / 2 + w * k, cHi - w / 2);
		return alongZ ? new double[] {c, near, c, far} : new double[] {near, c, far, c};
	}

	/** Turns toward {@code want} degrees by up to the pivot rate, on its tracks, if its box has room; true once squared to it. */
	private boolean pivot(ServerLevel level, float want, float most) {
		float err = net.minecraft.util.Mth.wrapDegrees(want - getYRot());
		if (Math.abs(err) < 0.3F) {
			if (err != 0) {
				setYRot(getYRot() + err);
			}
			return true;
		}
		float turned = getYRot() + Math.max(-most, Math.min(most, err));
		if (level.noCollision(this, boxAt(position(), turned).deflate(0.05))) {
			setYRot(turned);
		}
		return false;
	}

	/** Something in its way within 3 m of the blade (forward) or of its tail (reversing): a mob, a player not watching, a machine. */
	private boolean inTheWay(ServerLevel level, boolean forward) {
		Vec3 c = local(0, 0, (forward ? 1 : -1) * (spec().length() / 2 + 1.5));
		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(c.x - 3, getY() - 1, c.z - 3, c.x + 3, getY() + 4, c.z + 3);
		return !level.getEntities(this, box, e -> !hasPassenger(e) && !e.isSpectator()
				&& (e instanceof net.minecraft.world.entity.LivingEntity || e instanceof PitVehicleEntity)).isEmpty();
	}

	@Override
	protected @Nullable Drive autopilot(ServerLevel level) {
		if (!autoOn || autoArea == null) {
			return null;
		}
		if (outOfFuel()) {
			stopAuto("AUTO STOPPED: out of fuel");
			return Drive.STAND;
		}
		double[] line = laneLine(lane);
		double yaw = Math.toRadians(pushYaw);
		double fx = Math.round(-Math.sin(yaw)), fz = Math.round(Math.cos(yaw));
		switch (leg) {
			case BACK -> {
				// straight back down the lane it has just pushed clear, blade raised, to the lane's start
				if (blade != Blade.RAISED) {
					setBlade(Blade.RAISED);
				}
				double[] back = laneLine(backLane);
				double behind = (getX() - back[0]) * fx + (getZ() - back[1]) * fz;
				if (behind < ARRIVED) {
					leg = Leg.TO_LANE;
					autoCap = 0;
					stuckTicks = 0;
					return Drive.STAND;
				}
				pivot(level, pushYaw, 1F);
				if (inTheWay(level, false)) {
					autoWaiting = true;
					status = "AUTO: waiting: something in its way";
					return Drive.STAND;
				}
				if (dropBehind(level)) {
					stopAuto("AUTO STOPPED: a drop behind it");
					return Drive.STAND;
				}
				autoWaiting = false;
				autoCap = Math.max(0.3, Math.min(BACK_SPEED, behind * 0.6));
				if (stuck()) {
					stopAuto("AUTO STOPPED: stuck backing down lane " + (backLane + 1));
					return Drive.STAND;
				}
				status = "AUTO: backing down lane " + (backLane + 1);
				return new Drive(-1, 0, false);
			}
			case TO_LANE -> {
				if (blade != Blade.RAISED) {
					setBlade(Blade.RAISED);
				}
				double dx = line[0] - getX(), dz = line[1] - getZ();
				double dist = Math.hypot(dx, dz);
				if (dist < ARRIVED) {
					leg = Leg.ALIGN;
					autoCap = 0;   // it brakes at the lane's start: a 70 t dozer coasting from a crawl rolls on for metres
					return Drive.STAND;
				}
				// to the lane's start, the rear aimed at it (reversing) or the nose (forward), whichever needs the less turning
				float fwd = (float) Math.toDegrees(Math.atan2(-dx, dz));
				float rev = net.minecraft.util.Mth.wrapDegrees(fwd + 180F);
				boolean back = Math.abs(net.minecraft.util.Mth.wrapDegrees(rev - getYRot())) <= Math.abs(net.minecraft.util.Mth.wrapDegrees(fwd - getYRot()));
				float want = back ? rev : fwd;
				float err = Math.abs(net.minecraft.util.Mth.wrapDegrees(want - getYRot()));
				if (err > 4F && Math.abs(speed()) < 0.3) {
					pivot(level, want, PIVOT);
					status = "AUTO: turning for lane " + (lane + 1);
					return Drive.STAND;
				}
				pivot(level, want, 1F);
				if (inTheWay(level, !back)) {
					autoWaiting = true;
					status = "AUTO: waiting: something in its way";
					return Drive.STAND;
				}
				if (back && dropBehind(level)) {
					stopAuto("AUTO STOPPED: a drop behind it");
					return Drive.STAND;
				}
				autoWaiting = false;
				autoCap = Math.max(0.3, Math.min(BACK_SPEED, dist * 0.6));
				if (stuck()) {
					stopAuto("AUTO STOPPED: stuck on its way to lane " + (lane + 1));
					return Drive.STAND;
				}
				status = "AUTO: over to lane " + (lane + 1);
				return new Drive(back ? -1 : 1, 0, false);
			}
			case ALIGN -> {
				if (Math.abs(speed()) > 0.05) {
					return Drive.STAND;
				}
				if (!pivot(level, pushYaw, PIVOT)) {
					status = "AUTO: squaring up to lane " + (lane + 1);
					return Drive.STAND;
				}
				setBlade(Blade.CARRY);
				setSpread();
				leg = Leg.PUSH;
				autoCap = PitDrive.DOZING_SPEED;
				stuckTicks = 0;
				return Drive.STAND;
			}
			default -> {
				double left = (line[2] - getX()) * fx + (line[3] - getZ()) * fz;
				boolean ended = left <= 0.05 || status.startsWith("hard rock") || status.startsWith("blocked") || status.startsWith("at the crest");
				if (!ended && inTheWay(level, true)) {
					autoWaiting = true;
					status = "AUTO: waiting: something in its way";
					return Drive.STAND;
				}
				autoWaiting = false;
				if (!ended && stuck()) {
					ended = true;
				}
				if (ended) {
					if (bladeBlocks > 0) {
						// what it still carries is left at the area's end, as reversing in CARRY leaves it
						int n = Loose.blocks(load);
						spill(level, load, BlockPos.containing(local(0, 0.5, spec().length() / 2 + 1.0 + Pour.reach(n))), 0, 12, Math.min(40, 3 * n));
						emptied();
					}
					setBlade(Blade.RAISED);
					backLane = lane;
					int lanes = laneCount();
					if (lanes > 1) {
						if (lane + laneStep < 0 || lane + laneStep >= lanes) {
							laneStep = -laneStep;   // across the area and back: each sweep across is a pass
							passes++;
						}
						lane += laneStep;
					} else {
						passes++;
					}
					leg = Leg.BACK;
					stuckTicks = 0;
					autoCap = 0;   // it stops at the lane's end before it reverses
					return Drive.STAND;
				}
				pivot(level, pushYaw, 1F);
				autoCap = left < 1.5 ? 0.4 : PitDrive.DOZING_SPEED;   // it dozes at 3.5 km/h, loaded or not
				return new Drive(1, 0, false);
			}
		}
	}

	/** No headway for 10 s while it means to move. */
	private boolean stuck() {
		stuckTicks = Math.abs(speed()) < 0.05 ? stuckTicks + 1 : 0;
		return stuckTicks > 200;
	}

	/**
	 * Reversing, a crest just behind its tail: a drop of {@link DryStack#CREST_DROP} m or more, as a truck's crest (backing off a pile
	 * or a lift's 1 or 2 m edge is not one).
	 */
	private boolean dropBehind(ServerLevel level) {
		dev.wildcatter.tailings.pit.Crest.Ground ground = HaulTruckEntity.crestGround(level);
		int y = (int) Math.floor(getY() + 0.01);
		Vec3 tail = local(0, 0, -(spec().length() / 2 + 1.0));
		int x = (int) Math.floor(tail.x), z = (int) Math.floor(tail.z);
		for (int d = 1; d <= DryStack.CREST_DROP; d++) {
			if (ground.solid(x, y - d, z)) {
				return false;
			}
		}
		return true;
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
		bermHigh = Math.max(0, Math.min(2, input.getIntOr("berm_high", 1)));
		bladeBlocks = input.getIntOr("blade_blocks", 0);
		setLift(blade.lift());
		int[] area = input.getIntArray("auto_area").orElse(new int[0]);
		autoArea = area.length == 4 ? area : null;
		autoOn = autoArea != null && input.getBooleanOr("auto_on", false);
		pushYaw = input.getFloatOr("push_yaw", 0F);
		lane = input.getIntOr("auto_lane", 0);
		backLane = input.getIntOr("auto_back_lane", lane);
		laneStep = input.getIntOr("auto_step", 1) < 0 ? -1 : 1;
		passes = input.getIntOr("auto_passes", 0);
		try {
			leg = Leg.valueOf(input.getStringOr("auto_leg", "TO_LANE"));
		} catch (IllegalArgumentException ex) {
			leg = Leg.TO_LANE;
		}
		compactedBlocks = input.getIntOr("compacted", 0);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("blade", blade.name());
		output.putString("grade", grade.name());
		output.putInt("berm_high", bermHigh);
		output.putInt("blade_blocks", bladeBlocks);
		if (autoArea != null) {
			output.putIntArray("auto_area", autoArea);
			output.putBoolean("auto_on", autoOn);
			output.putFloat("push_yaw", pushYaw);
			output.putInt("auto_lane", lane);
			output.putInt("auto_back_lane", backLane);
			output.putInt("auto_step", laneStep);
			output.putInt("auto_passes", passes);
			output.putString("auto_leg", leg.name());
		}
		output.putInt("compacted", compactedBlocks);
	}
}
