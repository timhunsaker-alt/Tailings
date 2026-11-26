package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestTracker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/**
 * The Built From Scratch check (bfs-check branch, not for main): can every item task in the quest book be made in a fresh survival world
 * with only the quests it requires done? It walks the server's real recipe manager (crafting, furnaces, stonecutter; tags resolved as the
 * game resolves them) from every vanilla item, the mods' items that come from the world (ores, animals, crops), and the items the mods'
 * machines make, each counted only once the quest that gives the player that machine is in the prerequisites. Machines make things in code,
 * not in recipe files, so that table is written by hand from the block entities (file:line beside each entry).
 * <p>
 * Two kinds of problem: an item task nothing makes even with every quest done (NO SOURCE), and one that needs a quest outside its
 * prerequisites (ORDER). Shared with the same class in Tailings' bfs-check branch; entries for a mod that is not loaded are skipped.
 */
final class QuestReach {
	private QuestReach() {
	}

	/** Mod items that come from the world: an ore block's drop, an animal, a crop. Always available. */
	static final Set<String> WORLD = Set.of(
			// Wildcatter ores: sulfur is the caprock ore's drop (salt domes: neoforge biome_modifier/salt_dome.json, SaltDomePlacement.java)
			"wildcatter:sulfur",
			// Farmings: eggs come from tamed birds (MACHINES, under the cattle quest that teaches taming)
			"minecraft:air");

	/**
	 * Items a mod's machine makes in code, and the quest that first gives the player that machine (or, for a fluid in a bucket, the quest
	 * whose machine fills it). Filled from the traces in QUEST_CHECK.md.
	 */
	static final Map<String, String> MACHINES = new LinkedHashMap<>();

	static {
		// Wildcatter (paths under common/src/main/java/dev/wildcatter/block/)
		MACHINES.put("wildcatter:crude_oil_bucket", "wildcatter:lease");          // StorageTankBlockEntity.java:147; the pumpjack's well fluid needs the separator (lease)
		MACHINES.put("wildcatter:diesel_bucket", "wildcatter:crude_unit");        // StorageTankBlockEntity.java:150, from ToppingColumnBlockEntity.java:33
		MACHINES.put("wildcatter:calcined_coke_bag", "wildcatter:crude_unit");    // CokeCoolerBlockEntity.java:36, on crude-unit residue; no quest teaches the coker
		MACHINES.put("wildcatter:plastic_sheet", "wildcatter:plastic");           // PelletizerBlockEntity.java:51
		MACHINES.put("wildcatter:quicklime_bag", "wildcatter:gas_well");          // LimeKilnBlockEntity.java:57; no quest teaches the kiln (gas, 480 V from step_up)
		MACHINES.put("wildcatter:barite_bag", "wildcatter:gas_well");             // MudMillBlockEntity.java:89; no quest teaches the barite mill (480 V)
		MACHINES.put("wildcatter:obm_package", "wildcatter:ammonia");             // MudMillBlockEntity.java:87: the bentonite dryer's oil-mud mode takes ammonia
		MACHINES.put("wildcatter:metallurgical_coke", "wildcatter:coke");         // CokeQuenchTowerBlockEntity.java:63
		MACHINES.put("wildcatter:aluminium_ingot", "wildcatter:aluminium");       // CastingMachineBlockEntity.java:63
		MACHINES.put("wildcatter:chlorine_ton_container", "wildcatter:chlor_alkali"); // ChlorineFillerBlockEntity.java:53
		MACHINES.put("wildcatter:dap_bag", "wildcatter:fertilizer");              // GranulationPlantBlockEntity.java:86
		MACHINES.put("wildcatter:tsp_bag", "wildcatter:fertilizer");              // GranulationPlantBlockEntity.java:86
		MACHINES.put("wildcatter:mop_bag", "wildcatter:fertilizer");              // PotashDryerBlockEntity.java:69 (no quest of its own)
		MACHINES.put("wildcatter:ferrosilicon_bag", "wildcatter:coke");           // SubmergedArcFurnaceBlockEntity.java:452; no quest teaches the furnace
		MACHINES.put("wildcatter:helium_cylinder", "wildcatter:helium");          // HeliumRecoveryBlockEntity.java:91
		MACHINES.put("wildcatter:boric_acid", "wildcatter:boron");                // BoronLineBlockEntity.java:227
		MACHINES.put("wildcatter:b4c_pellets", "wildcatter:boron");               // BoronLineBlockEntity.java:258
		MACHINES.put("wildcatter:purified_graphite_bag", "wildcatter:battery_graphite"); // GraphiteFurnaceBlockEntity.java:114
		MACHINES.put("wildcatter:lfp_cathode_bag", "wildcatter:cathode_powder");  // LfpKilnBlockEntity.java:160
		MACHINES.put("wildcatter:lfp_module", "wildcatter:lfp_module");           // CellLineBlockEntity.java:209
		MACHINES.put("wildcatter:yellowcake_drum", "wildcatter:yellowcake");      // DrumFillerBlockEntity.java:133
		MACHINES.put("wildcatter:uf6_cylinder_48y", "wildcatter:conversion");     // CylinderFillerBlockEntity.java:104
		MACHINES.put("wildcatter:leu_cylinder_30b", "wildcatter:enrichment");     // TakeoffBlockEntity.java:50
		MACHINES.put("wildcatter:zircaloy_tube", "wildcatter:zirconium");         // PilgerMillBlockEntity.java:136
		MACHINES.put("wildcatter:fuel_assembly", "wildcatter:fuel");              // AssemblyFixtureBlockEntity.java:196
		MACHINES.put("wildcatter:rcca", "wildcatter:rccas");                      // BoronLineBlockEntity.java:291
		// Tailings
		MACHINES.put("tailings:zircon_concentrate", "tailings:stamp_mill");       // rock/Milling.java:44-90: stamp mill, hand screen, shaking table
		MACHINES.put("tailings:copper_cathode", "tailings:copper_smelter");       // block/stage/SmelterStages.java:343
		// Farmings
		MACHINES.put("farmings:chicken_egg", "farmings:cattle");                  // a tamed hen lays (corral/CorralPostEntity.java:221 tames any species)
	}

