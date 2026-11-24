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
	/** A highwall or spoil pile creeping: a deep ground groan (dragline-hand-control.md). */
	public static final SoundEvent HIGHWALL_CREAK = make("highwall_creak");
	/** Loose ground ravelling off a cracking face. */
	public static final SoundEvent RAVEL = make("ravel");
	/** A slope sliding: a long low rumble. */
	public static final SoundEvent HIGHWALL_SLIDE = make("highwall_slide");
	/** The survey station's alarm. */
	public static final SoundEvent SURVEY_ALARM = make("survey_alarm");
	/** The dragline's swing brake, set by hand. */
	public static final SoundEvent DRAGLINE_SWING_BRAKE = make("dragline_swing_brake");
	public static final SoundEvent MUCK_LOAD = make("muck_load");
	public static final SoundEvent RING_SET = make("ring_set");
	public static final SoundEvent CAR_COUPLE = make("car_couple");
	public static final SoundEvent GYRATORY_CRUSHER = make("gyratory_crusher");
	public static final SoundEvent SAG_MILL = make("sag_mill");
	public static final SoundEvent LARGE_BALL_MILL = make("large_ball_mill");
	public static final SoundEvent MINE_HOIST = make("mine_hoist");
	public static final SoundEvent XRT_SORTER = make("xrt_sorter");
	public static final SoundEvent ROTARY_DRILL = make("rotary_drill");
	public static final SoundEvent TRACK_DOZER = make("track_dozer");
	public static final SoundEvent ROCK_BREAKER = make("rock_breaker");
	public static final SoundEvent SLURRY_PUMP = make("slurry_pump");
	public static final SoundEvent TANK_CELL = make("tank_cell");
	public static final SoundEvent BLAST_SIREN = make("blast_siren");

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
			case FILTER, TAILINGS_FILTER -> PRESSURE_FILTER;
			case ORE_SORTER -> ORE_SORTER;
			case XRT_SORTER -> XRT_SORTER;
			case SORTER_COMPRESSOR -> WildcatterSounds.MOTOR_HUM;
			case BLOWER, VENT_FAN, BOOSTER_FAN, FLUID_BED_DRYER, CONCENTRATE_DRYER, FLASH_FURNACE, COPPER_CONVERTER, ANODE_FURNACE -> WildcatterSounds.FAN;
			case ANODE_CASTING_WHEEL, REFINERY_TANKHOUSE -> WildcatterSounds.MOTOR_HUM;
			case SPIRAL_BANK, SCAVENGER_SPIRAL_BANK, CLEANER_BANK, CCD_THICKENER -> SLURRY_RUSH;
			case RER_SEPARATOR, HT_ROLL_SEPARATOR, IRM_SEPARATOR -> WildcatterSounds.MOTOR_HUM;
			case SCAVENGER_BANK, LEACH_TANK, BYPRODUCT_PLANT -> AGITATOR;
			case HEAP_PAD -> dev.wildcatter.WildcatterSounds.MOTOR_HUM;
			case REGRIND_MILL -> BALL_MILL;
			case ELECTROWINNING_BANK -> dev.wildcatter.WildcatterSounds.MOTOR_HUM;
			case THICKENER, TAILINGS_THICKENER -> WildcatterSounds.MOTOR_HUM;
			case HOIST_HOUSE, CAGE_WINDER -> MINE_HOIST;
			case RAISE_BORER -> WildcatterSounds.MOTOR_HUM;
			case PASTE_FILTER, PASTE_PUMP -> WildcatterSounds.MOTOR_HUM;
			case PASTE_MIXER -> AGITATOR;
			case DESLIME_SCREEN, DRAIN_RINSE_SCREEN -> VIBRATING_SCREEN;
			case DMS_MODULE -> SLURRY_RUSH;
			case MEDIUM_RECOVERY -> WildcatterSounds.MOTOR_HUM;
			case GYRATORY_CRUSHER -> GYRATORY_CRUSHER;
			case ROCK_BREAKER -> ROCK_BREAKER;
			case PRIMARY_APRON_FEEDER, RECLAIM_FEEDER -> APRON_FEEDER;
			case HEAVY_CONVEYOR_DRIVE -> BELT_CONVEYOR;
			case SAG_MILL -> SAG_MILL;
			case BALL_MILL_LARGE -> LARGE_BALL_MILL;
			case PEBBLE_CRUSHER -> CONE_CRUSHER;   // a pebble crusher is a cone crusher
			case CYCLONE_FEED_PUMP -> SLURRY_PUMP;
			case CONDITIONING_TANK_LARGE -> AGITATOR;
			case TANK_CELL -> TANK_CELL;
			case COLUMN_CELL, FLOTATION_BLOWER_LARGE, TAILINGS_THICKENER_LARGE -> WildcatterSounds.MOTOR_HUM;
			default -> null;
		};
	}

	/** Its volume: above 1 reaches further, over 16 m times this. */
	public static float volume(Machine machine) {
		return switch (machine) {
			case BALL_MILL -> 2F;
			case JAW, CONE -> 1.5F;
			case GYRATORY_CRUSHER -> 2.5F;
			case SAG_MILL, BALL_MILL_LARGE -> 4F;
			case PRIMARY_APRON_FEEDER, HOIST_HOUSE, ROCK_BREAKER -> 1.5F;
			case XRT_SORTER, CYCLONE_FEED_PUMP -> 1F;
			case HEAVY_CONVEYOR_DRIVE -> 1F;
			case FLOTATION_BANK, TANK_CELL -> 1.2F;
			case FLOTATION_BLOWER_LARGE -> 1.5F;
			case SCREEN, APRON_FEEDER -> 1F;
			case CONVEYOR_DRIVE, CONDITIONER, ACTIVATION_TANK -> 0.6F;
			case THICKENER, TAILINGS_THICKENER -> 0.4F;
			case TAILINGS_FILTER -> 1.2F;
			default -> 0.8F;
		};
	}
}
