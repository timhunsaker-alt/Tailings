package dev.wildcatter.tailings.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.item.PowerMeterItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.Tailings;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Tailings part at true size on Wildcatter's big parts (docs/multiblocks/README.md): one item filling its real size, the ghost
 * outline, pieces that break together. Two things Wildcatter keeps to its own namespace are done here: the collision shapes, read
 * from {@code assets/tailings/shapes/<id>.json} as the model tool writes them, and a click on any piece forwarded to the main cell.
 */
public abstract class TailingsPartBlock extends BigPartBlock implements WorldlyContainerHolder {
	private static final Map<String, VoxelShape[][]> SHAPES = new HashMap<>();

	private final String id;

	protected TailingsPartBlock(Properties properties, Footprint footprint, String id) {
		// Wildcatter caches shapes by name alone, so a Tailings name never matches one of its own
		super(properties, footprint, "tailings_" + id);
		this.id = id;
	}

	public String id() {
		return id;
	}

	/** The west face of the main cell, turned with the part: where every Tailings part's motor box is. */
	public static Direction boxFace(BlockState state) {
		return state.getValue(FACING).getCounterClockWise();
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		InteractionResult read = PowerMeterItem.readIfMeter(stack, level, main(pos, state), player);
		return read != null ? read : InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof dev.wildcatter.energy.Inspectable machine) {
			player.sendOverlayMessage(machine.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	/** A hopper on top of the part, or under it, works its main cell's slots. */
	@Override
	public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
		return level instanceof Level l && l.getBlockEntity(main(pos, state)) instanceof WorldlyContainer container ? container : null;
	}

	// ---- collision, from the model tool's shape file ------------------------------------------------------------------------
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape[][] shapes;
		synchronized (SHAPES) {
			shapes = SHAPES.computeIfAbsent(id, name -> load(name, footprint().pieces()));
		}
		VoxelShape[] turned = shapes[Math.floorMod(BigParts.turns(state), 4)];
		return turned[Math.min(state.getValue(piece()), turned.length - 1)];
	}

	/** Reads the shape file: per piece, a list of [x1, y1, z1, x2, y2, z2] boxes in sixteenths, facing north. */
	static VoxelShape[][] load(String name, int pieces) {
		VoxelShape[][] out = new VoxelShape[4][pieces];
		List<List<double[]>> boxes = new ArrayList<>();
		String path = "/assets/" + Tailings.MOD_ID + "/shapes/" + name + ".json";
		try (InputStream in = TailingsPartBlock.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			JsonArray list = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("pieces");
			for (int piece = 0; piece < pieces; piece++) {
				List<double[]> mine = new ArrayList<>();
				if (piece < list.size()) {
					for (JsonElement box : list.get(piece).getAsJsonArray()) {
						JsonArray b = box.getAsJsonArray();
						mine.add(new double[] {b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble(),
								b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble()});
					}
				}
				boxes.add(mine);
			}
		} catch (Exception e) {
			Tailings.LOGGER.warn("No collision shape for {} ({}); its pieces are solid", name, e.getMessage());
			for (VoxelShape[] turn : out) {
				Arrays.fill(turn, Shapes.block());
			}
			return out;
		}
		for (int t = 0; t < 4; t++) {
			for (int piece = 0; piece < pieces; piece++) {
				VoxelShape shape = Shapes.empty();
				for (double[] b : boxes.get(piece)) {
					double[] r = turn(b, t);
					shape = Shapes.or(shape, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
				}
				out[t][piece] = shape.isEmpty() ? Shapes.empty() : shape.optimize();
			}
		}
		return out;
	}

	/** A box in sixteenths turned {@code turns} quarter turns clockwise (seen from above) about the cell's centre, as Wildcatter turns its own. */
	static double[] turn(double[] b, int turns) {
		double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
		for (int i = 0; i < turns; i++) {
			double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
			x1 = nx1;
			x2 = nx2;
			z1 = nz1;
			z2 = nz2;
		}
		return new double[] {x1, b[1], z1, x2, b[4], z2};
	}
}
