package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.pit.Slopes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The survey station (dragline-hand-control.md, Warnings): reads every slope prism it can see within 500 m once a minute, and each one's
 * speed and trend; a prism that accelerates gets a predicted slide time from its inverse-velocity trend. A Wildcatter control device:
 * ALARM when any prism passes the set speed, WORST SPEED 4-20 mA over 0-50 mm/h, PRISMS 4-20 mA over 0-20, and ALARM SET as an analog
 * output (0-50 mm/h, 10 by default). Wired to a switch house outlet's MACHINE RUN, the alarm stops the dragline.
 */
public class SurveyStationBlockEntity extends BlockEntity implements dev.wildcatter.control.ControlDevice {
	public static final int READ_TICKS = 1200;
	public static final double SPEED_MA_FULL = 50, PRISMS_MA_FULL = 20, ALARM_DEFAULT = 10;

	private double alarmAt = ALARM_DEFAULT;
	private double worst;
	private int seen;
	private boolean alarm;
	private final List<String> lines = new ArrayList<>();
	private long lastRead = Long.MIN_VALUE;

	public SurveyStationBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.SURVEY_STATION_ENTITY, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SurveyStationBlockEntity station) {
		if (level instanceof ServerLevel server && server.getGameTime() - station.lastRead >= READ_TICKS) {
			station.read(server);
		}
	}

	/** Reads every prism it can see, now. */
	public void read(ServerLevel level) {
		lastRead = level.getGameTime();
		lines.clear();
		double most = 0;
		int n = 0;
		Vec3 eye = Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
		List<String> rows = new ArrayList<>();
		for (BlockPos p : Slopes.prisms(level)) {
			if (!level.isLoaded(p) || p.distSqr(worldPosition) > Slopes.STATION_M * Slopes.STATION_M) {
				continue;
			}
			Vec3 target = Vec3.atCenterOf(p).add(0, 0.3, 0);
			HitResult hit = level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
			if (hit.getType() == HitResult.Type.BLOCK && !((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos().equals(p)) {
				continue;   // out of its line of sight
			}
			Slopes.Reading r = Slopes.read(level, p);
			n++;
			most = Math.max(most, r.mmPerHour());
			rows.add(String.format(Locale.ROOT, "  %s: %s", p.toShortString(), r.describe()));
		}
		boolean was = alarm;
		worst = most;
		seen = n;
		alarm = most > alarmAt;
		lines.add(String.format(Locale.ROOT, "Survey Station: %d prism%s in sight, worst %.1f mm/h, alarm at %.0f mm/h%s", n, n == 1 ? "" : "s", most, alarmAt, alarm ? ": ALARM" : ""));
		lines.addAll(rows);
		if (alarm && !was) {
			level.playSound(null, worldPosition, dev.wildcatter.tailings.TailingsSounds.SURVEY_ALARM, SoundSource.BLOCKS, 3F, 1F);
		}
		if (n >= 3) {
			dev.wildcatter.tailings.quest.Milestones.fire(level, worldPosition, dev.wildcatter.tailings.quest.Milestones.SLOPE_WATCHED);
		}
		setChanged();
	}

	public List<String> lines() {
		return List.copyOf(lines);
	}

	public boolean alarm() {
		return alarm;
	}

	public double worst() {
		return worst;
	}

	public int seen() {
		return seen;
	}

	public double alarmAt() {
		return alarmAt;
	}

	// ---- PLC ---------------------------------------------------------------------------------------------------------------------

	@Override
	public String deviceName() {
		return "Survey station";
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("ALARM", false), new Point("WORST SPEED 4-20 mA, 0-50 mm/h", false, Signal.MA), new Point("PRISMS 4-20 mA, 0-20", false, Signal.MA),
				new Point("ALARM SET 4-20 mA, 0-50 mm/h", true, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 0 && alarm;
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
	}

	@Override
	public double readMa(int point) {
		return switch (point) {
			case 1 -> 4 + 16 * Math.min(1, worst / SPEED_MA_FULL);
			case 2 -> 4 + 16 * Math.min(1, seen / PRISMS_MA_FULL);
			default -> 0;
		};
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (point == 3 && milliamps >= 3.8) {
			alarmAt = Math.max(0, Math.min(SPEED_MA_FULL, (milliamps - 4) / 16 * SPEED_MA_FULL));
			setChanged();
		}
	}

	/** Sets its alarm speed (tests). */
	public void alarmAt(double mmPerHour) {
		alarmAt = mmPerHour;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putDouble("alarm_at", alarmAt);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		alarmAt = input.getDoubleOr("alarm_at", ALARM_DEFAULT);
	}
}
