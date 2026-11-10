package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PitPanelMenu;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The 250 t class hydraulic front shovel (docs/multiblocks/open-pit-p5-big-machines.md, P5c): a 15 m³ bucket of 27 t, a 1,119 kW
 * diesel, digging 14.06 m out and dumping 10.35 m up, on crawlers at 1.5 km/h. One rigid machine: it tracks and swings, drawn with its
 * house swinging, but moves as one box. A pass is 30 s (15 s digging, 15 s swinging, dumping and back; proposed split): 13 passes fill
 * an ultra truck. Its real electric variant draws 860 kW from a trailing cable instead (its panel's Electric).
 */
public class HydraulicShovelEntity extends PitShovelEntity implements PitPanelMenu.Panel {
	public static final int DIG_TICKS = 300;
	public static final int DUMP_TICKS = 300;
	/** The 5,000 L tank (proposed). */
	public static final double TANK_L = 5_000;

	private boolean electric;

	public HydraulicShovelEntity(EntityType<?> type, Level level) {
		super(type, level);
		status = "standing";
	}

	@Override
	public PitDrive.Spec spec() {
		return electric ? PitDrive.HYD_SHOVEL_ELECTRIC : PitDrive.HYD_SHOVEL;
	}

	@Override
	protected Item item() {
		return TailingsItems.HYDRAULIC_SHOVEL;
	}

	@Override
	public double tank() {
		return TANK_L;
	}

	@Override
	public boolean electric() {
		return electric;
	}

	/** Its electric variant on its cable, or its diesel. Public for tests. */
	public void setElectric(boolean on) {
		electric = on;
		syncCable();
		setBoundingBox(makeBoundingBox(position()));
	}

	@Override
	public boolean takesCable() {
		return true;
	}

	@Override
	public double peakWatts() {
		return PitPower.HYD_SHOVEL_ELECTRIC_WATTS;
	}

	// a 15 m bucket face: 8 wide, 12 high from its floor, 3 deep (proposed: its reach)
	@Override
	protected int reachAcross() {
		return 4;
	}

	@Override
	protected int reachUp() {
		return 12;
	}

	@Override
	protected int reachIn() {
		return 3;
	}

	/** The face starts at its front: its bucket reaches 3 m into what stands against it. */
	@Override
	protected double faceAhead() {
		return spec().length() / 2;
	}

	/** It dumps about 10 m out from its centre (its 14 m reach less the bucket's swing in, proposed), to a truck within 6 m of that. */
	@Override
	protected double dumpRadius() {
		return 10.0;
	}

	@Override
	protected double truckReach() {
		return 6.0;
	}

	@Override
	protected double dumpHeight() {
		return 10.35;
	}

	@Override
	protected int digTicks() {
		return DIG_TICKS;
	}

	@Override
	protected int dumpTicks() {
		return DUMP_TICKS;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-2.6, 6.0, -1.0);   // the cab at the house's front left
	}

	// ---- the panel ---------------------------------------------------------------------------------------------------------------

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (!level().isClientSide()) {
				player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PitPanelMenu(id, inventory, this), getType().getDescription()));
			}
			return InteractionResult.SUCCESS;
		}
		return super.interact(player, hand, location);
	}

	@Override
	public PitPanelMenu.Kind kind() {
		return PitPanelMenu.Kind.HYD_SHOVEL;
	}

	@Override
	public int setting(int i) {
		return i == 0 && electric ? 1 : 0;
	}

	@Override
	public void set(int i, int value, Player player) {
		if (i == 0) {
			setElectric(value == 1);
			say(player, value == 1 ? "electric, on its trailing cable" + (cable == null ? ": NO CABLE yet" : "") : "on diesel");
		}
	}

	@Override
	public void action(int a, Player player) {
		if (a == 0 && level() instanceof ServerLevel server) {
			if (selfRunning()) {
				setAuto(server, java.util.List.of());
			} else {
				setAutoHere();
			}
			say(player, status);
		}
	}

	@Override
	public String status() {
		return String.format(Locale.ROOT, "%s. Bucket %s; %s; %s", status, load.tonnes(), routeInfo(), powerInfo());
	}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && player.distanceToSqr(this) < 32 * 32;
	}

	@Override
	public Component readout() {
		return Component.literal("Hydraulic Shovel: " + status());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		electric = input.getBooleanOr("electric", false);
		super.readAdditionalSaveData(input);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("electric", electric);
	}
}
