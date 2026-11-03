package dev.wildcatter.tailings.mine;

import java.util.ArrayList;
import java.util.List;

/**
 * One advance of the TBM: probe three blocks ahead, void water, quench lava, cut the 8 x 8, and place the ring and floor.
 * The water it pumps is counted and then forgotten. Quench water comes from the water slot.
 */
public final class TunnelDrill {
	private final DrillProgram program;
	private final DrillStock stock;
	private int advance;
	private int flatDriven;
	private int startFloorY;
	private int originX;
	private int originZ;
	private int pumped;
	private Advance.Stop stopped = Advance.Stop.NONE;
	private String detail = "";

	public TunnelDrill(DrillProgram program, DrillStock stock, int originX, int startFloorY, int originZ) {
		this.program = program;
		this.stock = stock;
		this.originX = originX;
		this.startFloorY = startFloorY;
		this.originZ = originZ;
	}

	public DrillProgram program() {
		return program;
	}

	public DrillStock stock() {
		return stock;
	}

	public int advanceCount() {
		return advance;
	}

	public int flatDriven() {
		return flatDriven;
	}

	public int pumped() {
		return pumped;
	}

	public int originX() {
		return originX;
	}

	public int originZ() {
		return originZ;
	}

	public int startFloorY() {
		return startFloorY;
	}

	public void placeOrigin(int x, int floorY, int z) {
		this.originX = x;
		this.startFloorY = floorY;
		this.originZ = z;
	}

	/** The first slice is one block ahead of the cab, with the cab beside the walkway. */
	public void placeOriginFrom(int cabX, int cabY, int cabZ) {
		Heading heading = program.heading();
		placeOrigin(cabX + heading.forwardX() - heading.acrossX() * 3, cabY,
				cabZ + heading.forwardZ() - heading.acrossZ() * 3);
	}

	public void restore(int advance, int flatDriven, int pumped, Advance.Stop stopped, String detail) {
		this.advance = advance;
		this.flatDriven = flatDriven;
		this.pumped = pumped;
		this.stopped = stopped == null ? Advance.Stop.NONE : stopped;
		this.detail = detail == null ? "" : detail;
	}

	public int floorY() {
		return DriveCourse.floorY(startFloorY, advance, program.grade());
	}

	public Advance.Stop stopped() {
		return stopped;
	}

	public String detail() {
		return detail;
	}

	public void resetStop() {
		stopped = Advance.Stop.NONE;
		detail = "";
	}

	public boolean declining() {
		return DriveCourse.declining(floorY(), program.targetY());
	}

	public int worldX(int across, int ahead) {
		Heading heading = program.heading();
		return originX + heading.forwardX() * ahead + heading.acrossX() * across;
	}

	public int worldZ(int across, int ahead) {
		Heading heading = program.heading();
		return originZ + heading.forwardZ() * ahead + heading.acrossZ() * across;
	}

	/** Voxel Y of local row {@code up}. The probe uses the grade at {@code advance + ahead}, not the face's Y. */
	public int worldY(int up, int ahead) {
		return DriveCourse.floorY(startFloorY, advance + ahead, program.grade()) - 1 + up;
	}

