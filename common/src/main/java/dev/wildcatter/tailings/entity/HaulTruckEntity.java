package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.plant.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 100 t class haul truck (docs/multiblocks/open-pit.md, Haul trucks): 91 t in its body, counted in kilograms. Standing, jump
 * raises the body; at the top the load goes into a pit hopper under its tail, or out behind it onto the ground by the settle rule,
 * and the body comes down again.
 */
public class HaulTruckEntity extends PitVehicleEntity {
	/** The body goes up, or down, in 13 s (proposed). */
	public static final int RAISE_TICKS = 260;

	/** 1 raising, -1 lowering, 0 still. */
	private int raising;
	/** Its surveyed route when it drives itself (open-pit.md, P2a). */
	private final TruckRoute route = new TruckRoute();
	private long[] savedRoute;
	private int savedAt, savedWay;
	private String savedPhase = "DRIVE";

	public HaulTruckEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.TRUCK;
	}

	@Override
	protected Item item() {
		return TailingsItems.HAUL_TRUCK;
	}

	@Override
	public double tank() {
		return dev.wildcatter.tailings.pit.PitFuel.TRUCK_TANK_L;
	}

	@Override
	protected double workFraction() {
		return raising != 0 ? dev.wildcatter.tailings.pit.PitFuel.HOIST : 0;
	}

	@Override
	public boolean selfRunning() {
		return route.active();
	}

	/** Its route's stakes, for tests. */
	public java.util.List<BlockPos> routeStakes() {
		return java.util.List.copyOf(route.stakes);
	}

	public boolean blocked() {
		return route.blocked();
	}

	@Override
	protected @Nullable Drive autopilot(ServerLevel level) {
		if (savedRoute != null) {
			route.load(level, this, savedRoute, savedAt, savedWay, savedPhase);
			savedRoute = null;
		}
		return route.active() ? route.drive(level, this) : null;
	}

	/** Gives it the survey book's route; why not, or null. Public for tests. */
	public @Nullable String takeRoute(ServerLevel level, java.util.List<BlockPos> stakes) {
		String why = route.set(level, this, stakes);
		if (why == null) {
			setStatus("given a route of " + stakes.size() + " stakes");
		}
		return why;
	}

	@Override
	protected void onSurveyBook(ServerLevel level, Player player, net.minecraft.world.item.ItemStack book) {
		java.util.List<BlockPos> stakes = dev.wildcatter.tailings.item.SurveyBookItem.route(book);
		if (stakes.isEmpty()) {
			route.clear();
			say(player, "off its route: drive it by hand");
			return;
		}
		String why = takeRoute(level, stakes);
		say(player, why == null ? "driving itself on a route of " + stakes.size() + " stakes" : why);
	}

	@Override
	protected void takenOver() {
		route.clear();
		status = "driven by hand";
	}

	@Override
	protected String routeInfo() {
		return level() instanceof ServerLevel server ? route.describe(server) : "";
	}

	@Override
	public boolean busy() {
		return raising != 0 || lift() > 0;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-1.9, 3.1, 3.4);   // the cab, on the deck over the left front wheel
	}

	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		if (raising != 0) {
			return;
		}
		if (Math.abs(speed()) > 0.1) {
			say(driver, "stop to tip");
			return;
		}
		if (load.empty()) {
			say(driver, "empty");
			return;
		}
		raising = 1;
		status = "tipping";
	}

	private void say(@Nullable Player driver, String what) {
		status = what;
		if (driver != null) {
			driver.sendOverlayMessage(Component.literal("Haul Truck: " + what));
		}
	}

	@Override
	protected void work(ServerLevel level) {
		if (raising == 0) {
			return;
		}
		float lift = Math.max(0, Math.min(1, lift() + raising / (float) RAISE_TICKS));
		setLift(lift);
		if (raising > 0 && lift >= 1) {
			tip(level);
			raising = -1;
		} else if (raising < 0 && lift <= 0) {
			raising = 0;
		}
	}

	/** Where its load goes as the body comes up: the hopper under its tail, or the ground behind it. Public for tests. */
	public void tip(ServerLevel level) {
		Player driver = getFirstPassenger() instanceof Player p ? p : null;
		DumpBlockEntity hopper = hopperBehind(level);
		if (hopper != null) {
			String refused = hopper.tip(load);
			refreshLoad();
			say(driver, refused == null ? "tipped into the hopper" : "can't tip: " + refused);
		} else {
			Vec3 tail = local(0, 1, -spec().length() / 2 - 1.5);
			int placed = Loose.tip(level, BlockPos.containing(tail), load);
			refreshLoad();
			say(driver, "tipped " + placed + " blocks" + (load.empty() ? "" : ", some left in the body"));
		}
		level.playSound(null, getX(), getY(), getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 3F, 0.8F);
	}

	/** A pit hopper under the body's tail: up to 3 m behind it, across its width, down to 8 m. */
	public @Nullable DumpBlockEntity hopperBehind(Level level) {
		for (double back = 0; back <= 3; back += 1) {
			for (double across = -2; across <= 2; across += 1) {
				Vec3 at = local(across, 0, -spec().length() / 2 - back);
				BlockPos column = BlockPos.containing(at);
				for (int down = 0; down <= 8; down++) {
					BlockPos pos = column.below(down);
					BlockState state = level.getBlockState(pos);
					if (state.getBlock() instanceof PlantMachineBlock block && block.machine() == Machine.PIT_HOPPER
							&& level.getBlockEntity(PlantMachineBlock.mainOf(pos, state)) instanceof DumpBlockEntity hopper) {
						return hopper;
					}
				}
			}
		}
		return null;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		raising = input.getIntOr("raising", 0);
		String saved = input.getStringOr("route", "");
		long[] stakes = saved.isEmpty() ? new long[0] : java.util.Arrays.stream(saved.split(",")).mapToLong(Long::parseLong).toArray();
		if (stakes.length > 0) {
			savedRoute = stakes;   // set again on its first tick, when the level can survey it
			savedAt = input.getIntOr("route_at", 0);
			savedWay = input.getIntOr("route_way", 1);
			savedPhase = input.getStringOr("route_phase", "DRIVE");
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("raising", raising);
		if (route.active()) {
			output.putString("route", java.util.Arrays.stream(route.save()).mapToObj(Long::toString).collect(java.util.stream.Collectors.joining(",")));
			output.putInt("route_at", route.at);
			output.putInt("route_way", route.way);
			output.putString("route_phase", route.phase.name());
		}
	}
}
