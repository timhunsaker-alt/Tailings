package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlock;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.PitPanelMenu;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.PitDrive;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The bulk emulsion truck (docs/multiblocks/open-pit-p3-blasting.md): 12 t of emulsion matrix, filled at a matrix fill point, pumped
 * down the hole at 20 kg/s through a 12 m hose off the reel at its back, and gassed there. Driven by a player like the haul truck.
 * Jump, standing, charges every primed, uncharged hole in the hose's reach, nearest first, to the hole's depth less the stemming set on
 * its panel (sneak-right-click). A hole not primed is skipped and counted.
 */
public class EmulsionTruckEntity extends PitVehicleEntity implements PitPanelMenu.Panel {
	private double matrixKg;
	private int stemming = Blasting.STEMMING;
	private final Deque<BlockPos> queue = new ArrayDeque<>();
	private @Nullable BlockPos charging;
	private double target;
	private int pumpRate = 1;

	public EmulsionTruckEntity(EntityType<?> type, Level level) {
		super(type, level);
		status = "empty: fill it at a matrix fill point";
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.EMULSION_TRUCK;
	}

	@Override
	protected Item item() {
		return TailingsItems.EMULSION_TRUCK;
	}

	@Override
	public double tank() {
		return Blasting.TRUCK_TANK_L;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-0.6, 2.2, -3.0);
	}

	@Override
	public boolean busy() {
		return charging != null;
	}

	@Override
	protected double workFraction() {
		return charging != null ? Blasting.PUMPING : 0;
	}

	public double matrixKg() {
		return matrixKg;
	}

	/** What the fill point gives it; returns what it took. */
	public double fill(double kg) {
		double took = Math.max(0, Math.min(kg, Blasting.TRUCK_MATRIX_KG - matrixKg));
		matrixKg += took;
		return took;
	}

	/** Test hooks. */
	public void setMatrix(double kg) {
		matrixKg = Math.max(0, Math.min(Blasting.TRUCK_MATRIX_KG, kg));
	}

	public void fastPump(int times) {
		pumpRate = Math.max(1, times);
	}

	public void setStemming(int metres) {
		stemming = PitPanelMenu.Kind.TRUCK.settings()[0].clamp(metres);
	}

	/** The hose reel at its back. */
	public Vec3 reel() {
		return local(0, 0, -spec().length() / 2);
	}

	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		chargeAll(level);
	}

	/** Charges every primed, uncharged hole in reach, nearest first. Public for tests. */
	public String chargeAll(ServerLevel level) {
		if (charging != null) {
			return status;
		}
		if (Math.abs(speed()) > 0.05) {
			return say("stop to charge");
		}
		if (matrixKg < 1) {
			return say("EMPTY: fill it at a matrix fill point");
		}
		Vec3 reel = reel();
		List<BlockPos> holes = new ArrayList<>();
		int notPrimed = 0;
		int r = (int) Math.ceil(Blasting.HOSE_REACH);
		BlockPos centre = BlockPos.containing(reel);
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -4, -r), centre.offset(r, 3, r))) {
			if (!(level.getBlockState(pos).getBlock() instanceof BlastholeBlock) || !level.getBlockState(pos).getValue(BlastholeBlock.COLLAR)) {
				continue;
			}
			if (Math.hypot(pos.getX() + 0.5 - reel.x, pos.getZ() + 0.5 - reel.z) > Blasting.HOSE_REACH) {
				continue;
			}
			if (level.getBlockEntity(pos) instanceof BlastholeBlockEntity hole && hole.chargeKg() <= 0 && !hole.misfired()) {
				if (hole.primed()) {
					holes.add(pos.immutable());
				} else {
					notPrimed++;
				}
			}
		}
		if (holes.isEmpty()) {
			return say(notPrimed > 0 ? "NOT PRIMED (" + notPrimed + ")" : "NO HOLES IN REACH");
		}
		holes.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(reel.x, p.getY() + 0.5, reel.z)));
		queue.clear();
		queue.addAll(holes);
		startNext(level);
		return say(String.format(Locale.ROOT, "charging %d holes%s", holes.size(), notPrimed > 0 ? "; NOT PRIMED (" + notPrimed + ") skipped" : ""));
	}

	private void startNext(ServerLevel level) {
		charging = null;
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			if (level.getBlockEntity(pos) instanceof BlastholeBlockEntity hole && hole.primed() && hole.chargeKg() <= 0) {
				charging = pos;
				target = Blasting.chargeKg(Blasting.column(hole.depth(), stemming));
				return;
			}
		}
	}

	@Override
	protected void work(ServerLevel level) {
		setLift(charging == null ? 0 : 1);   // the hose off its reel
		if (charging == null) {
			return;
		}
		if (outOfFuel()) {
			say("out of fuel");
			return;
		}
		if (!(level.getBlockEntity(charging) instanceof BlastholeBlockEntity hole)) {
			startNext(level);
			return;
		}
		if (matrixKg <= 0) {
			queue.clear();
			charging = null;
			say("EMPTY: fill it at a matrix fill point");
			return;
		}
		double give = Math.min(Blasting.PUMP_KG_PER_TICK * pumpRate, Math.min(matrixKg, target - hole.chargeKg()));
		hole.charge(give, level.getGameTime());
		matrixKg -= give;
		status = String.format(Locale.ROOT, "charging %s: %.0f of %.0f kg; %.0f kg of matrix left", charging.toShortString(), hole.chargeKg(), target, matrixKg);
		if (hole.chargeKg() >= target - 1e-6) {
			startNext(level);
			if (charging == null) {
				say(String.format(Locale.ROOT, "charged; %.0f kg of matrix left", matrixKg));
			}
		}
	}

	private String say(String what) {
		status = what;
		if (getFirstPassenger() instanceof Player driver) {
			driver.sendOverlayMessage(Component.literal("Emulsion Truck: " + what));
		}
		return what;
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
		return PitPanelMenu.Kind.TRUCK;
	}

	@Override
	public int setting(int i) {
		return stemming;
	}

	@Override
	public void set(int i, int value, Player player) {
		stemming = value;
	}

	@Override
	public void action(int a, Player player) {
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		if (a == 0) {
			chargeAll(server);
		} else {
			queue.clear();
			charging = null;
			say("stopped");
		}
	}

	@Override
	public String status() {
		return String.format(Locale.ROOT, "%s. Matrix %.0f of %.0f kg; column = depth less %d m; fuel %.0f of %.0f L", status, matrixKg, Blasting.TRUCK_MATRIX_KG,
				stemming, fuel(), tank());
	}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && player.distanceToSqr(this) < 24 * 24;
	}

	@Override
	public Component readout() {
		return Component.literal("Emulsion Truck: " + status());
	}

	/** While pumping, the pump's motor (Wildcatter's motor hum stands in for it). */
	@Override
	public net.minecraft.sounds.@Nullable SoundEvent loop() {
		return lift() > 0 ? dev.wildcatter.WildcatterSounds.MOTOR_HUM : super.loop();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		matrixKg = input.getDoubleOr("matrix", 0);
		stemming = PitPanelMenu.Kind.TRUCK.settings()[0].clamp(input.getIntOr("stemming", Blasting.STEMMING));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putDouble("matrix", matrixKg);
		output.putInt("stemming", stemming);
	}
}
