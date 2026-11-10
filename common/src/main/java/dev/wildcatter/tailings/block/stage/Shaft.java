package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import dev.wildcatter.tailings.shaft.Hoisting;
import dev.wildcatter.tailings.shaft.Winder;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * The shaft and hoist (docs/multiblocks/shaft-and-hoist.md, L4): the hoist house winds its two skips in balance between the measuring
 * flask at the shaft bottom and the headframe bin at the top; the flask measures each skip's 3 t from what the apron feeder brings it;
 * the bin feeds a belt under it.
 */
public final class Shaft {
	private Shaft() {
	}

	private static final double DT = 1 / 20.0;
	/** How long the SKIP DUMPED point stays on after a dump, ticks: one second, for a PLC's scan to see it. */
	private static final int PULSE_TICKS = 20;

	/** The hoist's last look at its shaft and its skips (not saved: found again after a load). */
	static final class Run {
		@Nullable ShaftLayout layout;
		final ShaftSkipEntity[] skips = new ShaftSkipEntity[2];
		long dumpedAt = Long.MIN_VALUE / 2;
	}

	private static Run run(PlantBlockEntity part) {
		if (part.run instanceof Run run) {
			return run;
		}
		Run run = new Run();
		part.run = run;
		return run;
	}

	/** The headframe bin's fill, 0 to 1, for the hoist's FILL point; 0 with no shaft found. */
	public static double binLevel(PlantBlockEntity hoist) {
		ShaftLayout layout = run(hoist).layout;
		return layout == null || layout.bin == null ? 0 : layout.bin.hold().total() / Hoisting.BIN_KG;
	}

	/** True for a second after each skip dumps: the SKIP DUMPED point. */
	public static boolean dumpedRecently(PlantBlockEntity hoist) {
		return hoist.getLevel() != null && hoist.getLevel().getGameTime() - run(hoist).dumpedAt < PULSE_TICKS;
	}

	/** The shaft the hoist last found, or null. */
	public static @Nullable ShaftLayout layout(PlantBlockEntity hoist) {
		return run(hoist).layout;
	}

	private static final String[] SIDE = {"west", "east"};

