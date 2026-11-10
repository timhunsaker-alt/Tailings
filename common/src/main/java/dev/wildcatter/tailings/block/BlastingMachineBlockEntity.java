package dev.wildcatter.tailings.block;

import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.item.BlastLoggerItem;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.Shot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The blasting machine (open-pit-p3-blasting.md, Tying in and firing). The logger loads the shot into it; it must stand 50-500 m from
 * every hole. With the key in, ARM tests the shot (each hole's detonator answers; it reads holes, kg, tonnes, powder factor, and what
 * will not fire), and FIRE within 30 s sounds a siren for 10 s at the shot and fires it. A player in the danger zone asks a second FIRE.
 * After a shot with misfires it reads WAIT for 30 s. A shot will not arm or fire with a machine in the rock it breaks.
 *
 * <p>Its PLC points (a {@link ControlDevice}): ARM and FIRE (commands, on a rising edge, only with the key in), ARMED, FIRED (pulses),
 * HOLES (4-20 mA over 200) and MISFIRES (pulses).
 */
public class BlastingMachineBlockEntity extends BlockEntity implements Inspectable, ControlDevice {
	public static final int ARM = 0, FIRE = 1, ARMED = 2, FIRED = 3, HOLES = 4, MISFIRES = 5;

	private final List<Shot.Hole> holes = new ArrayList<>();
	private boolean key;
	private long armedUntil = -1;
	private long sirenUntil = -1;
	private long warnedUntil = -1;
	private long waitUntil = -1;
	private @Nullable Shot shot;
	private int shotTicks;
	private long firedCount;
	private long misfireCount;
	private boolean lastArm, lastFire;
	private String status = "NOT LOGGED: load a shot with the logger";

