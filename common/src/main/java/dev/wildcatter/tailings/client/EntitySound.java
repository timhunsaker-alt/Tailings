package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.entity.SoundingEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** A moving machine's running loop: follows the entity, and fades out when it stops or is gone (as Wildcatter's MachineSound). */
public class EntitySound extends AbstractTickableSoundInstance {
	private static final Map<Entity, EntitySound> PLAYING = new WeakHashMap<>();
	private static final float FADE_PER_TICK = 0.05F;

	private final Entity entity;
	private final SoundEvent event;
	private float fade = 0;
	private float loudness = 0;
	private float tone = 1;

	private EntitySound(Entity entity, SoundEvent event) {
		super(event, SoundSource.BLOCKS, RandomSource.create());
		this.entity = entity;
		this.event = event;
		this.looping = true;
		this.delay = 0;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		this.volume = 0.01F;
		follow();
	}

	/** Called from a sounding entity's client tick: starts its loop if it should be playing and is not. */
	public static void start(Entity entity) {
		if (!(entity instanceof SoundingEntity sounding) || sounding.loop() == null) {
			return;
		}
		EntitySound current = PLAYING.get(entity);
		if (current != null && !current.isStopped() && current.event == sounding.loop()) {
			return;
		}
		EntitySound sound = new EntitySound(entity, sounding.loop());
		PLAYING.put(entity, sound);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	private void follow() {
		x = entity.getX();
		y = entity.getY();
		z = entity.getZ();
	}

	@Override
	public void tick() {
		boolean on = !entity.isRemoved() && entity instanceof SoundingEntity sounding && sounding.loop() == event && sounding.soundVolume() > 0;
		fade = on ? Math.min(1, fade + FADE_PER_TICK) : fade - FADE_PER_TICK;
		if (fade <= 0) {
			PLAYING.remove(entity, this);
			stop();
			return;
		}
		follow();
		if (on) {   // while fading out, keep the last loudness and let the pitch sag as it runs down
			SoundingEntity sounding = (SoundingEntity) entity;
			loudness = sounding.soundVolume();
			tone = sounding.soundPitch();
		} else {
			tone *= 0.97F;
		}
		volume = Math.max(0.01F, loudness * fade);
		pitch = Math.max(0.5F, Math.min(2.0F, tone));
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