	static final Stage HOIST = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			Run run = run(part);
			Winder winder = part.winder();
			boolean scan = run.layout == null || part.scanDue() || !run.layout.stillStands();
			if (scan) {
				run.layout = ShaftLayout.find(part);
			}
			ShaftLayout layout = run.layout;
			if (layout.problem != null || layout.frame == null) {
				winder.brake();
				part.say("NOT BUILT: " + layout.problem);
				return;
			}
			ServerLevel level = (ServerLevel) part.getLevel();
			if (scan || run.skips[0] == null || run.skips[1] == null) {
				List<ShaftSkipEntity> hung = ShaftSkipEntity.near(level, layout.frame);
				for (int which = 0; which < 2; which++) {
					final int c = which;
					run.skips[which] = hung.stream().filter(s -> s.compartment() == c).findFirst().orElse(null);
				}
			}
			for (int which = 0; which < 2; which++) {
				ShaftSkipEntity skip = run.skips[which];
				if (skip == null || skip.isRemoved()) {
					run.skips[which] = null;
					winder.brake();
					if (!winder.load(which).isEmpty()) {
						winder.lose(which);
						part.setChanged();
					}
					part.say("NOT BUILT: needs a skip on each rope: the " + SIDE[which] + " compartment has none (click the headframe with a skip)");
					return;
				}
			}
			winder.setTravel(layout.travel);
			if (winder.winding()) {
				part.want();
				if (!part.running()) {
					winder.brake();
					part.say("STOPPED: the brakes hold the skips (" + (part.strength() > 0 ? "its motor is starting" : "no power to its motor") + ")");
				} else {
					winder.wind(DT, part.strength());
					part.say(String.format("WINDING at %.1f m/s, the %s skip coming up loaded", winder.speed(), SIDE[winder.bottom()]));
				}
				part.setChanged();
			} else if (winder.rest() > 0) {
				winder.rested(DT);
				part.say("loading and dumping");
			} else {
				ends(part, layout, winder, run);
			}
			place(layout, run.skips, winder);
		}
	};

	/** At rest with the rest run out: the top skip dumps if the bin has room, the bottom one loads if the flask holds its 3 t, then it winds. */
	private static void ends(PlantBlockEntity part, ShaftLayout layout, Winder winder, Run run) {
		PlantBlockEntity bin = layout.bin, flask = layout.flask;
		RockLoad top = winder.load(winder.top());
		if (!top.isEmpty() && bin.room() >= top.total() - 1e-6) {
			bin.receive(winder.dump());
			run.dumpedAt = part.getLevel().getGameTime();
			part.setChanged();
		}
		if (winder.load(winder.bottom()).isEmpty() && flask.hold().total() >= Hoisting.FLASK_KG - 1e-6) {
			winder.loadBottom(flask.hold().take(Double.MAX_VALUE));
			flask.setChanged();
			part.setChanged();
		}
		if (!winder.load(winder.top()).isEmpty()) {
			part.say("BIN FULL: the headframe bin has no room for the skip's load");
			return;
		}
		if (winder.load(winder.bottom()).isEmpty()) {
			part.say(String.format("WAITING FOR ORE: the flask holds %.1f of %.0f t", flask.hold().total() / 1000, Hoisting.FLASK_KG / 1000));
			return;
		}
		part.want();
		if (!part.running()) {
			part.say("waiting for its motor");
			return;
		}
		winder.start();
		part.say("WINDING");
		part.setChanged();
	}

	/** Sets each skip where its drum puts it, shows its load, and runs its rope up to the sheave and back to the drum. */
	private static void place(ShaftLayout layout, ShaftSkipEntity[] skips, Winder winder) {
		for (int which = 0; which < 2; which++) {
			ShaftSkipEntity skip = skips[which];
			skip.setAt(layout.frame.skipCorner(which, layout.bottomY + winder.height(which)), layout.frame.facing());
			skip.setLoaded(!winder.load(which).isEmpty());
			skip.setRope((float) layout.frame.sheaveTop(), layout.drum);
		}
	}

	/** The measuring flask: what the apron feeder drops in its surge runs into the flask until it holds one skip's 3 t. */
	static final Stage FLASK = new Stage() {
		@Override
		public double holdKg() {
			return Hoisting.FLASK_KG;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			double room = Hoisting.FLASK_KG - part.hold().total();
			if (!part.made().isEmpty() && room > 1e-6) {
				part.hold().add(part.made().take(Math.min(room, Hoisting.FLASK_FILL_KG_PER_SECOND * DT)));
				part.setChanged();
			}
			if (part.hold().total() >= Hoisting.FLASK_KG - 1e-6) {
				part.say("FULL: 3 t measured for the next skip");
			} else if (part.made().isEmpty()) {
				part.say(String.format("NO FEED: %.1f of %.0f t (an apron feeder from a dump pocket, its tail end touching the flask)", part.hold().total() / 1000, Hoisting.FLASK_KG / 1000));
			} else {
				part.say(String.format("filling: %.1f of %.0f t", part.hold().total() / 1000, Hoisting.FLASK_KG / 1000));
			}
		}
	};

	/**
	 * A vanilla container (a hopper, a chest) under the bin's bottom, for a bin with no belt under it, while
	 * {@code headframe_hopper} is on in {@code config/tailings-rules.properties} (off by default; docs/questions/gaps2.md #4); never a
	 * plant part.
	 */
	private static net.minecraft.world.@Nullable Container container(PlantBlockEntity part) {
		for (dev.wildcatter.block.Lines.Face face : part.side(Direction.DOWN)) {
			if (part.getLevel().getBlockEntity(face.pipe()) instanceof net.minecraft.world.Container box && !(box instanceof PlantBlockEntity)
					&& !(dev.wildcatter.multiblock.BigParts.entity(part.getLevel(), face.pipe()) instanceof PlantBlockEntity)) {
				return box;
			}
		}
		return null;
	}

	/**
	 * Out of its bottom into the container under it as items, a block's worth (2.7 t) of one rock and size an item, at the belt's
	 * rate: one item each {@link #ITEM_TICKS}. Less than a block of a rock stays in the bin.
	 */
	private static void bag(PlantBlockEntity part, net.minecraft.world.Container box) {
		Object[] whole = {null, null};
		part.hold().forEach((rock, size, kg) -> {
			if (whole[0] == null && kg >= dev.wildcatter.tailings.plant.PlantRates.BLOCK_KG - 1e-6) {
				whole[0] = rock;
				whole[1] = size;
			}
		});
		if (whole[0] == null) {
			part.say(String.format("less than a block of any rock for the hopper under it; holds %.1f t", part.hold().total() / 1000));
			return;
		}
		if (part.getLevel().getGameTime() % Math.max(1, ITEM_TICKS / part.fast()) != 0) {
			part.say(String.format("feeding %s to the hopper under it; holds %.0f t", Crushing.main(part.hold()), part.hold().total() / 1000));
			return;
		}
		Rock rock = (Rock) whole[0];
		SizeClass size = (SizeClass) whole[1];
		net.minecraft.world.item.ItemStack item = dev.wildcatter.tailings.block.Feeds.stack(size == null ? dev.wildcatter.tailings.block.Feeds.blockId(rock) : size.itemId(rock), 1);
		if (item.isEmpty()) {
			return;
		}
		net.minecraft.world.item.ItemStack left = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, box, item, Direction.UP);
		if (!left.isEmpty()) {
			part.say(String.format("FULL: the container under it is full; holds %.0f t", part.hold().total() / 1000));
			return;
		}
		part.hold().take(dev.wildcatter.tailings.plant.PlantRates.BLOCK_KG, (r, s) -> r == rock && s == size);
		box.setChanged();
		part.say(String.format("feeding %s to the hopper under it; holds %.0f t", Crushing.main(part.hold()), part.hold().total() / 1000));
		part.setChanged();
	}

	/** Ticks between items into a container: a block's 2.7 t at the belt's rate. */
	static final int ITEM_TICKS = (int) Math.ceil(dev.wildcatter.tailings.plant.PlantRates.BLOCK_KG / Belts.BELT_KG_PER_TICK);

	/** The headframe bin: what the skips dump, out of its bottom onto the belt under it, or into a hopper under it as items. */
	static final Stage BIN = new Stage() {
		@Override
		public double holdKg() {
			return Hoisting.BIN_KG;
		}

		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return true;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.hold().isEmpty()) {
				part.say("NO FEED: empty");
				return;
			}
			PlantBlockEntity.Sink sink = part.sink(part.side(Direction.DOWN));
			if (sink == null) {
				boolean hopper = dev.wildcatter.tailings.OptInRules.on(part.getLevel().getServer(), dev.wildcatter.tailings.OptInRules.HEADFRAME_HOPPER);
				net.minecraft.world.Container box = hopper ? container(part) : null;
				if (box != null) {
					bag(part, box);
					return;
				}
				part.say(String.format("%s (a belt%s under it); holds %.0f t", Crushing.NOWHERE, hopper ? " or a hopper" : "", part.hold().total() / 1000));
				return;
			}
			double max = Math.min(Belts.BELT_KG_PER_TICK, sink.room(part.hold()));
			if (max < 1e-6) {
				part.say("FULL: its belt is full");
				return;
			}
			sink.put(part.hold().take(max));
			part.say(String.format("feeding %s; holds %.0f t", Crushing.main(part.hold()), part.hold().total() / 1000));
			part.setChanged();
		}
	};
}
