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



		// Check islands
		// Islands are known points that are connected via neighbouring unknown points.
		// Probably best some recursive path filling a set with points.
		// # of known points is the number of equations
		// # of unknown neighbours is the number of variables. (think about known mines and how this affects this.
		// prob best to check if island gives new results before checking next island.


		// Make equations. Ax = b
		// The array sizes for A and b are known from the island mapping.
		// Think about how the coordinates match up to the equation variables (x1, x2, x3, x4...)

		/*
		https://massaioli.wordpress.com/2013/01/12/solving-minesweeper-with-matricies/comment-page-1/
		0. Check list with safe spaces for easy next point
		1. Make a list with islands
		2. While no clickable found for each island
		2.1. Make a list of fields which might be clicked
		2.2. Make the equations using the potential fields
		2.3 Process equations
		2.4. if answer found abort
		3. if no answer found guess somewhere (think of algorithm).

		Safe known mine locations.
		Safe known safe spaces.
		 */
//		for (Point mine : mines) {
//			System.out.print(mine + "\t");
//		}
//		System.out.println();

		List<Point> guessPoints = new ArrayList<>();
		for (Island island : islands) {
			guessPoints.addAll(island.unknowns);
		}
		return guessPoints.get((int)Math.round(Math.random() * guessPoints.size())).returnValue();
//		throw new UnsupportedOperationException("No further implementation!");
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

		lsolve(A, b);

		// verwerking (volgorde?)
		// Checken of er al opgeloste zijn (Zo ja verwerk en maak nieuwe kleinere array? of verder gaan met grote en - waardes?)
		// Rij manipulatie, zou ook

//		System.out.print("\nb = Ax");
//		for (int i = 0; i < b.length; i++) {
//			System.out.print("\n" + b[i] + " | ");
//			for (int j = 0; j < A[0].length; j++) {
//				System.out.print(A[i][j] + " ");
//			}
//		}
//		System.out.println("\n");

		for (int i = 0; i < A.length; i++) {
			int bi = b[i];
			int countPos = 0;
			int countNeg = 0;
			for (int j = 0; j < A[i].length; j++) {
				if (A[i][j] < 0) {
					countNeg += A[i][j];
				} else if (A[i][j] > 0) {
					countPos += A[i][j];
				}
			}

			if (bi != 0 && bi == countPos) {
				// All positives are mines
				for (int j = 0; j < A[i].length; j++) {
					if (A[i][j] > 0) {
						mines.add(island.unknowns.get(j));
					}
				}
			} else if (bi != 0 && bi == countNeg) {
				// All negatives are mines
				for (int j = 0; j < A[i].length; j++) {
					if (A[i][j] < 0) {
						mines.add(island.unknowns.get(j));
					}
				}
			} else if (bi == 0 && countPos != 0 && countNeg == 0) {
				// All positives are safe
				for (int j = 0; j < A[i].length; j++) {
					if (A[i][j] > 0) {
						safePoints.add(island.unknowns.get(j));
					}
				}
			} else if (bi == 0 && countPos == 0 && countNeg != 0) {
				// All negatives are safe
				for (int j = 0; j < A[i].length; j++) {
					if (A[i][j] < 0) {
						safePoints.add(island.unknowns.get(j));
					}
				}
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

//	private void processSurroundingUnknowns(Point point, List<Point> knowns, List<Point> unknowns) {
//		List<Point> newUnknowns = new ArrayList<>();
//		for (int yOffset = -1; yOffset <= 1; yOffset++) {
//			for (int xOffset = -1; xOffset <= 1; xOffset++) {
//				// TODO: stop island bij bom afscheiding
//				// TODO: check voor randen
//				if (unknown.equals(node(point.x + xOffset, point.y + yOffset))) {
//					Point newPoint = new Point(point.x + xOffset,point.y + yOffset);
//					if (!unknowns.contains(newPoint)) {
//						newUnknowns.add(newPoint);
//						unknowns.add(newPoint);
//					}
//				}
//			}
//		}
//		for (Point newPoint : newUnknowns) {
//			// TODO: als node een mine is propageerd het island niet.
//			processSurroundingKnowns(newPoint, knowns, unknowns);
//		}
//	}

	private boolean isUnknown(int x, int y) {
		if (x < 0 || x >= fieldWidth
				|| y < 0 || y >= fieldHeight)  {
			throw new IllegalArgumentException();
		}
		return unknown.equals(node(x, y));
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

	private Integer[] lsolve(int[][] A, int[] b) {
		int n = Math.min(b.length,A[0].length);

		Integer[] solution = new Integer[A[0].length];

		for (int p = 0; p < n; p++) {

			// find pivot row and swap
			int max = p;
			for (int i = p + 1; i < n; i++) {
				if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
					max = i;
				}
			}
			int[] temp = A[p];
			A[p] = A[max];
			A[max] = temp;
			int t = b[p];
			b[p] = b[max];
			b[max] = t;

			// singular or nearly singular
			if (p >=  A[p].length || Math.abs(A[p][p]) == 0) {
				continue;
			}

			// pivot within A and b
			for (int i = p + 1; i < n; i++) {
				if (A[i][p] == 0) {
					continue;
				}
				int alpha = A[i][p] / A[p][p];
				b[i] -= alpha * b[p];
				for (int j = p; j < A.length; j++) {
					A[i][j] -= alpha * A[p][j];
				}
			}

			//TODO YH: fill solution matrix, but how?

		}

		return solution;
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
}