	/** Stone is one block per 20 s. Any deepslate in the face about to be cut slows that advance. */
	public int ticksFor(GroundGrid ground) {
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				if (ground.get(worldX(across, 0), worldY(up, 0), worldZ(across, 0)) == Ground.DEEPSLATE) {
					return DriveCourse.DEEPSLATE_TICKS;
				}
			}
		}
		return DriveCourse.STONE_TICKS;
	}

	/**
	 * Cuts and lines one block, or stops. {@code ahead} 0 is the face about to be cut. The probe looks at 1, 2 and 3.
	 */
	public Advance step(GroundGrid ground) {
		if (stopped != Advance.Stop.NONE) {
			return Advance.stopped(stopped, detail);
		}
		if (!declining() && flatDriven >= program.flatLength()) {
			return halt(Advance.Stop.TARGET_REACHED, "target Y reached");
		}
		if (program.stopOnCable() && advance >= program.cableBlocks()) {
			return halt(Advance.Stop.CABLE, "trailing cable ended");
		}
		boolean decline = declining();
		FloorPiece[] floor = DriveCourse.floor(advance, program.grade(), decline);
		int rockNeed = DriveCourse.rockPieces(floor);
		int liningStone = stock.segments() >= DriveCourse.SEGMENTS_PER_RING ? 0 : DriveCourse.SEGMENTS_PER_RING;
		int holdCap = rockNeed + liningStone;
		List<int[]> liquids = new ArrayList<>();
		boolean sawOre = false;
		boolean blocked = false;
		boolean unknown = false;
		int lava = 0;
		int waterSources = 0;
		int cave = 0;
		int incoming = 0;
		for (int ahead = 0; ahead <= DriveCourse.PROBE_BLOCKS; ahead++) {
			for (int up = 0; up < DriveCourse.FACE; up++) {
				for (int across = 0; across < DriveCourse.FACE; across++) {
					int x = worldX(across, ahead);
					int y = worldY(up, ahead);
					int z = worldZ(across, ahead);
					Ground cell = ground.get(x, y, z);
					if (cell == Ground.ORE && program.stopOnOre()) {
						sawOre = true;
					}
					if (ahead == 0 && cell == Ground.BEDROCK) {
						blocked = true;
					}
					if (cell == Ground.UNKNOWN_LIQUID) {
						unknown = true;
					}
					if (cell.isLiquid()) {
						liquids.add(new int[] {x, y, z, cell.ordinal()});
						if (cell.isLava()) {
							lava++;
						}
						if (cell == Ground.WATER) {
							waterSources++;
						}
					}
					if (ahead == 0 && predicted(cell).isFloorRock() && incoming < holdCap) {
						incoming++;
					}
				}
			}
		}
		for (int across = 1; across <= DriveCourse.CLEAR; across++) {
			int x = worldX(across, 0);
			int y = worldY(0, 0) - 1;
			int z = worldZ(across, 0);
			if (ground.get(x, y, z) == Ground.AIR) {
				cave++;
			}
		}
		if (sawOre && program.stopOnOre()) {
			return halt(Advance.Stop.ORE, "ore ahead");
		}
		if (blocked) {
			return halt(Advance.Stop.BLOCKED, "bedrock");
		}
		if (unknown && program.stopOnLiquid()) {
			return halt(Advance.Stop.LIQUID, "unknown liquid");
		}
		int keptWater = Math.min(waterSources, Math.max(0, DrillStock.TANK_MAX - stock.water()));
		if (lava > 0 && stock.water() + keptWater < lava) {
			return halt(Advance.Stop.LIQUID, "lava ahead and no quench water");
		}
		String missing = stock.missing(floor, cave, lava, incoming, keptWater);
		if (missing != null && program.stopOnEmpty()) {
			return halt(Advance.Stop.EMPTY, "out of " + missing);
		}
		for (int[] cell : liquids) {
			Ground kind = Ground.values()[cell[3]];
			if (kind.isLava()) {
				ground.set(cell[0], cell[1], cell[2], kind == Ground.LAVA ? Ground.OBSIDIAN : Ground.COBBLE);
			} else if (kind.isWater() || kind == Ground.UNKNOWN_LIQUID) {
				ground.set(cell[0], cell[1], cell[2], Ground.AIR);
			}
		}
		List<Ground> held = new ArrayList<>();
		Advance drove = Advance.drove(waterSources, lava);
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				int x = worldX(across, 0);
				int y = worldY(up, 0);
				int z = worldZ(across, 0);
				Ground kind = ground.get(x, y, z);
				if (kind == Ground.AIR || kind == Ground.LINING || kind == Ground.FLOOR) {
					continue;
				}
				String drop = ground.dropId(x, y, z);
				if (kind.isFloorRock() && held.size() < holdCap) {
					held.add(kind);
				} else {
					drove.addMuck(kind, drop, 1);
				}
				ground.set(x, y, z, Ground.AIR);
			}
		}
		stock.acceptWater(waterSources);
		Spent spent = stock.consume(floor, cave, lava, held);
		Ground[] used = spent.floor();
		Ground[] ring = spent.lining();
		int lined = 0;
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				if (DriveCourse.isLining(across, up)) {
					Ground material = ring == null ? null : ring[lined++];
					ground.placeLining(worldX(across, 0), worldY(up, 0), worldZ(across, 0), material);
				}
			}
		}
		for (int i = 0; i < floor.length; i++) {
			int across = i + 1;
			ground.placeFloor(worldX(across, 0), worldY(1, 0), worldZ(across, 0), floor[i], used[i]);
		}
		for (int across = 1; across <= DriveCourse.CLEAR; across++) {
			int x = worldX(across, 0);
			int y = worldY(0, 0) - 1;
			int z = worldZ(across, 0);
			if (ground.get(x, y, z) == Ground.AIR) {
				ground.placeCave(x, y, z);
			}
		}
		drove.recordFloor(floor, used);
		drove.recordLining(ring);
		drove.markPlaced();
		pumped += waterSources;
		if (!decline) {
			flatDriven++;
		}
		advance++;
		originX += program.heading().forwardX();
		originZ += program.heading().forwardZ();
		return drove;
	}

	private Advance halt(Advance.Stop stop, String why) {
		stopped = stop;
		detail = why;
		return Advance.stopped(stop, why);
	}

	/** What a cell will be once lava has been quenched and water voided. */
	private static Ground predicted(Ground cell) {
		return switch (cell) {
			case LAVA -> Ground.OBSIDIAN;
			case FLOWING_LAVA -> Ground.COBBLE;
			case WATER, FLOWING_WATER, UNKNOWN_LIQUID -> Ground.AIR;
			default -> cell;
		};
	}
}
