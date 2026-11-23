package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.pit.PitDrive;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A firing hole's flyrock and fragments, drawn (docs/multiblocks/open-pit-p3-blasting.md, Flyrock). Nothing here is in the world: the
 * shot ({@link dev.wildcatter.tailings.pit.Shot}) flies each fragment on the server, hurting what it hits and landing it; this carries
 * their arcs to the client, one entity a hole, as {@link SlideEntity} carries a slide's blocks. Never saved.
 */
public class FlyrockEntity extends Entity {
	private static final EntityDataAccessor<String> DATA_ARCS = SynchedEntityData.defineId(FlyrockEntity.class, EntityDataSerializers.STRING);
	/** Gravity, blocks a tick squared. */
	public static final double G_TICK = PitDrive.G / 400.0;
	private static final int LINGER = 10;

	private long until = Long.MAX_VALUE;
	private String shownFrom = "";
	private List<Arc> shown = List.of();

	/** One piece's flight: where it left, its speed (blocks a tick), when it left and landed (game ticks), its size and its rock. */
	public record Arc(Vec3 from, Vec3 v, long launch, long land, float size, BlockState state) {
		/** Where it is at game time {@code now} (ballistic, no drag). */
		public Vec3 at(double now) {
			double t = Math.max(0, Math.min(now, land) - launch);
			return position(from, v, t);
		}
	}

	/** A piece {@code t} ticks after leaving {@code from} at {@code v} blocks a tick. */
	public static Vec3 position(Vec3 from, Vec3 v, double t) {
		return new Vec3(from.x + v.x * t, from.y + v.y * t - G_TICK * t * t / 2, from.z + v.z * t);
	}

	public FlyrockEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}

	/** Draws {@code arcs} from one entity standing at {@code at}. */
	public static void show(ServerLevel level, Vec3 at, List<Arc> arcs) {
		if (arcs.isEmpty()) {
			return;
		}
		FlyrockEntity e = dev.wildcatter.tailings.TailingsEntities.FLYROCK.create(level, EntitySpawnReason.EVENT);
		if (e == null) {
			return;
		}
		e.setPos(at.x, at.y, at.z);
		long base = level.getGameTime();
		long last = base;
		StringBuilder out = new StringBuilder().append(base);
		for (Arc a : arcs) {
			last = Math.max(last, a.land());
			out.append(';').append(String.format(Locale.ROOT, "%.2f,%.2f,%.2f,%.4f,%.4f,%.4f,%d,%d,%.2f,%d", a.from().x, a.from().y, a.from().z,
					a.v().x, a.v().y, a.v().z, a.launch() - base, a.land() - base, a.size(), Block.getId(a.state())));
		}
		e.until = last + LINGER;
		e.entityData.set(DATA_ARCS, out.toString());
		level.addFreshEntity(e);
	}

	/** Its pieces' arcs, on the client. */
	public List<Arc> arcs() {
		String sent = entityData.get(DATA_ARCS);
		if (!sent.equals(shownFrom)) {
			shownFrom = sent;
			shown = parse(sent);
		}
		return shown;
	}

	private static List<Arc> parse(String sent) {
		List<Arc> out = new ArrayList<>();
		if (sent.isEmpty()) {
			return out;
		}
		try {
			String[] parts = sent.split(";");
			long base = Long.parseLong(parts[0]);
			for (int i = 1; i < parts.length; i++) {
				String[] p = parts[i].split(",");
				Vec3 from = new Vec3(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
				Vec3 v = new Vec3(Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]));
				out.add(new Arc(from, v, base + Long.parseLong(p[6]), base + Long.parseLong(p[7]), Float.parseFloat(p[8]), Block.stateById(Integer.parseInt(p[9]))));
			}
		} catch (RuntimeException ex) {
			// a garbled string draws nothing
		}
		return out;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server && server.getGameTime() > until) {
			discard();
		}
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double d) {
		return d < 320 * 320;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ARCS, "");
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
