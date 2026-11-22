package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.FluidNode;
import dev.wildcatter.block.Lines;
import dev.wildcatter.fluid.AcidPlant;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.Smelter;
import dev.wildcatter.tailings.plant.SmelterState;
import dev.wildcatter.tailings.quest.Milestones;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The copper smelter line (docs/copper-smelter.md): the concentrate dryer, the flash smelting furnace, the Peirce-Smith converter, the
 * anode furnace, the anode casting wheel and the electrorefining tankhouse. The furnaces hand their copper on to the part touching them,
 * as ladles on a crane would; the furnace's and the converter's off-gas goes on the pipe as Wildcatter burner gas for an acid plant.
 * Each part's rate is the line's 1.68 t/h of copper times its motor's output (and a game test's {@link PlantBlockEntity#fast()}).
 */
public final class SmelterStages {
	private SmelterStages() {
	}

	/** The gases a burner fires on, as Wildcatter's gas burners do: any natural gas on its pipe. */
	private static final List<Product> GASES = List.of(Product.PIPELINE_GAS, Product.DRIED_GAS, Product.GAS, Product.COMPRESSED_GAS);
	/** The furnace's bin of dry concentrate, kg of copper (proposed: about 7 t of concentrate). */
	public static final double FURNACE_BIN_KG = 2000;
	/** Off-gas held at the outlet: 20 s of the furnace's rated flow. */
	private static final double GAS_HOLD_SECONDS = 20;
	public static final int[] OUTPUT_SLOTS = Flotation.OUTPUT_SLOTS;
	static final String VENT = "OFF-GAS HAS NOWHERE TO GO: pipe the east side of its back cell, top layer, to an acid plant's converter; a smelter must not vent";

	private static double rate(PlantBlockEntity part, double kgPerTick) {
		return kgPerTick * part.strength() * part.fast();
	}

	/** Burns {@code joules} of gas off the pipe at the east face of its front cells; the share of it it got. */
	private static double burn(PlantBlockEntity part, double joules) {
		double wanted = joules / dev.wildcatter.Tuning.GAS_JOULES_PER_M3;
		if (wanted < 1e-12) {
			return 1;
		}
		double got = 0;
		List<Lines.Face> east = part.faces(at -> at[2] == 0, Direction.EAST);
		for (Product gas : GASES) {
			if (got < wanted) {
				got += Lines.pull(part.getLevel(), east, gas, wanted - got, FluidNode.Pull.MACHINE, part);
			}
		}
		if (got < wanted * 0.5) {
			// half a burner's gas is no fire: give it back to nothing, and stop
			return 0;
		}
		part.gasBurnt(got);
		return Math.min(1, got / wanted);
	}

	/** kg of burner gas on the pipe a kg of sulfur, at Wildcatter's 11 % SO2. */
	private static double gasPerSulfur() {
		return AcidPlant.gasPerSulfur();
	}

	/** Off-gas room at its outlet, kg. */
	private static double gasRoom(PlantBlockEntity part) {
		double hold = GAS_HOLD_SECONDS * 20 * Smelter.CU_KG_PER_TICK * Smelter.sulfurPerCu() * gasPerSulfur() * part.fast();
		return Math.max(0, hold - part.out().held(Product.BURNER_GAS));
	}

	/** The off-gas outlet: the east face of the back cell, top layer, where the uptake or the hood's flue leaves. */
	private static boolean gasFace(PlantBlockEntity part, int[] at, Direction localSide) {
		return localSide == Direction.EAST && at[0] == part.machine().width() - 1 && at[1] == part.machine().height() - 1 && at[2] == part.machine().depth() - 1;
	}

	/** The copper a part can hold, kg. */
	public static double hold(Machine machine) {
		return switch (machine) {
			case CONCENTRATE_DRYER -> Smelter.DRYER_HOLD_KG;
			case FLASH_FURNACE -> FURNACE_BIN_KG;
			case COPPER_CONVERTER -> Smelter.MATTE_HOLD_KG;
			case ANODE_FURNACE -> Smelter.BLISTER_HOLD_KG;
			case ANODE_CASTING_WHEEL -> Smelter.WHEEL_HOLD_KG;
			case REFINERY_TANKHOUSE -> Smelter.TANKHOUSE_ANODES * Smelter.anodeKg();
			default -> 1;
		};
	}

	/** Its FILL: the copper held over its hold. */
	public static double fill(PlantBlockEntity part) {
		return part.smelter().copper() / hold(part.machine());
	}

	private static double room(@Nullable PlantBlockEntity next) {
		return next == null ? 0 : Math.max(0, hold(next.machine()) - next.smelter().copper());
	}

	/** The concentrate dryer: the filter's copper cake in, dried on gas and blown to the flash furnace touching it. */
	static final Stage DRYER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			SmelterState s = part.smelter();
			if (s.copper() < 1e-9) {
				part.say("NO FEED: filter cake from a pressure filter touching it");
				return;
			}
			part.want();
			PlantBlockEntity next = part.touching(Machine.FLASH_FURNACE);
			if (next == null) {
				part.say("NOT BUILT: needs a Flash Smelting Furnace touching it");
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(Math.min(rate(part, Smelter.CU_KG_PER_TICK), s.copper()), room(next));
			if (max < 1e-9) {
				part.say(Crushing.NOWHERE + " (the furnace's concentrate bin is full)");
				return;
			}
			double got = burn(part, max * Smelter.dryerJoulesPerCu());
			if (got <= 0) {
				part.say("NO GAS: pipeline gas on the pipe at the east side of its front");
				return;
			}
			double kg = s.take(max * got);
			next.smelter().add(kg);
			s.count(kg, 0, 0, 0);
			next.setChanged();
			part.setChanged();
			part.say("drying the concentrate to 0.3 % moisture");
		}
	};

	/** The flash smelting furnace: dry concentrate and silica burn in air; matte to the converter, slag counted, SO2 to the acid plant. */
	static final Stage FURNACE = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return gasFace(part, at, localSide) ? Product.BURNER_GAS : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			SmelterState s = part.smelter();
			s.setGasBacked(false);
			if (s.copper() < 1e-9) {
				part.say("NO FEED: dry concentrate from a concentrate dryer touching it");
				return;
			}
			part.want();
			PlantBlockEntity next = part.touching(Machine.COPPER_CONVERTER);
			if (next == null) {
				part.say("NOT BUILT: needs a Peirce-Smith Converter touching it for its matte");
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double keep = 1 - Smelter.FURNACE_SLAG_LOSS;
			double gasPerCu = Smelter.furnaceSulfurPerCu() * gasPerSulfur();
			double max = Math.min(Math.min(rate(part, Smelter.CU_KG_PER_TICK), s.copper()), room(next) / keep);
			if (max < 1e-9) {
				part.say(Crushing.NOWHERE + " (the converter is full of matte)");
				return;
			}
			if (part.dryKg() < max * Smelter.furnaceFluxPerCu()) {
				part.say("NO FLUX: silica sand by a hopper on top");
				return;
			}
			max = Math.min(max, gasRoom(part) / gasPerCu);
			if (max < 1e-9) {
				s.setGasBacked(true);
				part.say(VENT);
				return;
			}
			double got = burn(part, max * Smelter.furnaceJoulesPerCu());
			if (got <= 0) {
				part.say("NO GAS: pipeline gas on the pipe at the east side of its front, for its burners");
				return;
			}
			double kg = s.take(max * got);
			double lost = kg * Smelter.FURNACE_SLAG_LOSS;
			next.smelter().add(kg - lost);
			part.setDryKg(part.dryKg() - kg * Smelter.furnaceFluxPerCu());
			part.out().offer(Product.BURNER_GAS, kg * gasPerCu, SlurryAssay.EMPTY);
			s.count(kg, lost, kg * Smelter.slagPerCu(), kg * Smelter.furnaceSulfurPerCu());
			Milestones.fire(part.getLevel(), part.getBlockPos(), Milestones.SMELTER_RUNNING);
			next.setChanged();
			part.setChanged();
			part.say("smelting: matte to the converter, slag to waste, SO2 to the acid plant");
		}
	};

	/** The Peirce-Smith converter: air through the matte; blister copper to the anode furnace, its slag back to the furnace, SO2 to the acid plant. */
	static final Stage CONVERTER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return gasFace(part, at, localSide) ? Product.BURNER_GAS : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			SmelterState s = part.smelter();
			s.setGasBacked(false);
			if (s.copper() < 1e-9) {
				part.say("NO MATTE: from a flash furnace touching it");
				return;
			}
			part.want();
			PlantBlockEntity furnace = part.touching(Machine.FLASH_FURNACE);
			PlantBlockEntity next = part.touching(Machine.ANODE_FURNACE);
			if (furnace == null || next == null) {
				part.say("NOT BUILT: needs " + (furnace == null ? "the Flash Smelting Furnace touching it, for its slag" : "an Anode Furnace touching it, for its blister copper"));
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double on = 1 - Smelter.CONVERTER_SLAG_RETURN;
			double gasPerCu = Smelter.converterSulfurPerCu() * gasPerSulfur();
			double max = Math.min(Math.min(rate(part, Smelter.CU_KG_PER_TICK), s.copper()), room(next) / on);
			if (max < 1e-9) {
				part.say(Crushing.NOWHERE + " (the anode furnace is full)");
				return;
			}
			if (part.dryKg() < max * Smelter.CONVERTER_FLUX_PER_CU) {
				part.say("NO FLUX: silica sand by a hopper on top");
				return;
			}
			max = Math.min(max, gasRoom(part) / gasPerCu);
			if (max < 1e-9) {
				s.setGasBacked(true);
				part.say(VENT);
				return;
			}
			double kg = s.take(max);
			next.smelter().add(kg * on);
			furnace.smelter().add(kg * Smelter.CONVERTER_SLAG_RETURN);
			part.setDryKg(part.dryKg() - kg * Smelter.CONVERTER_FLUX_PER_CU);
			part.out().offer(Product.BURNER_GAS, kg * gasPerCu, SlurryAssay.EMPTY);
			s.count(kg, 0, 0, kg * Smelter.converterSulfurPerCu());
			next.setChanged();
			furnace.setChanged();
			part.setChanged();
			part.say("blowing: blister copper to the anode furnace, its slag back to the furnace");
		}
	};

	/** The anode furnace: blister fire refined on gas (air, then gas to take the oxygen out), into the casting wheel. */
	static final Stage ANODE_FURNACE = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			SmelterState s = part.smelter();
			if (s.copper() < 1e-9) {
				part.say("NO FEED: blister copper from a converter touching it");
				return;
			}
			part.want();
			PlantBlockEntity next = part.touching(Machine.ANODE_CASTING_WHEEL);
			if (next == null) {
				part.say("NOT BUILT: needs an Anode Casting Wheel touching it");
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(Math.min(rate(part, Smelter.CU_KG_PER_TICK), s.copper()), room(next));
			if (max < 1e-9) {
				part.say(Crushing.NOWHERE + " (the casting wheel's tundish is full)");
				return;
			}
			double got = burn(part, max * Smelter.ANODE_FURNACE_J_PER_CU_KG);
			if (got <= 0) {
				part.say("NO GAS: pipeline gas on the pipe at the east side of its front");
				return;
			}
			double kg = s.take(max * got);
			next.smelter().add(kg);
			s.count(kg, 0, 0, 0);
			next.setChanged();
			part.setChanged();
			part.say("fire refining: anode copper to the casting wheel");
		}
	};

	/** The casting wheel: anode copper into its moulds at 25 t/h, a 400 kg anode into its output stacks for each mould's worth. */
	static final Stage WHEEL = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			SmelterState s = part.smelter();
			double anode = Smelter.anodeKg();
			if (s.plated() >= anode - 1e-6) {
				if (!part.emitTo(OUTPUT_SLOTS, new ItemStack(TailingsItems.COPPER_ANODE))) {
					part.say("FULL: take the anodes");
					return;
				}
				s.unplate(anode);
				s.made(part.getLevel().getGameTime());
				part.setChanged();
			}
			if (s.copper() + s.plated() < anode - 1e-6) {
				part.say(String.format(Locale.ROOT, "waiting for a mould's worth: %.0f of %.0f kg", s.copper() + s.plated(), anode));
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double kg = s.take(Math.min(rate(part, Smelter.WHEEL_KGH / 3600 / 20), anode - s.plated()));
			s.plate(kg);
			s.count(kg, 0, 0, 0);
			part.setChanged();
			part.say("casting anodes");
		}
	};

	/**
	 * The tankhouse: anodes in; their copper plated by the DC its Wildcatter rectifier drives through its 72 cells (20.5 kA rated), a
	 * 57.1 kg cathode out for each cathode's worth. Its own motor is the electrolyte pumps: no circulation, no plating.
	 */
	static final Stage TANKHOUSE = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			SmelterState s = part.smelter();
			double cathode = Smelter.cathodeKg();
			if (s.plated() >= cathode - 1e-6) {
				if (!part.emitTo(OUTPUT_SLOTS, new ItemStack(TailingsItems.COPPER_CATHODE))) {
					part.electrolysis().setDrawing(part.getLevel(), false);
					part.say("FULL: take the cathodes");
					return;
				}
				s.unplate(cathode);
				s.made(part.getLevel().getGameTime());
				Milestones.fire(part.getLevel(), part.getBlockPos(), Milestones.CATHODE_PULLED);
				part.setChanged();
			}
			if (s.copper() < 1e-9) {
				part.electrolysis().setDrawing(part.getLevel(), false);
				part.say("NO ANODES: copper anodes by a hopper on top");
				return;
			}
			part.want();
			if (!part.running()) {
				part.electrolysis().setDrawing(part.getLevel(), false);
				part.say("waiting for its electrolyte pumps");
				return;
			}
			part.electrolysis().setDrawing(part.getLevel(), true);
			double amps = part.electrolysis().amps();
			if (amps < 1) {
				part.say("NO DC: set a " + dev.wildcatter.energy.Voltage.MEDIUM.label() + " rectifier against it (its cells want " + String.format(Locale.ROOT, "%.1f kA", Smelter.CELL_KA) + ")");
				return;
			}
			double kg = s.take(Math.min(Smelter.platedKgs(amps) / 20 * part.fast(), cathode - s.plated()));
			s.plate(kg);
			s.count(kg, 0, 0, 0);
			part.setChanged();
			part.say(String.format(Locale.ROOT, "refining: %d cells at %.1f kA, %.1f V DC from its rectifier", Smelter.CELLS, amps / 1000, part.electrolysis().volts()));
		}
	};

	/** The part's own readout: its copper, flux and totals. */
	public static String readout(PlantBlockEntity part) {
		SmelterState s = part.smelter();
		StringBuilder out = new StringBuilder(String.format(Locale.ROOT, "; %.0f kg of copper held", s.copper()));
		switch (part.machine()) {
			case CONCENTRATE_DRYER -> out.append(String.format(Locale.ROOT, "; %.1f t of concentrate dried (%.0f kg of copper)", s.fedKg() * Smelter.concPerCu() / 1000, s.fedKg()));
			case FLASH_FURNACE -> out.append(String.format(Locale.ROOT, "; flux %.0f kg; %.1f t of concentrate smelted (%.0f kg of copper); %.1f t of slag to waste carrying %.0f kg of copper; %.0f kg of sulfur to the acid plant",
					part.dryKg(), s.fedKg() * Smelter.concPerCu() / 1000, s.fedKg(), s.slagKg() / 1000, s.lostKg(), s.sulfurKg()));
			case COPPER_CONVERTER -> out.append(String.format(Locale.ROOT, "; flux %.0f kg; %.0f kg of copper blown; %.0f kg of sulfur to the acid plant", part.dryKg(), s.fedKg(), s.sulfurKg()));
			case ANODE_FURNACE -> out.append(String.format(Locale.ROOT, "; %.0f kg of copper refined", s.fedKg()));
			case ANODE_CASTING_WHEEL -> out.append(String.format(Locale.ROOT, "; %.0f of %.0f kg in the mould; %d anodes cast", s.plated(), Smelter.anodeKg(), s.madeCount()));
			case REFINERY_TANKHOUSE -> out.append(String.format(Locale.ROOT, "; %.1f anodes' copper in its cells; %.1f of %.1f kg on the next cathode; %d cathodes pulled", s.copper() / Smelter.anodeKg(),
					s.plated(), Smelter.cathodeKg(), s.madeCount()));
			default -> {
			}
		}
		return out.toString();
	}
}
