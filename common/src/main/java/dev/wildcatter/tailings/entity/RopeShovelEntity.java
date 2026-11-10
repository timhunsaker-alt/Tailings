package dev.wildcatter.tailings.entity;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.PitPanelMenu;
import dev.wildcatter.tailings.block.RopeShovelBlockEntity;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitPower;
import dev.wildcatter.tailings.pit.RopeShovelLayout;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The 4100-class electric rope shovel, lifted off its parts and running (docs/multiblocks/open-pit-p5-big-machines.md, P5d): a 55 m³
 * dipper of 100 t, on a trailing cable at the world's medium voltage. A 35 s cycle: 12 s hoisting the dipper through the face at
 * 3.5 MW, then 23 s swinging, dumping and back at 0.6 MW (proposed: about 1.5 MW on average). Under 90 % of the nominal volts at its
 * end of the cable it stops, LOW VOLTAGE. Four passes fill an ultra truck, the last a part dipper; one fills a 100 t truck.
 *
 * <p>Its panel: STOP sets its parts back down where it stands (if their cells are free), AUTO loads the trucks that come. It has no
 * diesel and no fuel tank; it is nothing without its cable.
 */
public class RopeShovelEntity extends PitShovelEntity implements PitPanelMenu.Panel {
	/** The hoist part of its cycle, 12 s; swinging, dumping and back, 23 s (open-pit.md's 35 s cycle). */
	public static final int DIG_TICKS = 240;
	public static final int DUMP_TICKS = 460;

	public RopeShovelEntity(EntityType<?> type, Level level) {
		super(type, level);
		status = "standing";
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.ROPE_SHOVEL;
	}

	/** It is not placed from an item: it is built from its parts. Broken by hand, it gives back its crawlers. */
	@Override
	protected Item item() {
		return TailingsBlocks.SHOVEL_CRAWLERS.asItem();
	}

	@Override
	public double tank() {
		return 1;
	}

	@Override
	public boolean electric() {
		return true;
	}

	@Override
	public boolean takesCable() {
		return true;
	}

	@Override
	public double peakWatts() {
		return PitPower.ROPE_HOIST_WATTS;
	}

	/** It draws by its cycle: the hoist's 3.5 MW while it digs, 0.6 MW the rest of the time it works (P5-5, proposed). */
	@Override
	protected double electricWatts(double engineWatts) {
		return digging() ? PitPower.ROPE_HOIST_WATTS : PitPower.ROPE_REST_WATTS;
	}

	/** Lifted from its parts: freshly, with its cable on. */
	public void lifted() {
		status = "lifted off its parts: " + (cable == null ? "NO CABLE" : cable.describe());
	}

	// a 55 m³ dipper's face: 10 wide, 15 high from its floor, 4 deep (proposed: its reach)
	@Override
	protected int reachAcross() {
		return 5;
	}

	@Override
	protected int reachUp() {
		return 15;
	}

	@Override
	protected int reachIn() {
		return 4;
	}

	@Override
	protected double faceAhead() {
		return spec().length() / 2;
	}

	/** It dumps about 18 m out from its centre (its boom point, proposed), into a truck within 6 m of that arc. */
	@Override
	protected double dumpRadius() {
		return 18.0;
	}

	@Override
	protected double truckReach() {
		return 6.0;
	}

	@Override
	protected double dumpHeight() {
		return 9.5;
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
	protected double autoReach() {
		return 40;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-4.5, 8.5, 6.5);   // the cab at the house's front left corner
	}

	/** On the client: where its dipper is, 0 down in the face to 1 up over a truck, for drawing the hoist ropes. */
	public float hoist() {
		return lift();
	}

	// ---- setting down -------------------------------------------------------------------------------------------------------------

	/** STOP: its parts go back down as blocks where it stands, square to the nearest way, if their cells are free; why not, or null. */
	public String stopAndSetDown(ServerLevel level) {
		int turns = RopeShovelLayout.turns(getYRot());
		// the crawlers' main cell, from its centre
		double qx = RopeShovelLayout.CENTRE_X - 0.5, qz = RopeShovelLayout.CENTRE_Z - 0.5;
		for (int t = 0; t < turns; t++) {
			double tmp = qx;
			qx = -qz;
			qz = tmp;
		}
		BlockPos main = new BlockPos((int) Math.round(getX() - 0.5 - qx), (int) Math.floor(getY() + 0.01), (int) Math.round(getZ() - 0.5 - qz));
		CableLink link = cable;
		String why = RopeShovelBlockEntity.setDown(level, main, turns, getUUID(), link);
		if (why != null) {
			say(getFirstPassenger() instanceof Player p ? p : null, why);
			return why;
		}
		cable = null;
		ejectPassengers();
		discard();
		return "set down as its parts";
	}

	/** Broken by hand it drops every part. */
	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
				if (part != RopeShovelLayout.Part.CRAWLERS) {
					spawnAtLocation(level, new ItemStack(TailingsBlocks.plantBlock(part.machine())));
				}
			}
		}
		return super.hurtServer(level, source, amount);
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
		return PitPanelMenu.Kind.ROPE_SHOVEL;
	}

	@Override
	public int setting(int i) {
		return 0;
	}

	@Override
	public void set(int i, int value, Player player) {
	}

	@Override
	public void action(int a, Player player) {
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		switch (a) {
			case 0 -> say(player, "running: " + status);
			case 1 -> say(player, stopAndSetDown(server));
			case 2 -> {
				if (selfRunning()) {
					setAuto(server, java.util.List.of());
				} else {
					setAutoHere();
				}
				say(player, status);
			}
			default -> {
			}
		}
	}

	@Override
	public String status() {
		return String.format(Locale.ROOT, "%s. Dipper %s; %s; %s", status, load.tonnes(), routeInfo(), powerInfo());
	}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && player.distanceToSqr(this) < 40 * 40;
	}

	@Override
	public Component readout() {
		return Component.literal("Rope Shovel: " + status());
	}
}
