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

	/** Sets every gantry the machine has at its place, pitched to the floor under it. */
	public static void place(ServerLevel level, TbmCab cab) {
		TunnelDrill drill = cab.core().drill();
		double slice = cab.machineSlice();
		for (TbmGantry kind : TbmGantry.values()) {
			UUID id = cab.core().gantry(kind);
			if (id != null && level.getEntity(id) instanceof GantryEntity gantry) {
				double front = slice - kind.row();
				double rise = drill.floorExact(front - kind.length()) - drill.floorExact(front);
				gantry.setAt(slot(cab, kind), cab.machineFacing(), (float) Math.toDegrees(Math.atan2(rise, kind.length())));
			}
		}
	}

	/** What is wrong behind the machine, or null: a gantry not coupled or gone, or no track under one. */
	public static @Nullable String fault(ServerLevel level, TbmCore core) {
		TunnelDrill drill = core.drill();
		RailView view = new RailView(level);
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
			for (int row = kind.row(); row < kind.row() + kind.length(); row++) {
				for (int across : new int[] {TbmGantry.LEFT_RAIL + 1, TbmGantry.RIGHT_RAIL + 1}) {
					int back = -(row + 1);
					if (view.at(drill.worldX(across, back), drill.worldY(1, back), drill.worldZ(across, back)) == null) {
						return "track under the " + kind.displayName();
					}
				}
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
