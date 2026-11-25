package dev.wildcatter.tailings.fabric.gametest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Films a scene into {@code build/scene-videos/<scene>.mov} (the Gradle run folder's parent).
 * <p>
 * By default it reads the game's own frames, not the screen, so another window over the game is never filmed (fix3-hi04): at the
 * start of each client tick (a client game test renders one frame a tick) the main render target, which still holds the frame just
 * drawn with its HUD and card, is copied on the GPU into one of a few reused read-back buffers ({@code copyTextureToBuffer}, as
 * {@code Screenshot} does); when the copy is done the buffer's bytes go to a writer thread, which pipes them as raw RGBA to ffmpeg
 * ({@code -f rawvideo -pix_fmt rgba -s WxH -r 20 -i -}, flipped upright, doubled with nearest-neighbour scaling and written at 60
 * fps in x264, the same size and rate as the screen recordings, which were the 1440 x 810 window doubled on a Retina screen). So
 * the film runs at the game's ticks: one second of film is 20 ticks, whatever the clock. A frame is dropped
 * (and counted) only if all the read-back buffers are still busy or the writer is behind; the frames written, dropped and the real
 * rate are logged at the end.
 * <p>
 * TAILINGS_FILM_SCREEN=1 films the screen as before, with macOS's screencapture of the game window's rectangle. Off the Mac or
 * with no ffmpeg, the scene still runs, unfilmed.
 */
final class Filming {
	/** Frames read a second of game time, frames a second written, the scale up (the screen recordings were the window doubled on a Retina screen). */
	private static final int TICK_FPS = 20, OUT_FPS = 60, SCALE = 2, BUFFERS = 4, QUEUE = 6;
	private static volatile Filming active;
	private static boolean hooked;

	private final Process process;
	private Minecraft client;
	private final String scene;
	// framebuffer read-back (null when filming the screen)
	private final int width, height;
	private final GpuBuffer[] buffers;
	private final boolean[] busy;
	private final BlockingQueue<byte[]> queue;
	private final ArrayDeque<byte[]> spare = new ArrayDeque<>();
	private final Thread writer;
	private volatile boolean closed;
	private long grabbed, written, dropped, startNanos;

	private Filming(Process process, String scene) {
		this(process, scene, 0, 0, null);
	}

	private Filming(Process process, String scene, int width, int height, OutputStream sink) {
		this.process = process;
		this.scene = scene;
		this.width = width;
		this.height = height;
		this.buffers = sink == null ? null : new GpuBuffer[BUFFERS];
		this.busy = new boolean[BUFFERS];
		this.queue = sink == null ? null : new ArrayBlockingQueue<>(QUEUE);
		this.writer = sink == null ? null : new Thread(() -> write(sink), "tailings-filming-" + scene);
	}

	static Filming start(Minecraft client, String scene) {
		if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
			return new Filming(null, scene);
		}
		try {
			File folder = new File("../../scene-videos").getCanonicalFile();
			folder.mkdirs();
			File out = new File(folder, scene + ".mov");
			out.delete();
			String screen = System.getenv("TAILINGS_FILM_SCREEN");
			if (screen != null && !screen.isBlank()) {
				var window = client.getWindow();
				String rect = window.getX() + "," + window.getY() + "," + window.getScreenWidth() + "," + window.getScreenHeight();
				Process process = new ProcessBuilder("screencapture", "-x", "-v", "-R", rect, out.getPath()).redirectErrorStream(true)
						.redirectOutput(new File(folder, scene + ".log")).start();
				System.out.println("[tailings-test] filming the screen: " + scene + " at " + rect + " to " + out);
				return new Filming(process, scene);
			}
			RenderTarget target = client.gameRenderer.mainRenderTarget();
			int w = target.width, h = target.height;
			Process process = new ProcessBuilder(ffmpeg(), "-hide_banner", "-loglevel", "error", "-y",
					"-f", "rawvideo", "-pix_fmt", "rgba", "-s", w + "x" + h, "-r", Integer.toString(TICK_FPS), "-i", "-",
					"-vf", "vflip,scale=" + SCALE + "*iw:" + SCALE + "*ih:flags=neighbor,fps=" + OUT_FPS, "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-pix_fmt", "yuv420p",
					out.getPath()).redirectErrorStream(true).redirectOutput(new File(folder, scene + ".log")).start();
			Filming film = new Filming(process, scene, w, h, new BufferedOutputStream(process.getOutputStream(), 1 << 20));
			film.client = client;
			if (!hooked) {
				hooked = true;
				ClientTickEvents.START_CLIENT_TICK.register(Filming::tick);
			}
			film.startNanos = System.nanoTime();
			film.writer.start();
			active = film;
			System.out.println("[tailings-test] filming the game's frames: " + scene + " at " + w + "x" + h + ", " + TICK_FPS
					+ " frames a second of game time, to " + out);
			return film;
		} catch (IOException ex) {
			System.out.println("[tailings-test] cannot film " + scene + ": " + ex.getMessage());
			return new Filming(null, scene);
		}
	}

	private static String ffmpeg() {
		for (String path : new String[] {"/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg"}) {
			if (new File(path).canExecute()) {
				return path;
			}
		}
		return "ffmpeg";
	}

	/** On the client thread, at the start of each tick: the frame just drawn, copied on the GPU into a free read-back buffer. */
	private static void tick(Minecraft client) {
		Filming film = active;
		if (film != null && !film.closed) {
			film.grab(client);
		}
	}

	private void grab(Minecraft client) {
		RenderTarget target = client.gameRenderer.mainRenderTarget();
		GpuTexture texture = target.getColorTexture();
		if (texture == null || target.width != width || target.height != height) {
			dropped++;
			return;
		}
		int free = -1;
		for (int i = 0; i < BUFFERS; i++) {
			if (!busy[i]) {
				free = i;
				break;
			}
		}
		if (free < 0) {
			dropped++;
			return;
		}
		int i = free;
		if (buffers[i] == null) {
			// MAP_READ | COPY_DST, as Screenshot's
			buffers[i] = RenderSystem.getDevice().createBuffer(() -> "Tailings filming buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
					(long) width * height * texture.getFormat().blockSize());
		}
		busy[i] = true;
		grabbed++;
		GpuBuffer buffer = buffers[i];
		RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, buffer, 0L, () -> {
			try {
				if (!closed) {
					byte[] frame;
					synchronized (spare) {
						frame = spare.pollFirst();
					}
					if (frame == null) {
						frame = new byte[width * height * 4];
					}
					try (GpuBufferSlice.MappedView view = buffer.map(true, false)) {
						view.data().get(0, frame, 0, frame.length);
					}
					if (!queue.offer(frame)) {
						dropped++;   // the writer is behind
					}
				}
			} finally {
				busy[i] = false;
				if (closed) {
					buffer.close();
					buffers[i] = null;
				}
			}
		}, 0);
	}

	private static final byte[] END = new byte[0];

	private void write(OutputStream sink) {
		try (sink) {
			while (true) {
				byte[] frame = queue.take();
				if (frame == END) {
					break;
				}
				sink.write(frame);
				written++;
				synchronized (spare) {
					if (spare.size() < QUEUE) {
						spare.addLast(frame);
					}
				}
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println("[tailings-test] filming " + scene + ": the encoder stopped: " + ex.getMessage());
		}
	}

	/** Stops the recording and writes the file. */
	void stop() {
		if (process == null) {
			return;
		}
		if (writer == null) {
			stopScreen();
			return;
		}
		closed = true;
		active = null;
		try {
			// any copy still on the GPU is let go (the films end on a still); its buffer is closed when it comes back
			queue.put(END);
			writer.join(60_000);
			if (!process.waitFor(120, TimeUnit.SECONDS)) {
				process.destroy();
			}
			double clock = (System.nanoTime() - startNanos) / 1e9;
			System.out.println(String.format(Locale.ROOT,
					"[tailings-test] filmed %s: %d frames written (%.1f s of game time), %d dropped, over %.1f s by the clock: %.1f frames a second",
					scene, written, written / (double) TICK_FPS, dropped, clock, written / clock));
		} catch (InterruptedException ex) {
			System.out.println("[tailings-test] filming did not stop cleanly: " + ex.getMessage());
		}
		client.execute(() -> {
			for (int i = 0; i < BUFFERS; i++) {
				if (buffers[i] != null && !busy[i]) {
					buffers[i].close();
					buffers[i] = null;
				}
			}
		});
	}

	/** Any character on screencapture's input stops the recording and writes the file. */
	private void stopScreen() {
		try {
			OutputStream in = process.getOutputStream();
			in.write('\n');
			in.flush();
			if (!process.waitFor(15, TimeUnit.SECONDS)) {
				new ProcessBuilder("kill", "-INT", Long.toString(process.pid())).start().waitFor();
				process.waitFor(15, TimeUnit.SECONDS);
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println("[tailings-test] filming did not stop cleanly: " + ex.getMessage());
		}
	}
}
