package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Flotation reagents. Doses are grams per tonne of a 2.7 t block. One item is one kilogram. Copper sulfate is only for
 * feed that holds sphalerite. Lime is quicklime from calcite; Wildcatter has no lime item.
 */
public enum Reagent {
	XANTHATE("xanthate", "Potassium Amyl Xanthate", "50"),
	MIBC("mibc", "MIBC Frother", "20"),
	LIME("lime", "Quicklime", "500"),
	COPPER_SULFATE("copper_sulfate", "Copper Sulfate", "300");

	/** One block of rock, the mass the doses are written against. */
	public static final BigDecimal BLOCK_TONNES = new BigDecimal("2.7");

	private final String id;
	private final String name;
	private final BigDecimal gramsPerTonne;

	Reagent(String id, String name, String gramsPerTonne) {
		this.id = id;
		this.name = name;
		this.gramsPerTonne = new BigDecimal(gramsPerTonne);
	}

	public String id() {
		return id;
	}

	public String itemId() {
		return "tailings:" + id;
	}

	public String displayName() {
		return name;
	}

	/** Kilograms of this reagent one block of feed consumes. */
	public BigDecimal kilogramsPerBlock() {
		return gramsPerTonne.multiply(BLOCK_TONNES).divide(new BigDecimal("1000"), 8, RoundingMode.DOWN);
	}

	public boolean requiredFor(Rock rock) {
		if (this == COPPER_SULFATE) {
			return rock.contained(Mineral.SPHALERITE).signum() > 0;
		}
		return true;
	}

	public static Map<Reagent, BigDecimal> dose(Rock rock) {
		Map<Reagent, BigDecimal> dose = new EnumMap<>(Reagent.class);
		for (Reagent reagent : values()) {
			if (reagent.requiredFor(rock)) {
				dose.put(reagent, reagent.kilogramsPerBlock());
			}
		}
		return dose;
	}
}
