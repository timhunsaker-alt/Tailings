package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.paste.FillVoid;
import dev.wildcatter.tailings.paste.PasteFill;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The fill outlet (docs/multiblocks/paste-backfill.md, L5): the open end of the fill line inside a void. It reads its void every 5 s
 * ({@link FillVoid}), takes paste from the paste pump on its line while the void is closed and not full, and lets each 200 kg out as a
 * block of wet paste fill in the void's lowest open cell, displacing water: a block stands for {@link PasteFill#FILL_SCALE} cubic metres
 * of the line's paste, the fill's game pace (l5-paste.md #19).
 */
public class FillOutletBlockEntity extends BlockEntity implements Inspectable {
	/** It reads its void every 5 s. */
	public static final int SCAN_TICKS = 100;
	/** Paste held in the line's last metre: 4 t, two cubic metres of the line's paste. */
	public static final double HOLD_KG = 2 * PasteFill.PASTE_KG_PER_M3;

	private double pasteKg;
	private long filled;
	/** The last scan, and how far down its fill list it has got (not saved: read again on load). */
	private FillVoid.@Nullable Result scan;
	private int next;
	private long scanned = Long.MIN_VALUE;

	public FillOutletBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.FILL_OUTLET_ENTITY, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FillOutletBlockEntity outlet) {
		outlet.serverTick();
	}

	private void serverTick() {
		long now = level.getGameTime();
		if (scan == null || now - scanned >= SCAN_TICKS || (pasteKg >= PasteFill.kgPerBlock() - 1e-9 && scan.enclosed() && next >= scan.toFill().length && now - scanned >= 20)) {
			rescan();
		}
		if (pasteKg >= PasteFill.kgPerBlock() - 1e-9 && scan != null && scan.enclosed()) {
			while (next < scan.toFill().length) {
				long cell = scan.toFill()[next++];
				BlockPos at = new BlockPos(FillVoid.x(cell), FillVoid.y(cell), FillVoid.z(cell));
				if (level.isLoaded(at) && open(level.getBlockState(at))) {
					level.setBlock(at, TailingsBlocks.PASTE_FILL.defaultBlockState(), Block.UPDATE_ALL);
					pasteKg = Math.max(0, pasteKg - PasteFill.kgPerBlock());
					filled++;
					setChanged();
					break;
				}
			}
		}
	}

	/** Reads the void again now. */
	public void rescan() {
		scan = FillVoid.scan(this::cell, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		next = 0;
		scanned = level.getGameTime();
	}

	/** Air or water: where paste can go. */
	static boolean open(BlockState state) {
		return state.isAir() || state.is(Blocks.WATER);
	}

	private FillVoid.Cell cell(int x, int y, int z) {
		BlockPos at = new BlockPos(x, y, z);
		if (y < level.getMinY() || y > level.getMaxY()) {
			return FillVoid.Cell.WALL;
		}
		if (!level.isLoaded(at)) {
			return FillVoid.Cell.UNKNOWN;
		}
		BlockState state = level.getBlockState(at);
		if (open(state)) {
			return y >= level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) ? FillVoid.Cell.SKY : FillVoid.Cell.OPEN;
		}
		if (PasteFillBlock.wet(state)) {
			return FillVoid.Cell.WET;
		}
		return PasteFillBlock.set(state) ? FillVoid.Cell.SET : FillVoid.Cell.WALL;
	}

	/** True while it takes paste: its void is closed and not yet full to its level. */
	public boolean accepting() {
		return scan != null && scan.enclosed() && !scan.full();
	}

	/** Takes up to {@code kg} of paste from the pump; returns what it took. */
	public double receive(double kg) {
		if (!accepting()) {
			return 0;
		}
		double took = Math.max(0, Math.min(kg, HOLD_KG - pasteKg));
		pasteKg += took;
		if (took > 0) {
			setChanged();
		}
		return took;
	}

	public FillVoid.@Nullable Result scan() {
		return scan;
	}

	/** Cubic metres it has let out. */
	public long filled() {
		return filled;
	}

	public double pasteKg() {
		return pasteKg;
	}

	/** Its readout: the void, or why it takes nothing. */
	public String status() {
		return scan == null ? "reading its void" : FillVoid.describe(scan);
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(java.util.Locale.ROOT, "Fill Outlet: %s; %d m3 placed", status(), filled));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("paste_kg", pasteKg);
		output.putLong("filled", filled);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		pasteKg = input.getDoubleOr("paste_kg", 0);
		filled = input.getLongOr("filled", 0);
	}
}
