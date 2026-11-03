package dev.wildcatter.tailings.rock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One feed rock and the mineral it holds. Country-rock traces are crustal abundance times ten, written as a fixed
 * amount so a nugget is reachable; granite zircon is the 8 kg in {@code docs/multiblocks/crushing-circuit.md}.
 * Wildcatter ore blocks hold {@link Mineral#blockAmount()}. Vanilla iron, copper and gold hold four ingots, so plain
 * smelting is one and the 50% route is two.
 */
public enum Rock {
	COBBLESTONE("cobblestone", "Cobblestone", Kind.COUNTRY),
	STONE("stone", "Stone", Kind.COUNTRY),
	GRANITE("granite", "Granite", Kind.COUNTRY),
	DIORITE("diorite", "Diorite", Kind.COUNTRY),
	ANDESITE("andesite", "Andesite", Kind.COUNTRY),
	DEEPSLATE("deepslate", "Deepslate", Kind.COUNTRY),
	TUFF("tuff", "Tuff", Kind.COUNTRY),
	SANDSTONE("sandstone", "Sandstone", Kind.COUNTRY),
	IRON("iron", "Iron Ore", Kind.VANILLA),
	COPPER("copper", "Copper Ore", Kind.VANILLA),
	GOLD("gold", "Gold Ore", Kind.VANILLA),
	URANINITE("uraninite", "Uranium Ore", Kind.ORE),
	PITCHBLENDE("pitchblende", "Pitchblende", Kind.ORE),
	SULFUR("sulfur", "Native Sulfur", Kind.ORE),
	HALITE("halite", "Rock Salt", Kind.ORE),
	SYLVITE("sylvite", "Potash", Kind.ORE),
	FLUORITE("fluorite", "Fluorspar", Kind.ORE),
	HEAVY_MINERAL("heavy_mineral", "Heavy Mineral Sand", Kind.ORE),
	BAUXITE("bauxite", "Bauxite", Kind.ORE),
	GALENA("galena", "Galena Ore", Kind.ORE),
	SPHALERITE("sphalerite", "Sphalerite Ore", Kind.ORE),
	CASSITERITE("cassiterite", "Tin Ore", Kind.ORE),
	PENTLANDITE("pentlandite", "Nickel Ore", Kind.ORE),
	CHROMITE("chromite", "Chromitite", Kind.ORE),
	PLATINUM("platinum", "Platinum Reef", Kind.ORE),
	MOLYBDENITE("molybdenite", "Molybdenum Ore", Kind.ORE),
	MAGNESITE("magnesite", "Magnesite", Kind.ORE),
	DOLOMITE("dolomite", "Dolomite", Kind.ORE),
	GRAPHITE("graphite", "Graphite Schist", Kind.ORE),
	BORAX("borax", "Borax Ore", Kind.ORE),
	SPODUMENE("spodumene", "Spodumene Pegmatite", Kind.ORE),
	BARITE("barite", "Barite Ore", Kind.ORE),
	BENTONITE("bentonite", "Bentonite", Kind.ORE),
	ACANTHITE("acanthite", "Silver Ore", Kind.ORE),
	SCHEELITE("scheelite", "Tungsten Ore", Kind.ORE),
	COBALTITE("cobaltite", "Cobalt Ore", Kind.ORE),
	PYROLUSITE("pyrolusite", "Manganese Ore", Kind.ORE),
	PHOSPHATE("phosphate", "Phosphate Rock", Kind.ORE);

	public enum Kind {
		COUNTRY,
		VANILLA,
		ORE
	}

	private final String id;
	private final String name;
	private final Kind kind;
	private final Map<Mineral, BigDecimal> contained = new EnumMap<>(Mineral.class);

	Rock(String id, String name, Kind kind) {
		this.id = id;
		this.name = name;
		this.kind = kind;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return name;
	}

	public Kind kind() {
		return kind;
	}

	public BigDecimal contained(Mineral mineral) {
		return contained.getOrDefault(mineral, BigDecimal.ZERO);
	}

	public List<Mineral> minerals() {
		return List.copyOf(contained.keySet());
	}

	public boolean pays(Separator separator) {
		for (Mineral mineral : contained.keySet()) {
			if (mineral.separator() == separator) {
				return true;
			}
		}
		return false;
	}

	/** Wildcatter ore block id, or the vanilla name for country rock and vanilla metal. */
	public String oreBlockId() {
		return switch (kind) {
			case ORE -> "wildcatter:" + id + "_ore";
			case VANILLA, COUNTRY -> "minecraft:" + id;
		};
	}

	static {
		trace(COBBLESTONE, Mineral.IRON, "0.02");
		trace(COBBLESTONE, Mineral.SPHALERITE, "0.4");
		trace(COBBLESTONE, Mineral.GALENA, "0.3");
		trace(STONE, Mineral.IRON, "0.02");
		trace(STONE, Mineral.SPHALERITE, "0.4");
		trace(STONE, Mineral.GALENA, "0.3");
		trace(GRANITE, Mineral.ZIRCON, "8");
		trace(GRANITE, Mineral.MONAZITE, "0.6");
		trace(GRANITE, Mineral.CASSITERITE, "0.675");
		trace(GRANITE, Mineral.MOLYBDENITE, "0.068");
		trace(DIORITE, Mineral.IRON, "0.05");
		trace(DIORITE, Mineral.ILMENITE, "5");
		trace(ANDESITE, Mineral.COPPER, "0.04");
		trace(ANDESITE, Mineral.GOLD, "0.004");
		trace(DEEPSLATE, Mineral.IRON, "0.02");
		trace(DEEPSLATE, Mineral.SPHALERITE, "0.4");
		trace(DEEPSLATE, Mineral.GALENA, "0.3");
		trace(DEEPSLATE, Mineral.PENTLANDITE, "5");
		trace(DEEPSLATE, Mineral.CHROMITE, "30");
		trace(TUFF, Mineral.URANINITE, "0.5");
		trace(SANDSTONE, Mineral.ZIRCON, "5.75");
		trace(SANDSTONE, Mineral.ILMENITE, "12.65");
		trace(SANDSTONE, Mineral.RUTILE, "1.15");
		trace(SANDSTONE, Mineral.MONAZITE, "0.69");
		trace(SANDSTONE, Mineral.URANINITE, "0.2");
		whole(IRON, Mineral.IRON);
		whole(COPPER, Mineral.COPPER);
		whole(GOLD, Mineral.GOLD);
		whole(URANINITE, Mineral.URANINITE);
		whole(PITCHBLENDE, Mineral.PITCHBLENDE);
		whole(SULFUR, Mineral.SULFUR);
		whole(HALITE, Mineral.HALITE);
		whole(SYLVITE, Mineral.SYLVITE);
		whole(FLUORITE, Mineral.FLUORITE);
		whole(HEAVY_MINERAL, Mineral.ZIRCON);
		whole(HEAVY_MINERAL, Mineral.ILMENITE);
		whole(HEAVY_MINERAL, Mineral.RUTILE);
		whole(HEAVY_MINERAL, Mineral.MONAZITE);
		whole(BAUXITE, Mineral.BAUXITE);
		whole(GALENA, Mineral.GALENA);
		whole(SPHALERITE, Mineral.SPHALERITE);
		whole(CASSITERITE, Mineral.CASSITERITE);
		whole(PENTLANDITE, Mineral.PENTLANDITE);
		whole(CHROMITE, Mineral.CHROMITE);
		whole(PLATINUM, Mineral.PLATINUM);
		whole(MOLYBDENITE, Mineral.MOLYBDENITE);
		whole(MAGNESITE, Mineral.MAGNESITE);
		whole(DOLOMITE, Mineral.DOLOMITE);
		whole(GRAPHITE, Mineral.GRAPHITE);
		whole(BORAX, Mineral.BORAX);
		whole(SPODUMENE, Mineral.SPODUMENE);
		whole(BARITE, Mineral.BARITE);
		whole(BENTONITE, Mineral.BENTONITE);
		whole(ACANTHITE, Mineral.ACANTHITE);
		whole(SCHEELITE, Mineral.SCHEELITE);
		whole(COBALTITE, Mineral.COBALTITE);
		whole(PYROLUSITE, Mineral.PYROLUSITE);
		whole(PHOSPHATE, Mineral.PHOSPHATE);
		for (Rock rock : values()) {
			if (rock.contained.isEmpty()) {
				throw new IllegalStateException(rock + " has no mineral list");
			}
		}
	}

	private static void trace(Rock rock, Mineral mineral, String amount) {
		rock.contained.put(mineral, new BigDecimal(amount));
	}

	private static void whole(Rock rock, Mineral mineral) {
		rock.contained.put(mineral, mineral.blockAmount());
	}

	public static List<Rock> wildcatterOres() {
		List<Rock> ores = new ArrayList<>();
		for (Rock rock : values()) {
			if (rock.kind == Kind.ORE) {
				ores.add(rock);
			}
		}
		return Collections.unmodifiableList(ores);
	}
}
