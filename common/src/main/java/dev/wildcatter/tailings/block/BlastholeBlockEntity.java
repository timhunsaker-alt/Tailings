package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.pit.Blasting;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * One block of a blasthole: the rock it replaced, and on the collar the whole hole (docs/multiblocks/open-pit-p3-blasting.md, The
 * hole's states): depth, the pattern it belongs to, its cuttings, its booster and detonator, its charge, stemming and delay.
 */
public class BlastholeBlockEntity extends BlockEntity implements Inspectable {
	/** What this block was. */
	private String replaced = "minecraft:stone";
	/** The hole's collar: this block, or the one this column hangs from. */
	private @Nullable BlockPos collar;

	// ---- the collar's -----------------------------------------------------------------------------------------------------------
	private int depth;
	private boolean shortHole;
	private String toe = "";
	private int row, col;
	/** The way to the pattern's free face (the drill's back), or null for a hole drilled alone. */
	private @Nullable Direction face;
	private int burden = Blasting.BURDEN, spacing = Blasting.SPACING, subdrill = Blasting.SUBDRILL;
	private double cuttings;
	private boolean booster;
	private int detonator;
	private double chargeKg;
	private long chargedTick;
	private double stemming;
	private boolean logged;
	private int delayMs;
	private boolean misfired;
	private String misfireWhy = "";

	public BlastholeBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.BLASTHOLE_ENTITY, pos, state);
	}

	// ---- drilling ----------------------------------------------------------------------------------------------------------------

	/** Set as the drill bores it. */
	public void drilled(String replaced, BlockPos collar) {
		this.replaced = replaced;
		this.collar = collar.immutable();
		setChanged();
	}

	/** The collar's pattern, set when the drill starts the hole. */
	public void pattern(int row, int col, @Nullable Direction face, int burden, int spacing, int subdrill) {
		this.row = row;
		this.col = col;
		this.face = face;
		this.burden = burden;
		this.spacing = spacing;
		this.subdrill = subdrill;
		setChanged();
	}

	/** The collar, when the drill has finished the hole: its depth, whether it stopped short, the rock at its toe. */
	public void finish(int depth, boolean shortHole, String toe) {
		this.depth = depth;
		this.shortHole = shortHole;
		this.toe = toe;
		this.cuttings = Blasting.cuttingsStemming(depth);
		refresh();
	}

	public String replaced() {
		return replaced;
	}

	public BlockState replacedState() {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(replaced));
		return block == null || block == Blocks.AIR ? Blocks.STONE.defaultBlockState() : block.defaultBlockState();
	}

	public boolean isCollar() {
		return collar == null || collar.equals(worldPosition);
	}

	public BlockPos collar() {
		return collar == null ? worldPosition : collar;
	}

	public @Nullable BlastholeBlockEntity collarEntity() {
		return level != null && level.getBlockEntity(collar()) instanceof BlastholeBlockEntity hole ? hole : null;
	}

	// ---- reading the collar ------------------------------------------------------------------------------------------------------

	public int depth() {
		return depth;
	}

	public boolean shortHole() {
		return shortHole;
	}

	public int row() {
		return row;
	}

	public int col() {
		return col;
	}

	public @Nullable Direction face() {
		return face;
	}

	public int burden() {
		return burden;
	}

	public int spacing() {
		return spacing;
	}

	public int subdrill() {
		return subdrill;
	}

	/** Collar to bench floor: the hole less its subdrill. */
	public int bench() {
		return Math.max(1, depth - subdrill);
	}

	public boolean booster() {
		return booster;
	}

	public int detonator() {
		return detonator;
	}

	public boolean primed() {
		return booster && detonator != 0;
	}

	public double chargeKg() {
		return chargeKg;
	}

	public long chargedTick() {
		return chargedTick;
	}

	public double column() {
		return chargeKg / Blasting.KG_PER_M;
	}

	public double stemming() {
		return stemming;
	}

	public boolean logged() {
		return logged;
	}

	public int delayMs() {
		return delayMs;
	}

	public boolean misfired() {
		return misfired;
	}

	public double cuttings() {
		return cuttings;
	}

	public double boxTonnes() {
		return Blasting.boxTonnes(burden, spacing, bench());
	}

	public BlastholeBlock.Stage stage() {
		if (misfired) {
			return BlastholeBlock.Stage.MISFIRED;
		}
		if (logged) {
			return BlastholeBlock.Stage.LOGGED;
		}
		if (stemming > 0) {
			return BlastholeBlock.Stage.STEMMED;
		}
		if (chargeKg > 0) {
			return BlastholeBlock.Stage.CHARGED;
		}
		return primed() ? BlastholeBlock.Stage.PRIMED : BlastholeBlock.Stage.DRILLED;
	}

	// ---- loading the hole --------------------------------------------------------------------------------------------------------

	/** What a click did: whether it used the item, and what to tell the player. */
	public record Said(boolean took, String text) {
	}

	public Said addBooster() {
		if (chargeKg > 0 && !misfired) {
			return new Said(false, "the primer goes in first: wash the charge out to prime it");
		}
		if (booster && !misfired) {
			return new Said(false, "it has its booster");
		}
		booster = true;
		reprimed();
		refresh();
		return new Said(true, primed() ? "primed: booster, detonator #" + detonator : "booster in: it needs a detonator");
	}

	public Said addDetonator(int id) {
		if (chargeKg > 0 && !misfired) {
			return new Said(false, "the primer goes in first: wash the charge out to prime it");
		}
		if (detonator != 0 && !misfired) {
			return new Said(false, "it has its detonator #" + detonator);
		}
		detonator = id;
		reprimed();
		refresh();
		return new Said(true, primed() ? "primed: booster, detonator #" + detonator : "detonator #" + detonator + " in: it needs a booster");
	}

	/** A misfire primed again is fired alone (spec, Misfires): it is no longer marked misfired once it has both. */
	private void reprimed() {
		if (misfired && primed()) {
			misfired = false;
			misfireWhy = "";
		}
	}

	/** The truck pumps {@code kg} in. */
	public void charge(double kg, long tick) {
		chargeKg += kg;
		chargedTick = tick;
		refresh();
	}

	/** What is left above the charge to stem. */
	public double unstemmed() {
		return Math.max(0, depth - column() - stemming);
	}

	public Said stemWithCuttings() {
		if (chargeKg <= 0) {
			return new Said(false, readout());
		}
		double room = unstemmed();
		if (room <= 0.01) {
			return new Said(false, String.format(Locale.ROOT, "stemmed to the collar: %.1f m", stemming));
		}
		if (cuttings <= 0.01) {
			return new Said(false, "no cuttings left: stem it with gravel");
		}
		double put = Math.min(room, cuttings);
		cuttings -= put;
		stemming += put;
		refresh();
		return new Said(false, String.format(Locale.ROOT, "stemmed with its cuttings: %.1f m", stemming));
	}

	public Said stemWithGravel() {
		if (chargeKg <= 0) {
			return new Said(false, "charge it before stemming");
		}
		double room = unstemmed();
		if (room <= 0.01) {
			return new Said(false, String.format(Locale.ROOT, "stemmed to the collar: %.1f m", stemming));
		}
		stemming += Math.min(room, Blasting.gravelStemming());
		refresh();
		return new Said(true, String.format(Locale.ROOT, "stemmed with gravel: %.1f m", stemming));
	}

	/** Stem it {@code metres} (tests and the logger's check). */
	public void stem(double metres) {
		stemming = Math.max(0, Math.min(Math.max(0, depth - column()), metres));
		refresh();
	}

	/** A water bucket washes the charge out; the booster and detonator come back. Not once stemmed, unless it misfired. */
	public Said washOut(Level level, @Nullable Player player) {
		if (stemming > 0 && !misfired) {
			return new Said(false, "stemmed: it cannot be washed out");
		}
		if (chargeKg <= 0 && !primed() && !booster && detonator == 0) {
			return new Said(false, "nothing in it to wash out");
		}
		if (player != null && !level.isClientSide()) {
			if (booster) {
				give(player, new ItemStack(TailingsItems.CAST_BOOSTER));
			}
			if (detonator != 0) {
				give(player, new ItemStack(TailingsItems.DETONATOR));
			}
		}
		chargeKg = 0;
		booster = false;
		detonator = 0;
		stemming = 0;
		logged = false;
		misfired = false;
		misfireWhy = "";
		refresh();
		return new Said(true, "washed out: the booster and detonator come back");
	}

	private static void give(Player player, ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	public void log(int delayMs) {
		this.logged = true;
		this.delayMs = delayMs;
		refresh();
	}

	public void misfire(String why) {
		misfired = true;
		misfireWhy = why;
		logged = false;
		refresh();
	}

	/** Its tag follows its state. */
	private void refresh() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			BlockState state = getBlockState();
			if (state.getBlock() instanceof BlastholeBlock && state.getValue(BlastholeBlock.STAGE) != stage()) {
				level.setBlock(worldPosition, state.setValue(BlastholeBlock.STAGE, stage()), Block.UPDATE_ALL);
			}
		}
	}

	// ---- reading -----------------------------------------------------------------------------------------------------------------

	public String readout() {
		StringBuilder out = new StringBuilder(stage().name());
		out.append(String.format(Locale.ROOT, ": %d m%s, toe in %s", depth, shortHole ? " SHORT" : "", toe.substring(toe.indexOf(':') + 1)));
		if (booster || detonator != 0) {
			out.append(primed() ? "; primed: booster, detonator #" + detonator : booster ? "; booster, no detonator" : "; detonator #" + detonator + ", no booster");
		}
		if (chargeKg > 0) {
			out.append(String.format(Locale.ROOT, "; %.0f kg, %.1f m column", chargeKg, column()));
			if (level != null && level.getGameTime() - chargedTick < Blasting.SLEEP_TICKS) {
				out.append(" (sleeping)");
			}
			if (!primed()) {
				out.append("; no primer: it will not fire");
			}
		}
		if (stemming > 0) {
			out.append(String.format(Locale.ROOT, "; stemmed %.1f m", stemming));
		}
		if (logged) {
			out.append("; logged ").append(delayMs).append(" ms");
		}
		if (misfired) {
			out.append("; MISFIRED: ").append(misfireWhy);
		}
		out.append(String.format(Locale.ROOT, "; cuttings for %.1f m", cuttings));
		return out.toString();
	}

	@Override
	public Component inspect() {
		BlastholeBlockEntity hole = collarEntity();
		return Component.literal("Blasthole: " + (hole == null ? "a hole's column" : hole.readout()));
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("replaced", replaced);
		if (collar != null) {
			output.putLong("collar", collar.asLong());
		}
		if (!isCollar()) {
			return;
		}
		output.putInt("depth", depth);
		output.putBoolean("short", shortHole);
		output.putString("toe", toe);
		output.putInt("row", row);
		output.putInt("col", col);
		output.putInt("face", face == null ? -1 : face.get3DDataValue());
		output.putInt("burden", burden);
		output.putInt("spacing", spacing);
		output.putInt("subdrill", subdrill);
		output.putDouble("cuttings", cuttings);
		output.putBoolean("booster", booster);
		output.putInt("detonator", detonator);
		output.putDouble("charge", chargeKg);
		output.putLong("charged_tick", chargedTick);
		output.putDouble("stemming", stemming);
		output.putBoolean("logged", logged);
		output.putInt("delay", delayMs);
		output.putBoolean("misfired", misfired);
		output.putString("misfire_why", misfireWhy);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		replaced = input.getStringOr("replaced", "minecraft:stone");
		long c = input.getLongOr("collar", Long.MIN_VALUE);
		collar = c == Long.MIN_VALUE ? null : BlockPos.of(c);
		depth = input.getIntOr("depth", 0);
		shortHole = input.getBooleanOr("short", false);
		toe = input.getStringOr("toe", "");
		row = input.getIntOr("row", 0);
		col = input.getIntOr("col", 0);
		int f = input.getIntOr("face", -1);
		face = f < 0 ? null : Direction.from3DDataValue(f);
		burden = input.getIntOr("burden", Blasting.BURDEN);
		spacing = input.getIntOr("spacing", Blasting.SPACING);
		subdrill = input.getIntOr("subdrill", Blasting.SUBDRILL);
		cuttings = input.getDoubleOr("cuttings", 0);
		booster = input.getBooleanOr("booster", false);
		detonator = input.getIntOr("detonator", 0);
		chargeKg = input.getDoubleOr("charge", 0);
		chargedTick = input.getLongOr("charged_tick", 0);
		stemming = input.getDoubleOr("stemming", 0);
		logged = input.getBooleanOr("logged", false);
		delayMs = input.getIntOr("delay", 0);
		misfired = input.getBooleanOr("misfired", false);
		misfireWhy = input.getStringOr("misfire_why", "");
	}
}
