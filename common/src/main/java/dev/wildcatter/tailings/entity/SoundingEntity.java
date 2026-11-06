package dev.wildcatter.tailings.entity;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** A moving machine with a running sound (docs/sound.md): Wildcatter's {@code Sounding}, for an entity. */
public interface SoundingEntity {
	/** The loop, or null when it is silent. */
	@Nullable SoundEvent loop();

	/** 1.0 at its usual speed. */
	float soundPitch();

	float soundVolume();

	/** Set by the client: starts an entity's loop if it is not already playing. */
	final class Hook {
		public static Consumer<Entity> start = entity -> {
		};

		private Hook() {
		}
	}
}