	/** Items a quest needs that are not its tasks: a machine its milestone needs, or what that machine is fed by hand. */
	static final Map<String, List<String>> BUILDS = Map.ofEntries(
			Map.entry("wildcatter:casing_point", List.of("wildcatter:oilwell_cement")),          // RigSubstructureBlockEntity.java:398-405, 4 bags
			Map.entry("wildcatter:choke_kill", List.of("wildcatter:barite_bag")),                // kill mud to 13.5 ppg
			Map.entry("wildcatter:oil_mud", List.of("wildcatter:obm_package")),                  // RigSubstructureBlockEntity.java:1243-1251
			Map.entry("wildcatter:aluminium", List.of("wildcatter:pot")),                        // PotBlockEntity.java:425
			Map.entry("wildcatter:lfp_module", List.of("wildcatter:aluminium_ingot")),           // the cell line takes aluminium
			Map.entry("wildcatter:bess_block", List.of("wildcatter:bess_pcs")),                  // 1 MW needs the 2.5 MW PCS
			Map.entry("wildcatter:tower", List.of("wildcatter:plastic_sheet")),                  // NaturalDraftTowerBlockEntity.java:281
			Map.entry("wildcatter:critical", List.of("wildcatter:fuel_assembly", "wildcatter:rcca", "wildcatter:boric_acid")),
			Map.entry("tailings:copper_smelter", List.of("wildcatter:cable_mv")),                // the converter
			Map.entry("farmings:milk_quality", List.of("farmings:teat_dip")),                    // HealthItems.java:275
			Map.entry("farmings:footbath", List.of("farmings:zinc_sulfate")),                    // HealthBlocks.java:154
			Map.entry("farmings:vaccinate", List.of("farmings:vaccine_respiratory")));

	/** Every problem, one line each; empty when every item task can be made in order. */
	static List<String> problems(ServerLevel level) {
		MinecraftServer server = level.getServer();
		QuestBook book = QuestTracker.book(server);
		List<Rule> rules = rules(level);
		List<String> out = new ArrayList<>();
		BROKEN.forEach(r -> out.add("BROKEN RECIPE " + r + ": its result cannot be made, so it crafts nothing"));
		Set<String> everything = new HashSet<>(MACHINES.keySet());
		Set<Item> all = reach(rules, items(everything));
		for (QuestBook.Quest q : book.quests().values()) {
			Set<Identifier> done = new HashSet<>();
			prerequisites(book, q.id(), done);
			done.add(q.id());
			Set<String> unlocked = new HashSet<>();
			MACHINES.forEach((item, quest) -> {
				if (done.contains(Identifier.parse(quest))) {
					unlocked.add(item);
				}
			});
			Set<Item> have = reach(rules, items(unlocked));
			List<String> wanted = new ArrayList<>();
			for (QuestBook.Task t : q.tasks()) {
				if (t.kind() == QuestBook.Kind.ITEM) {
					wanted.add(t.target());
				}
			}
			List<String> builds = BUILDS.getOrDefault(q.id().toString(), List.of());
			wanted.addAll(builds);
			for (String target : wanted) {
				String what = builds.contains(target) ? target + " (built or fed, not a task)" : target;
				if (members(target).isEmpty()) {
					continue;   // another mod's item, not loaded here
				}
				if (!holds(all, target)) {
					out.add("NO SOURCE " + q.id() + ": " + what + " cannot be made even with every quest done");
				} else if (!holds(have, target)) {
					out.add("ORDER " + q.id() + ": " + what + " needs " + needs(rules, target, have) + ", which its prerequisites do not give");
				}
			}
		}
		return out;
	}

