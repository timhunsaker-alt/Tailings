package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.VentNetwork;
import dev.wildcatter.tailings.block.VentState;
import dev.wildcatter.tailings.mine.Airflow;
import net.minecraft.server.level.ServerLevel;

/**
 * The drive's fans (docs/multiblocks/ventilation.md). A ventilation fan runs whenever it may (its selector, its PLC, its power), as a
 * real forcing fan runs round the clock, and once a second reads its duct line ({@link VentNetwork#read}). A booster fan runs the same
 * way; the main fan's reading counts it in.
 */
public final class Vent {
	private Vent() {
	}

	static final Stage FAN = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			part.want();
			VentState state = part.vent();
			if (state == null || !(part.getLevel() instanceof ServerLevel level)) {
				return;
			}
			if (part.scanDue()) {
				VentNetwork.read(level, part, state);
				if (state.ductCells() > 0 && part.running()) {
					ChunkRunner.refresh(level, part.getBlockPos());   // a line out to a far drive keeps its fan turning
				}
			}
			Airflow.Result air = state.air();
			if (!part.running()) {
				part.say("waiting for its motor");
			} else if (state.ductCells() == 0) {
				part.say(String.format(java.util.Locale.ROOT, "blowing %.1f m3/s into the open: no duct on its south face", air.fanFlow()));
			} else {
				part.say(String.format(java.util.Locale.ROOT, "blowing %.1f m3/s at %.0f Pa through %.0f m of duct; %s at its end%s", air.fanFlow(), air.pressurePa(), air.metres(),
						Airflow.describe(air.endFlow()), state.fault().isEmpty() ? "" : "; " + state.fault()));
			}
		}
	};

	static final Stage BOOSTER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			part.want();
			part.say(part.running() ? "boosting the duct: in at its north face, out of its south" : "waiting for its motor");
		}
	};
}
