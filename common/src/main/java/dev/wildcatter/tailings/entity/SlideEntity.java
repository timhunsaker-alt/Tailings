package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.pit.Pouring;
import dev.wildcatter.tailings.pit.SlideMotion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A slide's ground on its way, drawn (dragline-hand-control.md, The slide): each block where it stood until it lets go, then running
 * down the face and out over the cut to its cell, by {@link SlideMotion}, until the server sets the real block there. Nothing here is
 * in the world: the pour service ({@link Pouring}, kept with the slope record) sets the blocks; this only carries their ways to the
 * client, a few hundred to an entity (a synced string holds about 30,000 characters). Never saved: a slide reloaded mid-run sets its
 * blocks undrawn.
 */
public class SlideEntity extends Entity {
	private static final EntityDataAccessor<String> DATA_WAYS = SynchedEntityData.defineId(SlideEntity.class, EntityDataSerializers.STRING);
	/** Blocks drawn by one entity. */
	public static final int PER_ENTITY = 420;
	/** How long after its last block arrives it stays, ticks (the last blocks set a little after they arrive). */
	private static final int LINGER = 80;

	private long until = Long.MAX_VALUE;
	private String shownFrom = "";
	private List<Way> shown = List.of();

	/** One block's way, as the client draws it. */
	public record Way(BlockPos from, Vec3 via, BlockPos to, BlockState state, long launch, long arrive) {
		/** The block's centre at {@code now}. */
		public Vec3 at(double now) {
			return SlideMotion.at(Vec3.atCenterOf(from), via, Vec3.atCenterOf(to), now - launch);
		}
	}

	public SlideEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}

	/** A slide's blocks, drawn by as many entities as it takes, each standing at its first block. */
	public static void show(ServerLevel level, List<Pouring.Slid> slid) {
		for (int from = 0; from < slid.size(); from += PER_ENTITY) {
			List<Pouring.Slid> part = slid.subList(from, Math.min(slid.size(), from + PER_ENTITY));
			SlideEntity e = dev.wildcatter.tailings.TailingsEntities.SLIDE.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
			if (e == null) {
				return;
			}
			Pouring.Slid first = part.get(0);
			e.setPos(first.via().x, first.via().y, first.via().z);
			long base = level.getGameTime();
			long last = base;
			StringBuilder out = new StringBuilder().append(base);
			for (Pouring.Slid s : part) {
				double all = SlideMotion.distance(Vec3.atCenterOf(s.from()), s.via()) + SlideMotion.distance(s.via(), Vec3.atCenterOf(s.to()));
				last = Math.max(last, s.launch() + SlideMotion.ticks(all));
				out.append(';').append(s.from().getX()).append(',').append(s.from().getY()).append(',').append(s.from().getZ()).append(',')
						.append(String.format(Locale.ROOT, "%.1f,%.1f,%.1f", s.via().x, s.via().y, s.via().z)).append(',').append(s.to().getX()).append(',')
						.append(s.to().getY()).append(',').append(s.to().getZ()).append(',').append(Block.getId(s.drawn())).append(',').append(s.launch() - base);
			}
			e.until = last + LINGER;
			e.entityData.set(DATA_WAYS, out.toString());
			level.addFreshEntity(e);
		}
	}

	/** Its blocks' ways, on the client. */
	public List<Way> ways() {
		String sent = entityData.get(DATA_WAYS);
		if (!sent.equals(shownFrom)) {
			shownFrom = sent;
			shown = parse(sent);
		}
		return shown;
	}

	private static List<Way> parse(String sent) {
		List<Way> out = new ArrayList<>();
		if (sent.isEmpty()) {
			return out;
		}
		try {
			String[] parts = sent.split(";");
			long base = Long.parseLong(parts[0]);
			for (int i = 1; i < parts.length; i++) {
				String[] p = parts[i].split(",");
				BlockPos from = new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
				Vec3 via = new Vec3(Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]));
				BlockPos to = new BlockPos(Integer.parseInt(p[6]), Integer.parseInt(p[7]), Integer.parseInt(p[8]));
				long launch = base + Long.parseLong(p[10]);
				double all = SlideMotion.distance(Vec3.atCenterOf(from), via) + SlideMotion.distance(via, Vec3.atCenterOf(to));
				out.add(new Way(from, via, to, Block.stateById(Integer.parseInt(p[9])), launch, launch + SlideMotion.ticks(all)));
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
		return d < 256 * 256;
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
		builder.define(DATA_WAYS, "");
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