	private static void prerequisites(QuestBook book, Identifier id, Set<Identifier> out) {
		QuestBook.Quest q = book.quest(id);
		if (q == null) {
			return;
		}
		for (Identifier r : q.requires()) {
			if (out.add(r)) {
				prerequisites(book, r, out);
			}
		}
	}

	/** One recipe: its result, and for each ingredient the items that fill it. */
	record Rule(Item result, List<Set<Item>> slots, String id) {
	}

	/** Recipes whose result cannot be made (e.g. a count above the item's stack size): they craft nothing. */
	static final Set<String> BROKEN = new java.util.TreeSet<>();

	static List<Rule> rules(ServerLevel level) {
		BROKEN.clear();
		ContextMap context = SlotDisplayContext.fromLevel(level);
		List<Rule> out = new ArrayList<>();
		for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			Recipe<?> recipe = holder.value();
			if (recipe.placementInfo().isImpossibleToPlace()) {
				continue;   // a special recipe (dyeing, map copying): no fixed ingredients to walk
			}
			List<Set<Item>> slots = new ArrayList<>();
			for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
				Set<Item> fill = new HashSet<>();
				ingredient.items().map(Holder::value).forEach(fill::add);
				slots.add(fill);
			}
			for (RecipeDisplay display : recipe.display()) {
				ItemStack result = display.result().resolveForFirstStack(context);
				if (!result.isEmpty()) {
					out.add(new Rule(result.getItem(), slots, holder.id().identifier().toString()));
				} else {
					BROKEN.add(holder.id().identifier().toString());
				}
			}
		}
		return out;
	}

	/** Every item reachable from the vanilla items, the world's items and {@code extra}, through the recipes. */
	static Set<Item> reach(List<Rule> rules, Set<Item> extra) {
		Set<Item> have = new HashSet<>(extra);
		for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
			if (id.getNamespace().equals("minecraft")) {
				have.add(BuiltInRegistries.ITEM.getValue(id));
			}
		}
		have.addAll(items(WORLD));
		for (Item item : BuiltInRegistries.ITEM) {   // Wildcatter's ores and their minerals: every one generates (ore/Ore.java, worldgen/placed_feature)
			if (item instanceof dev.wildcatter.ore.MineralItem || item instanceof dev.wildcatter.ore.OreBlockItem) {
				have.add(item);
			}
		}
		boolean more = true;
		while (more) {
			more = false;
			for (Rule r : rules) {
				if (!have.contains(r.result()) && r.slots().stream().allMatch(s -> s.stream().anyMatch(have::contains))) {
					have.add(r.result());
					more = true;
				}
			}
		}
		return have;
	}

	/** For a report: the ingredients of the item's first recipe that are not in {@code have}, one level down. */
	private static String needs(List<Rule> rules, String target, Set<Item> have) {
		Set<String> missing = new java.util.TreeSet<>();
		for (Item item : members(target)) {
			for (Rule r : rules) {
				if (r.result() == item) {
					for (Set<Item> s : r.slots()) {
						if (s.stream().noneMatch(have::contains)) {
							s.stream().map(i -> BuiltInRegistries.ITEM.getKey(i).toString()).forEach(missing::add);
						}
					}
				}
			}
		}
		return missing.isEmpty() ? "a machine-made item with no entry in MACHINES for these prerequisites" : missing.toString();
	}

	private static boolean holds(Set<Item> have, String target) {
		return members(target).stream().anyMatch(have::contains);
	}

	private static List<Item> members(String target) {
		List<Item> out = new ArrayList<>();
		if (target.startsWith("#")) {
			BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, Identifier.parse(target.substring(1))))
					.ifPresent(set -> set.forEach(h -> out.add(h.value())));
		} else {
			Identifier id = Identifier.parse(target);
			if (BuiltInRegistries.ITEM.containsKey(id)) {
				out.add(BuiltInRegistries.ITEM.getValue(id));
			}
		}
		return out;
	}

	private static Set<Item> items(Set<String> ids) {
		Set<Item> out = new HashSet<>();
		for (String s : ids) {
			Identifier id = Identifier.parse(s);
			if (BuiltInRegistries.ITEM.containsKey(id)) {   // another mod's item, not loaded here: skipped
				out.add(BuiltInRegistries.ITEM.getValue(id));
			}
		}
		return out;
	}
}
