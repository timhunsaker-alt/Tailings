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
		@Nullable ShaftSkipEntity cage;
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

	/** How long the skip's 3 t takes to pour out of its bottom door, s (proposed: a few seconds of the 10 s rest). */
	static final double POUR_SECONDS = 4;

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
				run.cage = hung.stream().filter(ShaftSkipEntity::cage).findFirst().orElse(null);
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
			if (cage(part, layout, winder, run)) {
				place(layout, run.skips, winder);
				return;
			}
			if (winder.winding()) {
				part.want();
				if (!part.running()) {
					winder.brake();
					part.say("STOPPED: the brakes hold the skips (" + (part.strength() > 0 ? "its motor is starting" : "no power to its motor") + ")");
				} else {
					if (winder.wind(DT, part.strength()) && pours(layout, winder)) {
						ShaftSkipEntity top = run.skips[winder.top()];
						((ServerLevel) part.getLevel()).playSound(null, top.getX(), top.getY(), top.getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP,
								net.minecraft.sounds.SoundSource.BLOCKS, 3F, 0.8F);
					}
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
			pour(level, run.skips, winder, layout);
		}
	};

	/** The top skip is at the dump with a load the bin has room for: its bottom door opens and it pours through the rest. */
	private static boolean pours(ShaftLayout layout, Winder winder) {
		RockLoad top = winder.load(winder.top());
		return !winder.winding() && !top.isEmpty() && layout.bin != null && layout.bin.room() >= top.total() - 1e-6;
	}

	/**
	 * The rock pouring out of the bottom of the skip at the dump into the bin under it, over the first {@link #POUR_SECONDS} of the
	 * rest; the skip shows empty once it has poured. (Its load still goes into the bin's count at the rest's end, as before.)
	 */
	private static void pour(ServerLevel level, ShaftSkipEntity[] skips, Winder winder, ShaftLayout layout) {
		if (!pours(layout, winder) || winder.rest() <= 0) {
			return;
		}
		double poured = Hoisting.REST_SECONDS - winder.rest();
		ShaftSkipEntity top = skips[winder.top()];
		if (poured >= POUR_SECONDS) {
			top.setLoaded(false);
			return;
		}
		{
			// out of its bottom door down the headframe's chute into the bin: aimed at the bin's top middle
			net.minecraft.world.phys.Vec3 c = top.getBoundingBox().getCenter();
			double y = top.getBoundingBox().minY - 0.2;
			net.minecraft.core.Direction binFacing = layout.bin.getBlockState().getValue(dev.wildcatter.block.BigPartBlock.FACING);
			net.minecraft.core.BlockPos mid = layout.bin.getBlockPos().offset(dev.wildcatter.tailings.block.TbmMachine.offset(binFacing, 2, 5, 2));
			double tx = mid.getX() - c.x, ty = mid.getY() - y, tz = mid.getZ() - c.z;
			double fall = Math.max(8, Math.sqrt(2 * Math.max(1, -ty) / 0.04));   // ticks to fall that far under a block particle's gravity
			double vx = tx / fall, vz = tz / fall;
			double heavy = 1 - poured / POUR_SECONDS;
			var random = level.getRandom();
			int n = (int) (20 + 50 * heavy);
			for (int i = 0; i < n; i++) {
				var rock = i % 3 == 0 ? net.minecraft.world.level.block.Blocks.GRAVEL : net.minecraft.world.level.block.Blocks.COBBLESTONE;
				level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, rock.defaultBlockState()),
						c.x + (random.nextDouble() - 0.5) * 1.2, y + random.nextDouble() * 0.4, c.z + (random.nextDouble() - 0.5) * 1.2, 0,
						vx * (0.8 + 0.4 * random.nextDouble()), -0.05 - 0.1 * random.nextDouble(), vz * (0.8 + 0.4 * random.nextDouble()), 1);
			}
			level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.FALLING_DUST,
					net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState()), c.x + tx * 0.3, y + ty * 0.3, c.z + tz * 0.3, (int) (4 + 8 * heavy), 0.8, 1.5, 0.8, 0);
		}
	}

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
		if (winder.cageBusy()) {
			part.say("waiting for the cage");
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

	/**
	 * The cage (heavy-iron-fixes): beside the skips on the hoist's second rope, between the collar and the bottom landing. A rider's bell
	 * or a call from a landing sends it once the skips are at rest; it rings for 5 s, then winds at the man-riding 4 m/s; the skips wait
	 * for it. True while the cage has the hoist (the skips stand).
	 */
	private static boolean cage(PlantBlockEntity part, ShaftLayout layout, Winder winder, Run run) {
		ShaftSkipEntity cage = run.cage;
		if (cage == null || cage.isRemoved()) {
			run.cage = null;
			return false;
		}
		winder.setCageTravel(layout.frame.collarY() - layout.bottomY);
		int call = cage.takeCall();
		boolean bell = cage.takeBell();
		if (call >= 0) {
			winder.callCage(call == 1);
		} else if (bell && winder.cageAtLanding()) {
			winder.sendCage();
		}
		boolean has = winder.cageBusy() && !winder.winding() && winder.rest() <= 0;
		if (has) {
			part.want();
			if (!part.running()) {
				part.say("CAGE: waiting for its motor");
			} else {
				boolean ringing = winder.cageRinging();
				if (winder.windCage(DT, part.strength())) {
					((ServerLevel) part.getLevel()).playSound(null, cage.getX(), cage.getY(), cage.getZ(), net.minecraft.sounds.SoundEvents.BELL_BLOCK,
							net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 1.4F);
				} else if (ringing && part.getLevel().getGameTime() % 20 == 0) {
					// the bell: the signal to the hoistman, as the gate shuts
					((ServerLevel) part.getLevel()).playSound(null, cage.getX(), cage.getY(), cage.getZ(), net.minecraft.sounds.SoundEvents.BELL_BLOCK,
							net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 1.0F);
				}
				part.say(winder.cageRinging() ? "CAGE: the bell, going " + (winder.cageUp() ? "down" : "up")
						: winder.cageBusy() ? String.format("CAGE at %.1f m/s, %.0f m below the collar", winder.cageSpeed(), winder.cageTravel() - winder.cageAt())
						: "CAGE at the " + (winder.cageUp() ? "collar" : "bottom landing"));
			}
			part.setChanged();
		}
		cage.setAt(layout.frame.skipCorner(ShaftLayout.CAGE, layout.bottomY + winder.cageAt()), layout.frame.facing());
		cage.setRope((float) layout.frame.sheaveTop(), layout.drum);
		return has;
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

	// ---- the raise borer (shaft-and-hoist.md, L4b; heavy-iron-fixes) ------------------------------------------------------------

	/** The reamed shaft's cells in the borer's own frame (facing north from its main cell): x 0 to 4, z 1 to 5; its centre x 2, z 3. */
	static final int SHAFT_X = 0, SHAFT_Z = 1, CENTRE_X = 2, CENTRE_Z = 3;

	private static net.minecraft.core.BlockPos at(PlantBlockEntity part, int x, int y, int z) {
		Direction facing = part.getBlockState().getValue(dev.wildcatter.block.BigPartBlock.FACING);
		return part.getBlockPos().offset(dev.wildcatter.tailings.block.TbmMachine.offset(facing, x, y, z));
	}

	/** The drive under the borer: metres down its centre column to the first open cell under at least 3 m of rock; -1 for none. */
	public static int findDrive(PlantBlockEntity part) {
		net.minecraft.world.level.Level level = part.getLevel();
		for (int k = 1; k <= dev.wildcatter.tailings.shaft.RaiseBoring.MAX_DEPTH; k++) {
			net.minecraft.core.BlockPos p = at(part, CENTRE_X, -k, CENTRE_Z);
			if (p.getY() <= level.getMinY()) {
				return -1;
			}
			net.minecraft.world.level.block.state.BlockState s = level.getBlockState(p);
			if (s.isAir() || s.canBeReplaced()) {
				if (k - 1 < dev.wildcatter.tailings.shaft.RaiseBoring.MIN_COVER) {
					return -1;   // open ground under the pad: not a drive
				}
				// a drive has a floor within a few metres of its roof; a cave or an open pit does not
				for (int down = 1; down <= 6; down++) {
					net.minecraft.world.level.block.state.BlockState under = level.getBlockState(p.below(down));
					if (!under.isAir() && !under.canBeReplaced()) {
						return k;
					}
				}
				return -1;
			}
		}
		return -1;
	}

	/**
	 * The raise borer: placed on a level pad over a drive, it drills its pilot down the centre column a metre each 10 s, breaks into the
	 * drive, and reams the 5 x 5 back up a metre each 60 s from the drive's roof to under the collar slab, each layer's rock falling into
	 * the drive as its items; any open or flooded cell round a layer is grouted. Its 400 kW from the medium-voltage tier.
	 */
	static final Stage RAISE_BORER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			ServerLevel level = (ServerLevel) part.getLevel();
			dev.wildcatter.tailings.shaft.RaiseBoring.Progress p = part.raise();
			if (p.phase() == dev.wildcatter.tailings.shaft.RaiseBoring.Phase.SITING) {
				int drive = findDrive(part);
				if (drive < 0) {
					part.say(p.readout());
					return;
				}
				p.found(drive);
				part.setChanged();
			}
			if (p.phase() == dev.wildcatter.tailings.shaft.RaiseBoring.Phase.DONE) {
				part.say(p.readout());
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor: " + p.readout());
				return;
			}
			dev.wildcatter.tailings.shaft.RaiseBoring.Phase before = p.phase();
			if (p.run(DT * part.fast(), part.strength()) > 0) {
				if (before == dev.wildcatter.tailings.shaft.RaiseBoring.Phase.PILOT) {
					// the pilot's cuttings flushed up the hole and out at the collar
					net.minecraft.core.BlockPos collar = at(part, CENTRE_X, 0, CENTRE_Z);
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
							level.getBlockState(at(part, CENTRE_X, -p.pilot(), CENTRE_Z))), collar.getX() + 0.5, collar.getY() + 0.2, collar.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.05);
				} else {
					ream(level, part, p);
				}
			}
			part.setChanged();
			part.say(p.readout());
		}
	};

	/** One reamed layer: its rock falls into the drive as items; open or flooded cells round it grouted. */
	private static void ream(ServerLevel level, PlantBlockEntity part, dev.wildcatter.tailings.shaft.RaiseBoring.Progress p) {
		int y = -p.drive() + p.reamed();
		net.minecraft.core.BlockPos floor = at(part, CENTRE_X, -p.drive(), CENTRE_Z);
		var random = level.getRandom();
		for (int x = SHAFT_X - 1; x <= SHAFT_X + dev.wildcatter.tailings.shaft.RaiseBoring.SHAFT; x++) {
			for (int z = SHAFT_Z - 1; z <= SHAFT_Z + dev.wildcatter.tailings.shaft.RaiseBoring.SHAFT; z++) {
				net.minecraft.core.BlockPos pos = at(part, x, y, z);
				net.minecraft.world.level.block.state.BlockState s = level.getBlockState(pos);
				boolean inside = x >= SHAFT_X && x < SHAFT_X + dev.wildcatter.tailings.shaft.RaiseBoring.SHAFT && z >= SHAFT_Z && z < SHAFT_Z + dev.wildcatter.tailings.shaft.RaiseBoring.SHAFT;
				if (!inside) {
					if (s.isAir() || !s.getFluidState().isEmpty()) {
						level.setBlock(pos, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
					}
					continue;
				}
				if (s.isAir() || s.hasBlockEntity() || s.getDestroySpeed(level, pos) < 0) {
					continue;
				}
				for (net.minecraft.world.item.ItemStack drop : net.minecraft.world.level.block.Block.getDrops(s, level, pos, null)) {
					net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level,
							floor.getX() + 0.5 + (random.nextDouble() - 0.5) * 4, floor.getY() + 0.3, floor.getZ() + 0.5 + (random.nextDouble() - 0.5) * 4, drop);
					level.addFreshEntity(item);
				}
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			}
		}
		net.minecraft.core.BlockPos mid = at(part, CENTRE_X, y, CENTRE_Z);
		level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
				net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()), mid.getX() + 0.5, mid.getY() - 1, mid.getZ() + 0.5, 60, 2, 1, 2, 0.1);
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
	 * the world's {@code headframe_hopper} rule is on (on in new worlds; docs/questions/gaps2.md #4); never a
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
