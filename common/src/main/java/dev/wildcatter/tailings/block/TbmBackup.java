package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.mine.TunnelDrill;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The gantries behind a machine: where each belongs, what is wrong with them, and setting them there. */
public final class TbmBackup {
	private TbmBackup() {
	}

	/** The next gantry this machine needs coupled, or null when it has all three. */
	public static @Nullable TbmGantry next(TbmCore core) {
		for (TbmGantry kind : TbmGantry.values()) {
			if (core.gantry(kind) == null) {
				return kind;
			}
		}
		return null;
	}

	/** The corner of a gantry's main cell at its place behind the machine, along the floor the machine has come down. */
	public static Vec3 slot(TbmCab cab, TbmGantry kind) {
		Vec3 cutter = cab.cutterCorner();
		BlockPos at = TbmMachine.offset(cab.machineFacing(), TbmGantry.LEFT_RAIL, 0, 0);
		double[] along = along(cab.machineFacing(), kind.row());
		TunnelDrill drill = cab.core().drill();
		double slice = cab.machineSlice();
		double y = cutter.y + 1 + drill.floorExact(slice - kind.row()) - drill.floorExact(slice);
		return new Vec3(cutter.x + at.getX() + along[0], y, cutter.z + at.getZ() + along[1]);
	}

	private static double[] along(Direction facing, int row) {
		return new double[] {-facing.getStepX() * row, -facing.getStepZ() * row};
	}

	/**
	 * Tows every gantry the machine has to its place: each runs on its own wheels along the twin track (GantryEntity#tow), and the rails
	 * under it set its height and pitch. The reel gantry is told where its cable runs back to and how much is paid out.
	 */
	public static void place(ServerLevel level, TbmCab cab) {
		for (TbmGantry kind : TbmGantry.values()) {
			UUID id = cab.core().gantry(kind);
			if (id != null && level.getEntity(id) instanceof GantryEntity gantry) {
				gantry.tow(slot(cab, kind), cab.machineFacing());
				if (kind == TbmGantry.REEL) {
					gantry.setCable(cab.core().junction(), (float) Math.max(0, cab.machineSlice() + 1));
				}
			}
		}
	}

	/**
	 * The quench tank rides the pump gantry: before the cab works this tick it takes the tank's water from there, and {@link
	 * #storeTank} hands it back after. No pump gantry, no tank; a pump gantry broken takes its water with it. A pump gantry in an unloaded
	 * chunk keeps what the cab last saw.
	 */
	public static @Nullable GantryEntity loadTank(ServerLevel level, TbmCab cab) {
		TbmCore core = cab.core();
		UUID id = core.gantry(TbmGantry.PUMP);
		if (id == null) {
			core.setTankAboard(false, 0);
			return null;
		}
		if (level.getEntity(id) instanceof GantryEntity pump && pump.isAlive()) {
			if (core.legacyTank() > 0) {
				pump.setWater(Math.min(TbmCore.TANK_MAX, pump.water() + core.legacyTank()));   // a world saved with the tank in the cab
				core.clearLegacyTank();
			}
			core.setTankAboard(true, pump.water());
			return pump;
		}
		if (level.isPositionEntityTicking(BlockPos.containing(slot(cab, TbmGantry.PUMP)))) {
			core.uncouple(TbmGantry.PUMP);   // its chunk is loaded and it is not there: it was broken, and its water with it
			core.setTankAboard(false, 0);
		} else {
			core.setTankAboard(false, core.tank());
		}
		return null;
	}

	public static void storeTank(@Nullable GantryEntity pump, TbmCore core) {
		if (pump != null && pump.isAlive()) {
			pump.setWater(core.tank());
		}
	}

	/** What is wrong behind the machine, or null: a gantry not coupled or gone, or off the track (its wheels found no rail under it). */
	public static @Nullable String fault(ServerLevel level, TbmCore core) {
		TunnelDrill drill = core.drill();
		for (TbmGantry kind : TbmGantry.values()) {
			UUID id = core.gantry(kind);
			if (id == null) {
				return kind.displayName() + " coupled behind it";
			}
			int ahead = -(kind.row() + 1);
			BlockPos place = new BlockPos(drill.worldX(TbmGantry.LEFT_RAIL + 1, ahead), drill.worldY(1, ahead), drill.worldZ(TbmGantry.LEFT_RAIL + 1, ahead));
			if (!(level.getEntity(id) instanceof GantryEntity gantry) || !gantry.isAlive()) {
				if (level.isPositionEntityTicking(place)) {
					core.uncouple(kind);   // its chunk is loaded and it is not there: it was broken
					return kind.displayName() + " coupled behind it";
				}
				continue;
			}
			// its own wheels say whether the rails still carry it where it stands
			if (!gantry.tow(gantry.position(), gantry.facing())) {
				return "track under the " + kind.displayName();
			}
		}
		return null;
	}

	/** The machine is dismantled: its gantries stand where they are. */
	public static void release(ServerLevel level, TbmCore core) {
		for (TbmGantry kind : TbmGantry.values()) {
			UUID id = core.gantry(kind);
			if (id != null && level.getEntity(id) instanceof GantryEntity gantry) {
				gantry.uncouple();
			}
			core.uncouple(kind);
		}
	}
}
