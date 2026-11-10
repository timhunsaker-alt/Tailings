package dev.wildcatter.tailings.rock;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A mineral a rock can hold. Grades for the Wildcatter ores are the game grades and densities in Wildcatter {@code Ore}
 * ({@code docs/ores.md}). One mineral item is the plain-mining yield of one ore block: a quarter of the block's mineral,
 * because plain mining is 25%. A nugget is a ninth of that item.
 */
public enum Mineral {
	IRON(Unit.INGOT, Separator.MAGNETIC, "minecraft:iron_nugget", "minecraft:iron_ingot", "minecraft:iron_ingot", new BigDecimal("4")),
	COPPER(Unit.INGOT, Separator.GRAVITY, "tailings:copper_nugget", "minecraft:copper_ingot", "minecraft:copper_ingot", new BigDecimal("4")),
	GOLD(Unit.INGOT, Separator.GRAVITY, "minecraft:gold_nugget", "minecraft:gold_ingot", "minecraft:gold_ingot", new BigDecimal("4")),

	ZIRCON(Unit.KILOGRAM, Separator.GRAVITY, nugget("zircon"), "tailings:zircon", null, heavy(new BigDecimal("0.25"))),
	ILMENITE(Unit.KILOGRAM, Separator.GRAVITY, nugget("ilmenite"), "tailings:ilmenite", null, heavy(new BigDecimal("0.55"))),
	RUTILE(Unit.KILOGRAM, Separator.GRAVITY, nugget("rutile"), "tailings:rutile", null, heavy(new BigDecimal("0.05"))),
	MONAZITE(Unit.KILOGRAM, Separator.GRAVITY, nugget("monazite"), "tailings:monazite", null, heavy(new BigDecimal("0.03"))),

	URANINITE(Unit.KILOGRAM, null, nugget("uraninite"), "wildcatter:uraninite", null, ore("2.3", "0.02")),
	PITCHBLENDE(Unit.KILOGRAM, null, nugget("pitchblende"), "wildcatter:pitchblende", null, ore("2.5", "0.60")),
	SULFUR(Unit.KILOGRAM, null, nugget("sulfur"), "wildcatter:sulfur", null, ore("2.7", "0.80")),
	HALITE(Unit.KILOGRAM, null, nugget("halite"), "wildcatter:halite", null, ore("2.16", "0.98")),
	SYLVITE(Unit.KILOGRAM, null, nugget("sylvite"), "wildcatter:sylvite", null, ore("2.1", "0.80")),
	FLUORITE(Unit.KILOGRAM, null, nugget("fluorite"), "wildcatter:fluorite", null, ore("2.7", "0.80")),
	BAUXITE(Unit.KILOGRAM, null, nugget("bauxite"), "wildcatter:bauxite", null, ore("2.1", "0.80")),
	GALENA(Unit.KILOGRAM, null, nugget("galena"), "wildcatter:galena", "wildcatter:lead_ingot", ore("2.8", "0.23")),
	SPHALERITE(Unit.KILOGRAM, null, nugget("sphalerite"), "wildcatter:sphalerite", "wildcatter:zinc_ingot", ore("2.8", "0.80")),
	CASSITERITE(Unit.KILOGRAM, Separator.GRAVITY, nugget("cassiterite"), "wildcatter:cassiterite", "wildcatter:tin_ingot", ore("2.65", "0.13")),
	PENTLANDITE(Unit.KILOGRAM, null, nugget("pentlandite"), "wildcatter:pentlandite", null, ore("2.9", "0.60")),
	CHROMITE(Unit.KILOGRAM, Separator.GRAVITY, nugget("chromite"), "wildcatter:chromite", null, ore("3.3", "0.80")),
	PLATINUM(Unit.KILOGRAM, null, nugget("platinum"), "wildcatter:platinum", null, ore("3.1", "0.000063")),
	MOLYBDENITE(Unit.KILOGRAM, null, nugget("molybdenite"), "wildcatter:molybdenite", null, ore("2.65", "0.03")),
	MAGNESITE(Unit.KILOGRAM, null, nugget("magnesite"), "wildcatter:magnesite", "wildcatter:magnesia", ore("2.9", "0.90")),
	DOLOMITE(Unit.KILOGRAM, null, nugget("dolomite"), "wildcatter:dolomite", null, ore("2.85", "0.95")),
	GRAPHITE(Unit.KILOGRAM, null, nugget("graphite"), "wildcatter:graphite", null, ore("2.7", "0.80")),
	BORAX(Unit.KILOGRAM, null, nugget("borax"), "wildcatter:borax", null, ore("2.1", "0.80")),
	SPODUMENE(Unit.KILOGRAM, null, nugget("spodumene"), "wildcatter:spodumene", null, ore("2.7", "0.80")),
	BARITE(Unit.KILOGRAM, null, nugget("barite"), "wildcatter:barite", null, ore("2.6", "0.90")),
	BENTONITE(Unit.KILOGRAM, null, nugget("bentonite"), "wildcatter:bentonite", null, ore("2.2", "0.85")),
	ACANTHITE(Unit.KILOGRAM, Separator.GRAVITY, nugget("acanthite"), "wildcatter:acanthite", "wildcatter:silver_ingot", ore("2.65", "0.003")),
	SCHEELITE(Unit.KILOGRAM, Separator.GRAVITY, nugget("scheelite"), "wildcatter:scheelite", null, ore("2.9", "0.065")),
	COBALTITE(Unit.KILOGRAM, null, nugget("cobaltite"), "wildcatter:cobaltite", null, ore("2.7", "0.09")),
	PYROLUSITE(Unit.KILOGRAM, null, nugget("pyrolusite"), "wildcatter:pyrolusite", null, ore("3.2", "0.80")),
	PHOSPHATE(Unit.KILOGRAM, null, nugget("phosphate"), "wildcatter:phosphate", null, ore("2.6", "0.80"));