	public BlastingMachineBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.BLASTING_MACHINE_ENTITY, pos, state);
	}

	public String status() {
		return status;
	}

	public boolean key() {
		return key;
	}

	public boolean armed() {
		return level != null && level.getGameTime() < armedUntil;
	}

	public boolean firing() {
		return sirenUntil >= 0 || shot != null;
	}

	public @Nullable Shot lastShot() {
		return lastShot;
	}

	private @Nullable Shot lastShot;

	public List<Shot.Hole> holes() {
		return List.copyOf(holes);
	}

	/** Test hook: the key turned in or out. */
	public void setKey(boolean in) {
		key = in;
		setChanged();
	}

	public boolean insertKey() {
		if (key) {
			return false;
		}
		key = true;
		status = "key in";
		setChanged();
		return true;
	}

	public boolean takeKey() {
		if (!key) {
			return false;
		}
		key = false;
		armedUntil = -1;
		status = "key out";
		setChanged();
		return true;
	}

	/** The logger's shot, loaded: every hole 50-500 m off. */
	public String load(ItemStack logger) {
		List<Shot.Hole> loaded = BlastLoggerItem.holes(logger);
		if (loaded.isEmpty()) {
			return say("NOT LOGGED: the logger has no holes");
		}
		Vec3 here = Vec3.atCenterOf(worldPosition);
		double near = Double.MAX_VALUE, far = 0;
		for (Shot.Hole h : loaded) {
			double d = Math.hypot(h.collar().getX() + 0.5 - here.x, h.collar().getZ() + 0.5 - here.z);
			near = Math.min(near, d);
			far = Math.max(far, d);
		}
		if (near < Blasting.FIRING_MIN) {
			return say(String.format(Locale.ROOT, "TOO CLOSE: firing point inside the danger zone (%.0f m from a hole; at least %.0f m)", near, Blasting.FIRING_MIN));
		}
		if (far > Blasting.FIRING_MAX) {
			return say(String.format(Locale.ROOT, "TOO FAR: %.0f m is past the %.0f m lead line", far, Blasting.FIRING_MAX));
		}
		holes.clear();
		holes.addAll(loaded);
		armedUntil = -1;
		setChanged();
		return say(holes.size() + " holes loaded");
	}

	/** ARM: tests the shot. */
	public String arm() {
		if (!(level instanceof ServerLevel server)) {
			return status;
		}
		if (!key) {
			return say("NO KEY");
		}
		if (firing()) {
			return status;
		}
		if (server.getGameTime() < waitUntil) {
			return say(String.format(Locale.ROOT, "WAIT: after a misfire the zone stays closed %d s", (waitUntil - server.getGameTime() + 19) / 20));
		}
		if (holes.isEmpty()) {
			return say("NOT LOGGED: load a shot with the logger");
		}
		String machine = Shot.machineIn(server, Shot.plan(server, holes));
		if (machine != null) {
			return say(machine);
		}
		int answering = 0;
		double kg = 0, tonnes = 0;
		List<String> wont = new ArrayList<>();
		for (Shot.Hole h : holes) {
			if (!(server.getBlockEntity(h.collar()) instanceof BlastholeBlockEntity hole)) {
				wont.add(h.collar().toShortString() + " gone");
				continue;
			}
			String why = Shot.fires(hole);
			if (why != null) {
				wont.add(h.collar().toShortString() + " " + why);
			} else {
				answering++;
				kg += hole.chargeKg();
				tonnes += hole.boxTonnes();
				if (hole.stemming() < Blasting.BLOWOUT_STEMMING_M) {
					wont.add(String.format(Locale.ROOT, "%s stemmed %.1f m: it will blow out", h.collar().toShortString(), hole.stemming()));
				}
			}
		}
		armedUntil = server.getGameTime() + Blasting.ARMED_TICKS;
		setChanged();
		return say(String.format(Locale.ROOT, "ARMED: %d of %d holes answer; %.0f kg, %.0f t, %.2f kg/t%s", answering, holes.size(), kg, tonnes,
				Blasting.powderFactor(kg, tonnes), wont.isEmpty() ? "" : "; " + String.join("; ", wont)));
	}

	/** FIRE: armed, key in; a player in the danger zone asks a second press. */
	public String fire() {
		if (!(level instanceof ServerLevel server)) {
			return status;
		}
		if (!key) {
			return say("NO KEY");
		}
		if (firing()) {
			return status;
		}
		if (!armed()) {
			return say("not ARMED: ARM it first (it stays armed 30 s)");
		}
		String machine = Shot.machineIn(server, Shot.plan(server, holes));
		if (machine != null) {
			armedUntil = -1;
			return say(machine);
		}
		if (server.getGameTime() >= warnedUntil) {
			Player inZone = playerInZone(server);
			if (inZone != null) {
				warnedUntil = server.getGameTime() + Blasting.ARMED_TICKS;
				return say("PLAYER IN DANGER ZONE (" + inZone.getName().getString() + "): press FIRE again to fire");
			}
		}
		warnedUntil = -1;
		armedUntil = -1;
		sirenUntil = server.getGameTime() + Blasting.SIREN_TICKS;
		// the machine keeps its own chunk ticking until the shot is done: the player may stand out of range of the firing point
		ChunkRunner.refresh(server, worldPosition);
		setChanged();
		return say("SIREN: firing in 10 s");
	}

	private @Nullable Player playerInZone(ServerLevel server) {
		for (Player player : server.players()) {
			if (player.isSpectator()) {
				continue;
			}
			for (Shot.Hole h : holes) {
				if (player.position().distanceTo(Vec3.atCenterOf(h.collar())) < Blasting.HARM_RADIUS) {
					return player;
				}
			}
		}
		return null;
	}

	/** Test hook: skip the siren. */
	public void sirenNow() {
		if (sirenUntil >= 0 && level != null) {
			sirenUntil = level.getGameTime();
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BlastingMachineBlockEntity machine) {
		if (level instanceof ServerLevel server) {
			machine.serverTick(server);
		}
	}

	private void serverTick(ServerLevel server) {
		long now = server.getGameTime();
		if (sirenUntil >= 0 || shot != null) {
			if (now % ChunkRunner.REFRESH_TICKS == 0) {
				ChunkRunner.refresh(server, worldPosition);
			}
			for (Shot.Hole h : holes) {
				if (now % 40 == 0) {
					ChunkRunner.refresh(server, h.collar());
				}
			}
		}
		if (sirenUntil >= 0) {
			if ((sirenUntil - now) % 40 == 0 && !holes.isEmpty()) {
				BlockPos at = holes.get(0).collar();
				server.playSound(null, at, SoundEvents.RAID_HORN.value(), SoundSource.BLOCKS, 8F, 1.0F);
			}
			if (now >= sirenUntil) {
				sirenUntil = -1;
				shot = new Shot(server, holes);
				shotTicks = 0;
				say("FIRING");
			}
			return;
		}
		if (shot != null) {
			if (!shot.done()) {
				shot.tick();
			} else {
				finishShot(server);
			}
			shotTicks++;
			return;
		}
		if (lastShot != null && shotTicks < 400) {
			lastShot.dust(shotTicks++);
		}
	}

	private void finishShot(ServerLevel server) {
		Shot done = shot;
		shot = null;
		lastShot = done;
		firedCount++;
		misfireCount += done.misfires();
		if (done.misfires() > 0) {
			waitUntil = server.getGameTime() + Blasting.MISFIRE_WAIT_TICKS;
		}
		holes.clear();
		shotTicks = 0;
		setChanged();
		say(done.summary() + (done.misfires() > 0 ? "; MISFIRES: WAIT 30 s; " + String.join("; ", done.notes()) : ""));
	}

	private String say(String what) {
		status = what;
		return what;
	}

	// ---- the control cable -------------------------------------------------------------------------------------------------------

	@Override
	public String deviceName() {
		return "Blasting machine";
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("ARM", true), new Point("FIRE", true), new Point("ARMED", false), new Point("FIRED", false, Signal.PULSE),
				new Point("HOLES 4-20 mA, 0-" + Blasting.LOGGER_HOLES, false, Signal.MA), new Point("MISFIRES", false, Signal.PULSE));
	}

	@Override
	public boolean readPoint(int point) {
		return point == ARMED && armed();
	}

	/** ARM and FIRE on their rising edge; the machine does nothing without its key. */
	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == ARM) {
			if (on && !lastArm) {
				arm();
			}
			lastArm = on;
		} else if (point == FIRE) {
			if (on && !lastFire) {
				fire();
			}
			lastFire = on;
		}
	}

	@Override
	public double readMa(int point) {
		return point == HOLES ? 4 + 16.0 * Math.min(Blasting.LOGGER_HOLES, holes.size()) / Blasting.LOGGER_HOLES : 0;
	}

	@Override
	public long readPulses(int point) {
		return point == FIRED ? firedCount : point == MISFIRES ? misfireCount : 0;
	}

	@Override
	public Component inspect() {
		return Component.literal("Blasting Machine: " + status + (key ? "; key in" : "; NO KEY") + "; " + holes.size() + " holes loaded");
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("key", key);
		StringBuilder shot = new StringBuilder();
		holes.forEach(h -> shot.append(shot.isEmpty() ? "" : ",").append(h.collar().asLong()).append('@').append(h.delayMs()));
		output.putString("holes", shot.toString());
		output.putLong("wait_until", waitUntil);
		output.putLong("fired", firedCount);
		output.putLong("misfires", misfireCount);
		output.putString("status", status);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		key = input.getBooleanOr("key", false);
		holes.clear();
		for (String part : input.getStringOr("holes", "").split(",")) {
			int at = part.indexOf('@');
			if (at > 0) {
				try {
					holes.add(new Shot.Hole(BlockPos.of(Long.parseLong(part.substring(0, at))), Integer.parseInt(part.substring(at + 1))));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
		waitUntil = input.getLongOr("wait_until", -1);
		firedCount = input.getLongOr("fired", 0);
		misfireCount = input.getLongOr("misfires", 0);
		status = input.getStringOr("status", status);
	}
}
