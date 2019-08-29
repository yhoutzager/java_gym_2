/*
 U mag de onderstaande melding niet verwijderen of wijzigen.
 You are not allowed to delete or change the statement below.

 Copyright (c) Quinity B.V. 2001 - 2019

 Intellectueel eigendom van Quinity B.V.
 Intellectual property of Quinity B.V.

 Deze documentatie en de programmatuur die door deze documentatie beschreven wordt mogen niet worden gebruikt door
 andere applicaties dan waarvoor Quinity voorafgaand schriftelijk toestemming heeft gegeven. Deze documentatie en de
 programmatuur die door deze documentatie beschreven wordt mogen niet worden gebruikt door andere organisaties dan
 waarvoor Quinity voorafgaand schriftelijk toestemming heeft gegeven. Deze documentatie en de programmatuur die door
 deze documentatie beschreven wordt mogen niet openbaar worden gemaakt door middel van druk, fotokopie, microfilm of
 op welke andere wijze ook, zonder voorafgaande schriftelijke toestemming van Quinity B.V.

 This documentation and the software / source code that is described by this documentation may not be used by
 applications without prior written permission by Quinity B.V. This documentation and the software / source code
 that is described by this documentation may not be used by organisations without written prior permission by
 Quinity B.V. This documentation and the software / source code that is described by this documentation may not be made
 public by means of print, (photo)copy, microfilm, or any other method without prior written permission by Quinity B.V.
*/
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

	private final int m;      // number of rows
	private final int n;      // number of columns
	private int[][] a;     // m-by-(n+1) augmented matrix

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

		//TODO YH: hier Integer[]?
		Integer[] x = primal();

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
		//TODO YH: regels voor oplossing specifieke matrix

		// back substitution
		Integer[] x = new Integer[n];
		for (int i = Math.min(n - 1, m - 1); i >= 0; i--) {
			int sum = 0;
			for (int j = i + 1; j < n; j++) {
				sum += a[i][j] * x[j];
			}

			if (Math.abs(a[i][i]) > EPSILON)
				x[i] = (a[i][n] - sum) / a[i][i];
			else if (Math.abs(a[i][n] - sum) > EPSILON)
				return null;
		}

		// redundant rows
		for (int i = n; i < m; i++) {
			double sum = 0.0;
			for (int j = 0; j < n; j++) {
				sum += a[i][j] * x[j];
			}
			if (Math.abs(a[i][n] - sum) > EPSILON)
				return null;
		}
		return x;
	}
}
