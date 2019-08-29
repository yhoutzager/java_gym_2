package javagym;

import java.util.ArrayList;
import java.util.List;

public class YourStrategy extends MineSweeper {

	YourStrategy(int width, int height) {
		super(width, height);
	}

	List<Point> mines = new ArrayList<>();
	List<Point> safePoints = new ArrayList<>();


	@Override
	int[] pickSquare() {
		// Implement your algorithm to select an x and an y. The first square will have coordinates (1, 1)
		// The x and y will define which square you have selected.
		// The minesweeper board you're playing with is the array "display". It is a 2d String array
		// It is not allowed to use the 2d array "field" since this is what the game uses to find the mines.
		// '?' is undiscovered and otherwise it will have numbers indicating the standard minesweeper gameplay.

		// First turn
		if (unknown.equals(display[fieldWidth / 2][fieldHeight / 2])) {
			return new int[]{fieldWidth / 2, fieldHeight / 2};
		}

		// If previous calculations found safe points return those first.
		while (!safePoints.isEmpty()) {
			Point safePoint = safePoints.remove(safePoints.size() - 1);
			if (!unknown.equals(node(safePoint))) {
				continue;
			}
			return safePoint.returnValue();
		}

		{
			boolean allPointsChecked = false;
			while (!allPointsChecked) {
				while (!safePoints.isEmpty()) {
					Point safePoint = safePoints.remove(safePoints.size() - 1);
					if (!unknown.equals(node(safePoint))) {
						continue;
					}
					return safePoint.returnValue();
				}

				allPointsChecked = checkPoints();
			}
		}

		Islands islands = new Islands();
		for (int y = 0; y < fieldHeight; y++) {
			for (int x = 0; x < fieldWidth; x++) {
				Point point = new Point(x, y);
				if (unknown.equals(node(point)) || determineMineNumber(point) == 0) {
					continue;
				}
				if (islands.containsKnown(point)) {
					continue;
				}

				Island island = processIsland(point);
				if (island.knowns.size() == 0) {
					continue;
				}
				islands.add(island);

				solveLinearEquations(island);

				while (!safePoints.isEmpty()) {
					Point safePoint = safePoints.remove(safePoints.size() - 1);
					if (!unknown.equals(node(safePoint))) {
						continue;
					}
					return safePoint.returnValue();
				}
			}
		}

		//TODO YH: Algorithme for guessing. Checken islands for knowns unknown ratio thingy.
		List<Point> guessPoints = new ArrayList<>();
		for (Island island : islands) {
			guessPoints.addAll(island.unknowns);
		}
		if (guessPoints.size() != 0) {
			return guessPoints.get(0).returnValue();
		}
		for (int y = 0; y < fieldHeight; y++) {
			for (int x = 0; x < fieldWidth; x++) {
				if (unknown.equals(node(x, y)) && !mines.contains(new Point(x, y))) {
					return (new Point(x, y)).returnValue();
				}
			}
		}
		throw new IllegalStateException();
	}

	private boolean checkPoints() {

		for (int y = 0; y < fieldHeight; y++) {
			for (int x = 0; x < fieldWidth; x++) {
				if (unknown.equals(node(x, y))) {
					continue;
				}
				Point point = new Point(x,y);
				List<Point> surroundingPoints = surroundingUnknownPoints(point);
				if (surroundingPoints.isEmpty()) {
					continue;
				}
				int mineNumber = determineMineNumber(point);
				int unknowns = 0;

				for (Point surroundingPoint : surroundingPoints) {
					if (mines.contains(surroundingPoint)) {
						mineNumber--;
					} else {
						unknowns++;
					}
				}
				surroundingPoints.removeAll(mines);

				if (mineNumber < 0) {
					throw new IllegalArgumentException();
				} else if (mineNumber == 0 && unknowns > 0) {
					safePoints.addAll(surroundingPoints);
					return false;
				} else if (mineNumber != 0 && mineNumber == unknowns) {
					mines.addAll(surroundingPoints);
					x = Math.max(x - 2, 0);
					y = Math.max(y - 2, 0);
				}
			}
		}

		return true;
	}

	private List<Point> surroundingUnknownPoints(Point point) {
		List<Point> points = new ArrayList<>();

		for (int yOffset = -1; yOffset <= 1; yOffset++) {
			for (int xOffset = -1; xOffset <= 1; xOffset++) {
				if (yOffset == 0 && xOffset == 0) {
					continue;
				}

				int x = point.x + xOffset;
				int y = point.y + yOffset;

				if (x < 0 || x >= fieldWidth
						|| y < 0 || y >= fieldHeight
						|| !unknown.equals(node(x, y))) {
					continue;
				}

				points.add(new Point(x, y));
			}
		}

		return points;
	}

