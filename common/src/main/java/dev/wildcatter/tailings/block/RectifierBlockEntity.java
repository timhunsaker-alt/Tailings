package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.Haul;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 480 V in, 250 V DC out to the mine rail. The DC side stays inside this block. A rail is fed only while this motor
 * is running and the rail is connected, including a one-block step down the decline.
 */
public class RectifierBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, Haul.RECTIFIER_WATTS);
	private final Set<BlockPos> fed = new HashSet<>();
	private boolean demand = true;
	private boolean forced;

	public RectifierBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.RECTIFIER, pos, state);
	}

	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, RectifierBlockEntity rectifier) {
		if (level.getGameTime() % 20 == 0) {
			rectifier.spread();
		}
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public void forceRunning() {
		forced = true;
	}

	public int fedCount() {
		return fed.size();
	}

	public void spread() {
		if (level == null) {
			return;
		}
		Set<BlockPos> found = new HashSet<>();
		if (powered()) {
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			for (Direction direction : Direction.values()) {
				consider(worldPosition.relative(direction), queue, found);
			}
			while (!queue.isEmpty() && found.size() < 128) {
				BlockPos pos = queue.removeFirst();
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dy == 0 && dz == 0) {
								continue;
							}
							consider(pos.offset(dx, dy, dz), queue, found);
						}
					}
				}
			}
		}
		for (BlockPos old : fed) {
			if (!found.contains(old) && level.getBlockEntity(old) instanceof RailBlockEntity rail) {
				rail.setPowered(false);
			}
		}
		fed.clear();
		for (BlockPos pos : found) {
			if (level.getBlockEntity(pos) instanceof RailBlockEntity rail) {
				rail.setPowered(true);
				fed.add(pos);
			}
		}
		setChanged();
	}

	private void consider(BlockPos pos, ArrayDeque<BlockPos> queue, Set<BlockPos> found) {
		if (level.getBlockState(pos).getBlock() == TailingsBlocks.MINE_RAIL && found.add(pos)) {
			queue.add(pos);
		}
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, demand);
	}

	@Override
	public Component inspect() {
		return Component.literal("Traction rectifier (300 kW, 250 V DC inside): " + fed.size() + " rails; " + motor.describe());
	}
}