	public enum Unit {
		KILOGRAM,
		INGOT
	}

	private final Unit unit;
	private final Separator separator;
	private final String nuggetId;
	private final String mineralItemId;
	private final String smeltedId;
	/** Kilograms in one ore block, or 1 for an ingot. The plain-mining item is a quarter of an ore block. */
	private final BigDecimal blockAmount;

	Mineral(Unit unit, Separator separator, String nuggetId, String mineralItemId, String smeltedId, BigDecimal blockAmount) {
		this.unit = unit;
		this.separator = separator;
		this.nuggetId = nuggetId;
		this.mineralItemId = mineralItemId;
		this.smeltedId = smeltedId;
		this.blockAmount = blockAmount;
	}

	public Unit unit() {
		return unit;
	}

	/**
	 * Kilograms of metal an ingot counts for in a slurry's assay: 100 kg, as Wildcatter counts an iron ingot (its Tuning, proposed).
	 * The assay is always in kg; the vanilla metals are paid in ingots, so the filter divides by this.
	 */
	public static final double INGOT_KG = 100;

	/** Kilograms in one of this mineral's {@link #unit()}: {@link #INGOT_KG} for an ingot, 1 for a kilogram. */
	public double kgPerUnit() {
		return unit == Unit.INGOT ? INGOT_KG : 1;
	}

	/** Null when this mineral is not paid at 50% in phase 1. */
	public Separator separator() {
		return separator;
	}

	/** What the spiral bank takes (gravity-circuit.md): whatever has a gravity or magnetic route and does not float. */
	public boolean spirals() {
		return separator != null && !floats();
	}

	/** Sulfides, vanilla copper and vanilla gold: the flotation bank's 75% route. */
	public boolean floats() {
		return switch (this) {
			case GALENA, SPHALERITE, PENTLANDITE, MOLYBDENITE, COBALTITE, ACANTHITE, PLATINUM, COPPER, GOLD -> true;
			default -> false;
		};
	}

	public String nuggetId() {
		return nuggetId;
	}

	/** What nine nuggets craft: the Wildcatter mineral, a Tailings stand-in, or the vanilla ingot. */
	public String mineralItemId() {
		return mineralItemId;
	}

	/** Ingot or calcine the 2x machine puts out, when this mineral smelts. Otherwise the machine puts out concentrate. */
	public String smeltedId() {
		return smeltedId;
	}

	public String concentrateId() {
		return "tailings:" + name().toLowerCase() + "_concentrate";
	}

	/** What one pass of the 50% machine returns for a whole ore block (a mined drop is no plant feed; specs-tailings.md X-1). */
	public String phaseOneProductId() {
		return smeltedId != null ? smeltedId : concentrateId();
	}

	public boolean vanillaNugget() {
		return nuggetId.startsWith("minecraft:");
	}

	/** Mass of one ore block of this mineral, in {@link #unit()}. */
	public BigDecimal blockAmount() {
		return blockAmount;
	}

	/** Mass of one mineral item: the plain-mining yield, a quarter of {@link #blockAmount()}. */
	public BigDecimal itemAmount() {
		return blockAmount.divide(BigDecimal.valueOf(4), 8, RoundingMode.DOWN);
	}

	/** A ninth of {@link #itemAmount()}. */
	public BigDecimal nuggetAmount() {
		return itemAmount().divide(BigDecimal.valueOf(9), 8, RoundingMode.DOWN);
	}

	private static String nugget(String id) {
		return "tailings:" + id + "_nugget";
	}

	/** tonnes per cubic metre times 1000 times the game grade, from Wildcatter {@code Ore}. */
	private static BigDecimal ore(String density, String grade) {
		return new BigDecimal(density).multiply(new BigDecimal("1000")).multiply(new BigDecimal(grade));
	}

	/** Heavy mineral sand is 1.8 t/m3 and 50% heavy minerals; {@code fraction} is that mineral's share of the heavies. */
	private static BigDecimal heavy(BigDecimal fraction) {
		return new BigDecimal("1.8").multiply(new BigDecimal("1000")).multiply(new BigDecimal("0.50")).multiply(fraction);
	}
}
