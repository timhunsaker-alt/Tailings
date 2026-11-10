package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.pit.PitMass;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a block of muck is (docs/multiblocks/open-pit.md): broken rock, counted in quarters of a bank block of each rock or ore it
 * was. A full muck block holds three quarters, since 3 blocks of rock break to 4 of muck.
 */
public class MuckBlockEntity extends BlockEntity implements Inspectable {
	private final Map<String, Integer> quarters = new LinkedHashMap<>();

	public MuckBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.MUCK_ENTITY, pos, state);
	}

	public Map<String, Integer> quarters() {
		return Collections.unmodifiableMap(quarters);
	}

	public int held() {
		int n = 0;
		for (int q : quarters.values()) {
			n += q;
		}
		return n;
	}

	public void set(Map<String, Integer> contents) {
		quarters.clear();
		contents.forEach((id, q) -> {
			if (q > 0) {
				quarters.put(id, q);
			}
		});
		setChanged();
	}

	/** Its contents in kilograms, item by item. */
	public Map<String, Long> kilograms() {
		Map<String, Long> out = new LinkedHashMap<>();
		quarters.forEach((id, q) -> out.put(id, (long) q * PitMass.kg(id) / 4));
		return out;
	}

	@Override
	public Component inspect() {
		StringBuilder what = new StringBuilder();
		quarters.forEach((id, q) -> what.append(what.isEmpty() ? "" : ", ").append(q).append("/4 ").append(id.substring(id.indexOf(':') + 1)));
		if (getBlockState().getBlock() instanceof StockpileRockBlock) {
			return Component.literal("Stockpile: crushed rock, " + (what.isEmpty() ? "empty" : what) + " of a bank block");
		}
		return Component.literal("Muck: broken rock, " + (what.isEmpty() ? "empty" : what) + " of a bank block");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		StringBuilder out = new StringBuilder();
		quarters.forEach((id, q) -> out.append(out.isEmpty() ? "" : ",").append(id).append('=').append(q));
		output.putString("quarters", out.toString());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		quarters.clear();
		for (String part : input.getStringOr("quarters", "").split(",")) {
			int eq = part.lastIndexOf('=');
			if (eq > 0) {
				try {
					quarters.put(part.substring(0, eq), Integer.parseInt(part.substring(eq + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
	}
}
