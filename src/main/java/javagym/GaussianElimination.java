package javagym;

/**
 *  The {@code GaussianElimination} data type provides methods
 *  to solve a linear system of equations <em>Ax</em> = <em>b</em>,
 *  where <em>A</em> is an <em>m</em>-by-<em>n</em> matrix
 *  and <em>b</em> is a length <em>n</em> vector.
 *  <p>
 *  This is a bare-bones implementation that uses Gaussian elimination
 *  with partial pivoting.
 *  See <a href = "https://algs4.cs.princeton.edu/99scientific/GaussianEliminationLite.java.html">GaussianEliminationLite.java</a>
 *  for a stripped-down version that assumes the matrix <em>A</em> is square
 *  and nonsingular. See {@link GaussJordanElimination} for an alternate
 *  implementation that uses Gauss-Jordan elimination.
 *  For an industrial-strength numerical linear algebra library,
 *  see <a href = "http://math.nist.gov/javanumerics/jama/">JAMA</a>.
 *  <p>
 *  For additional documentation, see
 *  <a href="https://algs4.cs.princeton.edu/99scientific">Section 9.9</a>
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class GaussianElimination {
	private static final double EPSILON = 1e-8;

	private final int m;            // number of rows
	private final int n;            // number of columns
	private int[][] a;            	// m-by-(n+1) augmented matrix
	private Integer[] x;

	/**
	 * Solves the linear system of equations <em>Ax</em> = <em>b</em>,
	 * where <em>A</em> is an <em>m</em>-by-<em>n</em> matrix and <em>b</em>
	 * is a length <em>m</em> vector.
	 *
	 * @param A the <em>m</em>-by-<em>n</em> constraint matrix
	 * @param b the length <em>m</em> right-hand-side vector
	 * @throws IllegalArgumentException if the dimensions disagree, i.e.,
	 *                                  the length of {@code b} does not equal {@code m}
	 */
	public GaussianElimination(int[][] A, int[] b) {
		m = A.length;
		n = A[0].length;

		if (b.length != m) throw new IllegalArgumentException("Dimensions disagree");

		// build augmented matrix
		a = new int[m][n + 1];
		for (int i = 0; i < m; i++)
			for (int j = 0; j < n; j++)
				a[i][j] = A[i][j];
		for (int i = 0; i < m; i++)
			a[i][n] = b[i];

		forwardElimination();

		x = primal();
	}

	// forward elimination
	private void forwardElimination() {
		for (int p = 0; p < Math.min(m, n); p++) {

			// find pivot row using partial pivoting
			int max = p;
			for (int i = p + 1; i < m; i++) {
				if (Math.abs(a[i][p]) > Math.abs(a[max][p])) {
					max = i;
				}
			}

			// swap
			swap(p, max);

			// singular or nearly singular
			if (Math.abs(a[p][p]) <= EPSILON) {
				continue;
			}

			// pivot
			pivot(p);
		}
	}

	// swap row1 and row2
	private void swap(int row1, int row2) {
		int[] temp = a[row1];
		a[row1] = a[row2];
		a[row2] = temp;
	}

	// pivot on a[p][p]
	private void pivot(int p) {
		for (int i = p + 1; i < m; i++) {
			int alpha = a[i][p] / a[p][p];
			for (int j = p; j <= n; j++) {
				a[i][j] -= alpha * a[p][j];
			}
		}
	}

	/**
	 * Returns a solution to the linear system of equations <em>Ax</em> = <em>b</em>.
	 *
	 * @return a solution <em>x</em> to the linear system of equations
	 * <em>Ax</em> = <em>b</em>; {@code null} if no such solution
	 */
	public Integer[] primal() {

		// back substitution
		Integer[] x = new Integer[n];
		boolean allSolved = false;
		boolean[] rowSolved = new boolean[m];
		CHECK: while (!allSolved) {
			for (int i = 0; i < x.length; i++) {
				if (x[i] == null) {
					continue;
				}

				if (x[i] == 0) {
					for (int j =0; j < m; j++) {
						a[j][i] = 0;
					}
				} else if (x[i] == 1) {
					for (int j =0; j < m; j++) {
						if (a[j][i] > 0) {
							a[j][n] -= a[j][i];
							a[j][i] = 0;
						}
					}
				} else {
					throw new IllegalArgumentException();
				}
			}

			for (int i = 0; i < m; i++) {
				if (rowSolved[i]) {
					continue;
				}
				int bi = a[i][n];
				int countPos = 0;
				int countNeg = 0;
				for (int j = 0; j < n; j++) {
					if (a[i][j] < 0) {
						countNeg += a[i][j];
					} else if (a[i][j] > 0) {
						countPos += a[i][j];
					}
				}

				if (bi != 0 && bi == countPos) {
					// all positives are mines
					for (int j = 0; j < n; j++) {
						if (a[i][j] > 0) {
							x[j] = 1;
						}
					}
					rowSolved[i] = true;
					continue CHECK;
				} else if (bi != 0 && bi == countNeg) {
					// all negatives are mines
					for (int j = 0; j < n; j++) {
						if (a[i][j] < 0) {
							x[j] = 1;
						}
					}
					rowSolved[i] = true;
					continue CHECK;
				} else if (bi == 0 && countPos != 0 && countNeg == 0) {
					// all positives are safe
					for (int j = 0; j < n; j++) {
						if (a[i][j] > 0) {
							x[j] = 0;
						}
					}
					rowSolved[i] = true;
					continue CHECK;
				} else if (bi == 0 && countPos == 0 && countNeg != 0) {
					// all negatives are safe
					for (int j = 0; j < n; j++) {
						if (a[i][j] < 0) {
							x[j] = 0;
						}
					}
					rowSolved[i] = true;
					continue CHECK;
				}
			}

			allSolved = true;
		}

		return x;
	}

	public Integer[] getSolution() {
		return x;
	}
}
