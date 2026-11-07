package dev.wildcatter.tailings;

import dev.wildcatter.WildcatterSounds;
import dev.wildcatter.tailings.plant.Machine;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

/** The mod's own sounds and which machine makes which (docs/sound.md). The files are in assets/tailings/sounds. */
public final class TailingsSounds {
	public static final SoundEvent JAW_CRUSHER = make("jaw_crusher");
	public static final SoundEvent CONE_CRUSHER = make("cone_crusher");
	public static final SoundEvent VIBRATING_SCREEN = make("vibrating_screen");
	public static final SoundEvent BALL_MILL = make("ball_mill");
	public static final SoundEvent BELT_CONVEYOR = make("belt_conveyor");
	public static final SoundEvent APRON_FEEDER = make("apron_feeder");
	public static final SoundEvent SLURRY_RUSH = make("slurry_rush");
	public static final SoundEvent AGITATOR = make("agitator");
	public static final SoundEvent PRESSURE_FILTER = make("pressure_filter");
	public static final SoundEvent ORE_SORTER = make("ore_sorter");
	public static final SoundEvent TBM_CUTTER = make("tbm_cutter");
	public static final SoundEvent MINE_LOCOMOTIVE = make("mine_locomotive");
	public static final SoundEvent MUCK_TIP = make("muck_tip");
	public static final SoundEvent MUCK_LOAD = make("muck_load");
	public static final SoundEvent RING_SET = make("ring_set");
	public static final SoundEvent CAR_COUPLE = make("car_couple");

	private TailingsSounds() {
	}

	private static SoundEvent make(String name) {
		var id = Tailings.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Touch this class early so the sounds are registered. */
	public static void register() {
	}

	/** A plant part's running loop, or null if it has no motor to hear. */
	public static @Nullable SoundEvent loop(Machine machine) {
		return switch (machine) {
			case JAW -> JAW_CRUSHER;
			case CONE -> CONE_CRUSHER;
			case SCREEN -> VIBRATING_SCREEN;
			case BALL_MILL -> BALL_MILL;
			case CONVEYOR_DRIVE -> BELT_CONVEYOR;
			case APRON_FEEDER -> APRON_FEEDER;
			case CYCLONE -> SLURRY_RUSH;
			case CONDITIONER, ACTIVATION_TANK, FLOTATION_BANK -> AGITATOR;
			case FILTER -> PRESSURE_FILTER;
			case ORE_SORTER -> ORE_SORTER;
			case BLOWER -> WildcatterSounds.FAN;
			case SPIRAL_BANK, CLEANER_BANK, CCD_THICKENER -> SLURRY_RUSH;
			case SCAVENGER_BANK, LEACH_TANK, BYPRODUCT_PLANT -> AGITATOR;
			case HEAP_PAD -> dev.wildcatter.WildcatterSounds.MOTOR_HUM;
			case REGRIND_MILL -> BALL_MILL;
			case ELECTROWINNING_BANK -> dev.wildcatter.WildcatterSounds.MOTOR_HUM;
			case THICKENER -> WildcatterSounds.MOTOR_HUM;
			default -> null;
		};
	}

	/** Its volume: above 1 reaches further, over 16 m times this. */
	public static float volume(Machine machine) {
		return switch (machine) {
			case BALL_MILL -> 2F;
			case JAW, CONE -> 1.5F;
			case FLOTATION_BANK -> 1.2F;
			case SCREEN, APRON_FEEDER -> 1F;
			case CONVEYOR_DRIVE, CONDITIONER, ACTIVATION_TANK -> 0.6F;
			case THICKENER -> 0.4F;
			default -> 0.8F;
		};
	}
}