	private void solveLinearEquations(Island island) {
		int[] b = new int[island.knowns.size()];
		int[][] A = new int[island.knowns.size()][island.unknowns.size()];

		for (int i = 0; i < b.length; i++) {
			Point point = island.knowns.get(i);
			int mineNumber = determineMineNumber(point);
			for (int yOffset = -1; yOffset <= 1; yOffset++) {
				for (int xOffset = -1; xOffset <= 1; xOffset++) {
					if (yOffset == 0 && xOffset == 0) {
						continue;
					}
					int x = point.x + xOffset;
					int y = point.y + yOffset;

					Point checkPoint = new Point(x,y);
					if (mines.contains(checkPoint)) {
						mineNumber--;
						continue;
					}

					if (unknown.equals(node(checkPoint))) {
						int index = island.unknowns.indexOf(checkPoint);
						if (index == -1) {
							throw new IllegalArgumentException();
						}
						A[i][index] = 1;
					}
				}
			}
			b[i] = mineNumber;
		}

//		System.out.print("\nb = Ax");
//		for (int i = 0; i < b.length; i++) {
//			System.out.print("\n" + b[i] + " | ");
//			for (int j = 0; j < A[0].length; j++) {
//				System.out.print(A[i][j] + " ");
//			}
//		}
//		System.out.println("\n");


		GaussianElimination gaussianElimination = new GaussianElimination(A, b);
		Integer[] solution = gaussianElimination.getSolution();

		for (int i = 0; i < solution.length; i++) {
			if (Integer.valueOf(0).equals(solution[i])) {
				safePoints.add(island.unknowns.get(i));
			} else if (Integer.valueOf(1).equals(solution[i])) {
				mines.add(island.unknowns.get(i));
			}
		}
	}

	private Island processIsland(Point startPoint) {
		List<Point> unknowns = new ArrayList<>();
		List<Point> knowns = new ArrayList<>();
		processSurroundingBlocks(startPoint, knowns, unknowns, false);
		return new Island(knowns, unknowns);
	}

	private void processSurroundingBlocks(Point point, List<Point> knowns, List<Point> unknowns, boolean isForKnown) {
		List<Point> newPoints = new ArrayList<>();
		for (int yOffset = -1; yOffset <= 1; yOffset++) {
			for (int xOffset = -1; xOffset <= 1; xOffset++) {

				int x = point.x + xOffset;
				int y = point.y + yOffset;
				if (x < 0 || x >= fieldWidth
						|| y < 0 || y >= fieldHeight) {
					continue;
				}

				Point newPoint = new Point(point.x + xOffset,point.y + yOffset);


				if (isForKnown && !unknown.equals(node(newPoint)) && determineMineNumber(newPoint) != 0) {
					if (!knowns.contains(newPoint)) {
						newPoints.add(newPoint);
						knowns.add(newPoint);
					}
				}
				if (!isForKnown && unknown.equals(node(newPoint)) && !mines.contains(newPoint)) {
					if (!unknowns.contains(newPoint)) {
						newPoints.add(newPoint);
						unknowns.add(newPoint);
					}
				}
			}
		}
		for (Point newPoint : newPoints) {
			processSurroundingBlocks(newPoint, knowns, unknowns, !isForKnown);
		}
	}

	private String node(int x, int y) {
		return display[x + 1][y + 1];
	}

	private String node(Point point) {
		return node(point.x, point.y);
	}

	private int determineMineNumber(Point point) {
		String neighbourCountString = node(point);
		for (int i = 0; i < neighbourCount.length; i ++) {
			if (neighbourCount[i].equals(neighbourCountString)) {
				return i;
			}
		}
		throw new IllegalArgumentException();
	}

	private class Point {
		int x;
		int y;
		Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof Point) {
				return x == ((Point) obj).x
						&& y == ((Point) obj).y;
			}
			return false;
		}

		public int[] returnValue() {
			return new int[]{x + 1, y + 1};
		}

		@Override
		public String toString() {
			return "(" + x +"," + y + ")";
		}
	}

	private class Islands extends ArrayList<Island> {

		Islands() {
			super();
		}

		public boolean containsKnown(Point point) {
			// TODO: maybe stream.
			for (Island island : this) {
				for (Point comparePoint : island.knowns) {
					if (point.equals(comparePoint)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private class Island {
		List<Point> knowns;
		List<Point> unknowns;

		Island(List<Point> knowns ,List<Point> unknowns) {
			this.knowns = knowns;
			this.unknowns = unknowns;
		}
	}

	@Override
	void print() {

	}

	@Override
	protected void printGame(String[][] str) {
		System.out.print(" |");
		for(int i = 0; i < str[0].length; i++) {
			System.out.print(" " + i + " |");
		}
		for(int y = 1; y < str[0].length - 1; y++) {
			System.out.print("\n" + (y-1));
			for(int x = 1; x < str.length ; x++) {
				System.out.print("|");
				// Prints out content of each tile.
				if (mines.contains(new Point(x-1,y-1))) {
					System.out.print(" * ");
				} else {
					System.out.print(str[x][y]);
				}
			}
		}
		System.out.println("");
	}
}
