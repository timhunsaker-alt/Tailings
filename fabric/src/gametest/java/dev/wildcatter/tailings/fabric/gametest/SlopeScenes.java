package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineRopes;
import dev.wildcatter.tailings.pit.Slopes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The dragline by hand and its slopes failing (docs/multiblocks/dragline-hand-control.md), filmed for the trailer in real time at
 * 1920 x 1080 with a clean view (no hotbar, no chat; the HUD only where the cab's readout is the point): TAILINGS_ONLY=dragline_hand,
 * highwall_slide, spoil_slide, dragline_wrecked. Each film holds still 3 s at its start and end for captions. Films to
 * {@code fabric/build/scene-videos/<scene>.mov}; each film's events are logged at their second in it ("[tailings-film]").
 */
final class SlopeScenes {
	private static final int W = 1920, H = 1080;
	/** Held still at each film's start and end, ticks, for captions. */
	private static final int PAD = 60;

	private final ClientGameTestContext context;
	private final TestServerContext server;
	private final int y;
	private int ft;
	private long filmStart;
	private final List<String> events = new ArrayList<>();
	private final List<String> summary = new ArrayList<>();

	SlopeScenes(ClientGameTestContext context, TestServerContext server, int y) {
		this.context = context;
		this.server = server;
		this.y = y;
	}

	private static void log(String scene, String line) {
		System.out.println("[tailings-test] " + scene + ": " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[tailings-test] " + what);
		}
	}

	private static void fill(ServerLevel level, BlockPos o, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int yy = y0; yy <= y1; yy++) {
					level.setBlock(o.offset(x, yy, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Clears a site and lays a stone floor at its y 0. */
	private static void site(ServerLevel level, BlockPos o) {
		fill(level, o, Blocks.AIR, -40, 110, 1, 60, -50, 90);
		fill(level, o, Blocks.STONE, -40, 110, -2, 0, -50, 90);
	}

	void run(boolean hand, boolean slide, boolean spoil, boolean wrecked) {
		context.runOnClient(client -> {
			client.options.particles().set(ParticleStatus.ALL);
			client.options.renderDistance().set(12);
			client.options.entityDistanceScaling().set(3.0);
			client.options.bobView().set(false);
			client.options.fov().set(70);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		context.getInput().resizeWindow(W, H);
		context.waitTicks(10);
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamerule advance_time false");
		server.runCommand("gamerule advance_weather false");
		server.runCommand("gamemode spectator @p");
		try {
			if (hand) {
				hand();
			}
			server.runCommand("tailings rule slope_failures true");
			if (slide) {
				highwallSlide();
			}
			if (spoil) {
				spoilSlide();
			}
			if (wrecked) {
				wrecked();
			}
		} finally {
			server.runCommand("tailings rule slope_failures false");
			server.runCommand("wildcatter rule timeScale 1");
			server.runCommand("gamerule advance_time true");
			hud(true);
			System.out.println("[tailings-film] summary:\n  " + String.join("\n  ", summary));
		}
	}

	private void hud(boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
	}

	// ---- camera and films ----

	private void put(Cam c) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y(), dz = c.tz() - c.z();
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y() - 1.62, c.z(), yaw, pitch));
	}

	private void settle(int ticks) {
		context.waitTicks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 400);
		} catch (RuntimeException | AssertionError ex) {
			System.out.println("[tailings-film] sections still compiling: " + ex.getMessage());
		}
		context.waitTicks(5);
	}

	/** Something seen in the film running now, at its second (by ticks, and by the wall clock). */
	private void event(String what) {
		events.add(String.format(Locale.ROOT, "%.1f s (clock %.1f s): %s", ft / 20.0, (System.nanoTime() - filmStart) / 1e9, what));
	}

	/**
	 * A film: the camera where {@code cam} puts it each tick, {@code each} run every tick, until {@code done} says it is over (then
	 * {@link #PAD} more ticks held still), or {@code most} ticks.
	 */
	private void film(String id, int most, IntFunction<Cam> cam, IntConsumer each, java.util.function.BooleanSupplier done) {
		put(cam.apply(0));
		settle(40);
		events.clear();
		context.takeScreenshot("tailings-" + id + "-first");
		Filming film = context.computeOnClient(client -> Filming.start(client, id));
		filmStart = System.nanoTime();
		int endAt = -1;
		for (ft = 0; ft <= most && (endAt < 0 || ft <= endAt); ft++) {
			put(cam.apply(ft));
			if (each != null) {
				each.accept(ft);
			}
			context.waitTick();
			if (endAt < 0 && done.getAsBoolean()) {
				endAt = ft + PAD;
				event("over; held still for the end caption");
			}
		}
		double clock = (System.nanoTime() - filmStart) / 1e9;
		film.stop();
		context.takeScreenshot("tailings-" + id + "-last");
		String line = String.format(Locale.ROOT, "%s: %.1f s by ticks, %.1f s by the clock; %s", id, ft / 20.0, clock, events);
		System.out.println("[tailings-film] " + line);
		summary.add(line);
	}

	private static Input keys(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sprint) {
		return new Input(forward, back, left, right, jump, false, sprint);
	}

	// ---- dragline_hand ----

	private record Pose(double swing, double rate, double out, double up, double ground, long casts, String readout, double tons, Vec3 eye, Vec3 bucket, boolean hand) {
	}

	/**
	 * dragline_hand, from the cab: the operator takes the levers, lowers the bucket onto the bench ahead, drags it full through the bank,
	 * hoists it clear, swings right and lets the house coast onto a haul truck, and pays out the drag to dump into its body. Real time,
	 * the machine's own rates; the cab's readout on the action bar.
	 */
	private void hand() {
		BlockPos o = new BlockPos(2200, y + 4, -800);
		UUID[] name = new UUID[1];
		UUID[] truckId = new UUID[1];
		double truckSwing = 38, truckOut = 34;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.DIRT, -40, 110, 1, 9, -50, 90);
			fill(level, o, Blocks.GRASS_BLOCK, -40, 110, 10, 10, -50, 90);
			BlockPos hs = o.offset(-10, 11, -30);
			level.setBlock(hs, TailingsBlocks.PIT_SWITCH_HOUSE.defaultBlockState().setValue(dev.wildcatter.block.BigPartBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
			dev.wildcatter.multiblock.BigParts.grow(level, hs, level.getBlockState(hs));
			PitSwitchHouseBlockEntity house = (PitSwitchHouseBlockEntity) level.getBlockEntity(hs);
			house.forceSupply(true);
			DraglineEntity d = TailingsEntities.DRAGLINE.create(level, EntitySpawnReason.COMMAND);
			d.setPos(o.getX() + 20.5, o.getY() + 11, o.getZ() + 0.5);
			d.setYRot(0);
			d.job().set(0, 10);
			d.job().set(1, 10);
			d.job().set(2, 2);
			d.job().set(3, 0);
			level.addFreshEntity(d);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE, 4);
			TrailingCableItem.pickUp(reel, house);
			log("dragline_hand", "cable " + TrailingCableItem.couple(level, reel, d) + "; outlet " + house.close(0));
			d.start();
			name[0] = d.getUUID();
			// the truck, parked where the operator will swing the bucket: its body under the bucket's arc
			Vec3 at = d.bucketAt(truckSwing, truckOut, 0);
			HaulTruckEntity truck = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(at.x, o.getY() + 11, at.z);
			truck.setYRot((float) truckSwing);
			level.addFreshEntity(truck);
			truckId[0] = truck.getUUID();
		});
		context.waitTicks(10);
		// seated, a key touched: HAND; the bucket hung empty 45 m ahead, 6 m up, over uncut bank
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
				d.testSeat(true);
				d.testDrive(keys(false, false, false, false, true, false));
			}
		});
		context.waitTicks(2);
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
				d.testDrive(Input.EMPTY);
				d.emptyBucket();
				d.handPose(0, 45, 6);
			}
		});
		hud(true);
		context.runOnClient(client -> client.options.guiScale().set(3));
		Vec3[] look = {null};
		Pose[] pose = new Pose[1];
		java.util.function.Supplier<Pose> read = () -> server.computeOnServer(srv -> {
			if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
				return new Pose(d.swing(), d.swingRate(), d.bucketOut(), d.bucketUp(), 0, d.handCasts(), d.handReadoutText(), d.load().total() / 1000.0, d.cabEye(),
						d.bucketAt(d.swing(), d.bucketOut(), d.bucketUp()), d.hand());
			}
			return null;
		});
		pose[0] = read.get();
		String[] phase = {"pre"};
		int[] phaseAt = {0};
		IntFunction<Cam> cam = t -> {
			Pose p = pose[0];
			Vec3 target = p.bucket().add(0, -2, 0);
			look[0] = look[0] == null ? target : look[0].lerp(target, 0.12);
			Vec3 e = p.eye();
			return new Cam(e.x, e.y, e.z, look[0].x, look[0].y, look[0].z);
		};
		IntConsumer each = t -> {
			Pose p = pose[0];
			Input k = Input.EMPTY;
			String next = phase[0];
			switch (phase[0]) {
				case "pre" -> next = t >= PAD ? "lower" : "pre";
				case "lower" -> {
					k = keys(false, false, false, false, false, true);
					if (p.up() <= 0.05 && t - phaseAt[0] > 20) {
						next = "drag";
					}
				}
				case "drag" -> {
					k = keys(true, false, false, false, false, false);
					if (p.tons() >= 39.9 || t - phaseAt[0] > 500) {
						next = "hoist";
					}
				}
				case "hoist" -> {
					k = keys(false, false, false, false, true, false);
					if (p.up() >= 11) {
						next = "swing";
					}
				}
				case "swing" -> {
					// hold right until the house would coast onto the truck, then let go: the operator's braking point
					double stop = p.swing() + p.rate() * p.rate() / (2 * DraglineRopes.SWING_ACCEL);
					boolean pull = p.out() > truckOut + 0.5;
					k = keys(pull, false, false, stop < truckSwing, false, false);
					if (stop >= truckSwing) {
						next = "coast";
					}
				}
				case "coast" -> {
					boolean pull = p.out() > truckOut + 0.5;
					k = keys(pull, false, false, false, false, false);
					if (p.rate() == 0 && !pull) {
						next = "dump";
					}
				}
				case "dump" -> {
					k = keys(false, true, false, false, false, false);
					if (p.casts() >= 1) {
						next = "done";
					}
				}
				default -> k = Input.EMPTY;
			}
			if (!next.equals(phase[0])) {
				event(phase[0] + " -> " + next + String.format(Locale.ROOT, " (swing %.0f, %.0f m out, %+.0f m, %.0f t)", p.swing(), p.out(), p.up(), p.tons()));
				phase[0] = next;
				phaseAt[0] = t;
			}
			Input keysNow = k;
			pose[0] = server.computeOnServer(srv -> {
				if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
					d.testDrive(keysNow);
					if (srv.getPlayerList().getPlayers().size() > 0 && !d.handReadoutText().isEmpty()) {
						srv.getPlayerList().getPlayers().get(0).sendOverlayMessage(Component.literal(d.handReadoutText()));
					}
					return new Pose(d.swing(), d.swingRate(), d.bucketOut(), d.bucketUp(), 0, d.handCasts(), d.handReadoutText(), d.load().total() / 1000.0, d.cabEye(),
							d.bucketAt(d.swing(), d.bucketOut(), d.bucketUp()), d.hand());
				}
				return pose[0];
			});
		};
		film("dragline_hand", 3600, cam, each, () -> phase[0].equals("done"));
		double truckTons = server.computeOnServer(srv -> srv.overworld().getEntity(truckId[0]) instanceof HaulTruckEntity t ? (Double) (t.load().total() / 1000.0) : (Double) (-1.0));
		server.runOnServer(srv -> {
			if (srv.overworld().getEntity(name[0]) instanceof DraglineEntity d) {
				d.testSeat(false);
				d.testDrive(Input.EMPTY);
			}
		});
		context.runOnClient(client -> client.options.guiScale().set(0));
		log("dragline_hand", "casts " + pose[0].casts() + "; the truck holds " + truckTons + " t");
		require(pose[0].casts() == 1 && truckTons > 20, "one bucket cast by hand into the truck: " + pose[0].casts() + ", " + truckTons + " t");
	}

	// ---- the slopes ----

	/** A strip in the slope record, from a dragline centre at o + (20.5, crest, 0.5) facing +z, the strip to +x from x 33. */
	private static long strip(ServerLevel level, BlockPos o, int crest, int width, int batter) {
		return Slopes.register(level, UUID.randomUUID(), o.getX() + 20.5, o.getZ() + 0.5, 0, 1, 1, 0, o.getY() + crest, width, 10, batter);
	}

	/**
	 * highwall_slide: a 21 m dirt highwall laid back 2 in 1 (FS 0.81, FAILING), three prisms on its bench and a survey station behind,
	 * at a time scale of 20: cracks open along the bench as its clock runs, the slab between the crack and the crest moves toward the
	 * face three times with its prisms, then the wall runs into the cut.
	 */
	private void highwallSlide() {
		BlockPos o = new BlockPos(2200, y + 4, -500);
		long[] id = new long[1];
		int crest = 22;
		server.runCommand("wildcatter rule timeScale 20");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.DIRT, -40, 32, 1, crest - 2, -30, 40);
			fill(level, o, Blocks.GRASS_BLOCK, -40, 32, crest - 1, crest - 1, -30, 40);
			for (int d = 1; d <= crest - 1; d++) {
				int off = Dragline.batterOffset(Dragline.BATTER_2_IN_1, d);
				if (off > 0) {
					fill(level, o, Blocks.DIRT, 33, 32 + off, crest - d, crest - d, -30, 40);
				}
			}
			for (int z : new int[] {3, 6, 9}) {
				level.setBlock(o.offset(29, crest, z), TailingsBlocks.SLOPE_PRISM.defaultBlockState(), Block.UPDATE_ALL);
			}
			level.setBlock(o.offset(8, crest, 16), TailingsBlocks.SURVEY_STATION.defaultBlockState(), Block.UPDATE_ALL);
			id[0] = strip(level, o, crest, 30, Dragline.BATTER_2_IN_1);
		});
		hud(false);
		Cam c = Cam.of(o, 62, crest + 9, -22, 30, crest - 6, 6);
		int[] cracks = {0}, steps = {0};
		String[] stage = {"STABLE"};
		film("highwall_slide", 1400, t -> c, t -> {
			if (t == PAD) {
				server.runOnServer(srv -> Slopes.checkNow(srv.overworld(), id[0], 0));
			}
			String[] now = server.computeOnServer(srv -> {
				Slopes.Part p = Slopes.strip(srv, id[0]).section(0).wall();
				return new String[] {p.stage().name(), Integer.toString(p.cracks()), Integer.toString(p.steps()), String.format(Locale.ROOT, "%.2f", p.fs())};
			});
			if (!now[0].equals(stage[0]) && !stage[0].equals("SLID")) {
				event(now[0] + " (FS " + now[3] + ")");
				stage[0] = now[0];
			}
			int k = Integer.parseInt(now[1]), s = Integer.parseInt(now[2]);
			if (k > cracks[0]) {
				event("cracks on the bench: " + k);
				cracks[0] = k;
			}
			if (s > steps[0]) {
				event("the slab and its prisms move a metre toward the face (" + s + ")");
				steps[0] = s;
			}
		}, () -> stage[0].equals("SLID") && server.computeOnServer(srv -> Slopes.pending(srv.overworld())) == 0);
		server.runCommand("wildcatter rule timeScale 1");
		log("highwall_slide", "stage " + stage[0] + "; steps " + steps[0]);
		require(stage[0].equals("SLID"), "the highwall should slide: " + stage[0]);
	}

	/** spoil_slide: an 18 m spoil pile at 37 degrees on a clay floor, a pond at its toe: it slumps into the pond to about 1 in 2. */
	private void spoilSlide() {
		BlockPos o = new BlockPos(2200, y + 4, -200);
		long[] id = new long[1];
		server.runCommand("wildcatter rule timeScale 20");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.CLAY, -40, 110, 1, 1, -50, 90);
			for (int x = 44; x <= 70; x++) {
				int k = Math.min(18, (int) Math.ceil((x - 43) * 0.75));
				fill(level, o, Blocks.DIRT, x, x, 2, 1 + k, -12, 22);
			}
			fill(level, o, Blocks.STONE, 30, 31, 2, 3, -14, 24);
			fill(level, o, Blocks.STONE, 30, 43, 2, 3, -14, -13);
			fill(level, o, Blocks.STONE, 30, 43, 2, 3, 23, 24);
			fill(level, o, Blocks.WATER, 32, 43, 2, 2, -12, 22);
			id[0] = strip(level, o, 12, 10, Dragline.BATTER_VERTICAL);
			Slopes.cast(level, id[0], o.offset(50, 10, 5));
		});
		hud(false);
		Cam c = Cam.of(o, 20, 22, -26, 52, 9, 6);
		String[] stage = {"STABLE"};
		film("spoil_slide", 900, t -> c, t -> {
			if (t == PAD) {
				server.runOnServer(srv -> Slopes.checkNow(srv.overworld(), id[0], 0));
			}
			String now = server.computeOnServer(srv -> Slopes.strip(srv, id[0]).section(0).spoil().stage().name());
			if (!now.equals(stage[0]) && !stage[0].equals("SLID")) {
				event(now);
				stage[0] = now;
			}
		}, () -> stage[0].equals("SLID") && server.computeOnServer(srv -> Slopes.pending(srv.overworld())) == 0);
		server.runCommand("wildcatter rule timeScale 1");
		log("spoil_slide", "stage " + stage[0]);
		require(stage[0].equals("SLID"), "the spoil should slide: " + stage[0]);
	}

	/** dragline_wrecked: a dragline standing on dirt by a 30 m vertical highwall: the wall slides, and the dragline tips into the cut. */
	private void wrecked() {
		BlockPos o = new BlockPos(2200, y + 4, 100);
		UUID[] name = new UUID[1];
		long[] stripId = new long[1];
		server.runCommand("wildcatter rule timeScale 20");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			site(level, o);
			fill(level, o, Blocks.DIRT, -40, 32, 1, 29, -40, 40);
			fill(level, o, Blocks.GRASS_BLOCK, -40, 32, 30, 30, -40, 40);
			DraglineEntity d = TailingsEntities.DRAGLINE.create(level, EntitySpawnReason.COMMAND);
			d.setPos(o.getX() + 20.5, o.getY() + 31, o.getZ() + 0.5);
			d.setYRot(0);
			d.job().set(0, 30);
			d.job().set(1, 10);
			d.job().set(2, 1);
			d.job().set(3, 0);
			level.addFreshEntity(d);
			d.start();
			name[0] = d.getUUID();
			stripId[0] = d.stripId();
		});
		hud(false);
		Cam c = Cam.of(o, 88, 40, -46, 24, 18, 4);
		boolean[] tipping = {false}, gone = {false};
		film("dragline_wrecked", 1200, t -> c, t -> {
			if (t == PAD) {
				server.runOnServer(srv -> Slopes.checkNow(srv.overworld(), stripId[0], 0));
				event("failing: cracks open");
			}
			boolean[] now = server.computeOnServer(srv -> {
				var e = srv.overworld().getEntity(name[0]);
				return new boolean[] {e instanceof DraglineEntity d && d.tipping(), !(e instanceof DraglineEntity)};
			});
			if (now[0] && !tipping[0]) {
				event("the wall slides; the dragline tips into the cut");
				tipping[0] = true;
			}
			if (now[1] && !gone[0]) {
				event("the dragline lands and breaks into its parts");
				gone[0] = true;
			}
		}, () -> gone[0] && server.computeOnServer(srv -> Slopes.pending(srv.overworld())) == 0);
		server.runCommand("wildcatter rule timeScale 1");
		log("dragline_wrecked", "tipped " + tipping[0] + ", gone " + gone[0]);
		require(gone[0] && tipping[0], "the dragline should tip and go down with the slide");
	}
}
