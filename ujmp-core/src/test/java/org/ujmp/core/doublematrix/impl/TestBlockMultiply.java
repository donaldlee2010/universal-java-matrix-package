/*
 * Copyright (C) 2010 by Frode Carlsen
 *
 * This file is part of the Universal Java Matrix Package (UJMP).
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * UJMP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * UJMP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with UJMP; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */
package org.ujmp.core.doublematrix.impl;

import static java.lang.System.out;

import java.util.concurrent.Callable;

import junit.framework.TestCase;

import org.junit.Test;
import org.ujmp.core.Matrix;
import org.ujmp.core.doublematrix.impl.BlockMatrixLayout.BlockOrder;
import org.ujmp.core.util.UJMPSettings;

public class TestBlockMultiply extends TestCase {

	public static void multiplyCompare(int m, int n, int k) throws Exception {

		boolean useJblas = UJMPSettings.getInstance().isUseJBlas();
		UJMPSettings.getInstance().setUseJBlas(false);
		boolean useBlockMultiply = UJMPSettings.getInstance().isUseBlockMatrixMultiply();

		try {
			BlockDenseDoubleMatrix2D a = Fixture.createBlockRowLayoutWithGeneratedData(m, n,
					BlockOrder.ROWMAJOR);
			BlockDenseDoubleMatrix2D b = Fixture.createBlockRowLayoutWithGeneratedData(n, k,
					BlockOrder.COLUMNMAJOR);
			Matrix d = Fixture.createDenseMatrixWithGeneratedData(m, n);
			Matrix e = Fixture.createDenseMatrixWithGeneratedData(n, k);

			UJMPSettings.getInstance().setUseBlockMatrixMultiply(false);
			Matrix f = multiplyDenseDouble2DMatrix(d, e);

			UJMPSettings.getInstance().setUseBlockMatrixMultiply(true);
			Matrix c = multiplyBlockMatrix(a, b);

			Fixture.compare(f, c);
		} finally {
			// reset
			UJMPSettings.getInstance().setUseBlockMatrixMultiply(useBlockMultiply);
			UJMPSettings.getInstance().setUseJBlas(useJblas);
		}

		System.gc();
	}

	private static Matrix multiplyDenseDouble2DMatrix(Matrix d, Matrix e) throws Exception {

		Matrix f = Fixture.createBlockMultiplier(d, e).call();

		assertTrue("f==null", f != null);
		return f;
	}

	private static BlockDenseDoubleMatrix2D multiplyBlockMatrix(BlockDenseDoubleMatrix2D a,
			BlockDenseDoubleMatrix2D b) throws Exception {
		Callable<BlockDenseDoubleMatrix2D> multiplyTask = Fixture.createMultiplier(a, b);
		Callable<BlockDenseDoubleMatrix2D> multiplyTimer = new TimerDecorator<BlockDenseDoubleMatrix2D>(
				a.getRowCount(), a.getColumnCount(), b.getColumnCount(), multiplyTask);

		Callable<BlockDenseDoubleMatrix2D> callable = multiplyTimer;
		BlockDenseDoubleMatrix2D c = callable.call();
		assertTrue("c==null", c != null);
		return c;
	}

	protected static BlockDenseDoubleMatrix2D multiplyMatrix(final int i, final int j, final int k,
			final int blkSz, final boolean time) throws Exception {

		double[][] matA = Fixture.createLargeMatrix(i, j);
		double[][] matB = Fixture.createLargeMatrix(j, k);
		final BlockDenseDoubleMatrix2D a = Fixture.createBlockRowLayout(matA, blkSz,
				BlockOrder.ROWMAJOR);
		final BlockDenseDoubleMatrix2D b = Fixture.createBlockRowLayout(matB, blkSz,
				BlockOrder.COLUMNMAJOR);
		matA = null;
		matB = null;

		Callable<BlockDenseDoubleMatrix2D> task = Fixture.createMultiplier(a, b);

		BlockDenseDoubleMatrix2D c = new TimerDecorator<BlockDenseDoubleMatrix2D>(i, j, k, task)
				.call();

		return c;
	}

	private void multiplyWithDifferentBlockOrder(BlockOrder aOrder, BlockOrder bOrder) {
		System.out.println();
		BlockDenseDoubleMatrix2D a = Fixture.createBlockRowLayout(Fixture.MAT_A1, 3, aOrder);
		System.out.println(a);

		BlockDenseDoubleMatrix2D b = Fixture.createBlockRowLayout(Fixture.MAT_B1, 3, bOrder);
		System.out.println(b);

		BlockDenseDoubleMatrix2D c = (BlockDenseDoubleMatrix2D) a.mtimes(b);

		System.out.println(c);
		Fixture.assertMatrixEquals2DIntArray(c, Fixture.C_2x3_RESULT);
	}

	@Test
	public void testMultiplyBlockLayout() throws Exception {
		BlockDenseDoubleMatrix2D a = Fixture.createBlockRowLayout(Fixture.MAT_A, 2,
				BlockOrder.ROWMAJOR);
		BlockDenseDoubleMatrix2D b = Fixture.createBlockRowLayout(Fixture.MAT_A, 2,
				BlockOrder.COLUMNMAJOR);

		BlockDenseDoubleMatrix2D c = (BlockDenseDoubleMatrix2D) a.mtimes(b);

		out.format("\n\n\na = \n%s", a.getBlockLayout());
		out.format("\n\n\nb = \n%s", b.getBlockLayout());
		out.format("\n\n\nc = \n%s", c.getBlockLayout());

		out.format("\n\n\na = \n%s", a);
		out.format("\n\n\nb = \n%s", b);
		out.format("\n\n\nc = \n%s", c);

		Fixture.assertMatrixEquals2DIntArray(c, Fixture.MAT_C);
	}

	@Test
	public void testThatMultiply1000x100x1000xwithSquareBlockGivesCorrectResult() throws Exception {
		int m = 1000, n = 100, k = 1000;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply1000x100x100xwithSquareBlockGivesCorrectResult() throws Exception {
		int m = 1000, n = 100, k = 100;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply1000x2000x101xwithSquareBlockGivesCorrectResult() throws Exception {
		int m = 1000, n = 2000, k = 100;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply110x120x115xwithSquareBlockGivesCorrectResult() throws Exception {
		int m = 110, n = 120, k = 115;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiplyThenSubtractGivesCorrectBlockSize() throws Exception {

		BlockDenseDoubleMatrix2D a = new BlockDenseDoubleMatrix2D(new double[][] { { 1, 2, 3 },
				{ 1, 2, 4 }, { 1, 2, 6 } }, 2, BlockOrder.ROWMAJOR);

		BlockDenseDoubleMatrix2D b = new BlockDenseDoubleMatrix2D(new double[][] { { 1 }, { 2 },
				{ 3 } }, 2, BlockOrder.COLUMNMAJOR);

		BlockDenseDoubleMatrix2D c = new BlockDenseDoubleMatrix2D(new double[][] { { 1 }, { 2 },
				{ 3 } }, 2, BlockOrder.ROWMAJOR);

		BlockDenseDoubleMatrix2D axb = (BlockDenseDoubleMatrix2D) a.mtimes(b);
		System.out.println(axb);
		System.out.println(axb.minus(c));
	}

	@Test
	public void testThatMultiply100x100x100xwithSquareBlockGivesCorrectResult() throws Exception {
		int m = 100, n = 100, k = 100;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply2x3MatrixWithRectangularBlockGivesCorrectResult() throws Exception {
		System.out.println();
		BlockDenseDoubleMatrix2D a = Fixture.createBlockRowLayout(Fixture.MAT_A1, 2,
				BlockOrder.ROWMAJOR);
		System.out.println(a);

		BlockDenseDoubleMatrix2D b = Fixture.createBlockRowLayout(Fixture.MAT_B1, 2,
				BlockOrder.COLUMNMAJOR);
		System.out.println(b);

		BlockDenseDoubleMatrix2D c = (BlockDenseDoubleMatrix2D) a.mtimes(b);

		System.out.println(c);

		Fixture.assertMatrixEquals2DIntArray(c, Fixture.C_2x3_RESULT);
	}

	@Test
	public void testThatMultiply2x3MatrixWithSquareBlockGivesCorrectResult() throws Exception {
		System.out.println();
		BlockDenseDoubleMatrix2D a = Fixture.createBlockRowLayout(Fixture.MAT_A1, 3,
				BlockOrder.ROWMAJOR);
		System.out.println(a);

		BlockDenseDoubleMatrix2D b = Fixture.createBlockRowLayout(Fixture.MAT_B1, 3,
				BlockOrder.COLUMNMAJOR);
		System.out.println(b);

		BlockDenseDoubleMatrix2D c = (BlockDenseDoubleMatrix2D) a.mtimes(b);

		System.out.println(c);
		Fixture.assertMatrixEquals2DIntArray(c, Fixture.C_2x3_RESULT);

	}

	@Test
	public void testThatMultiply50x30x40withSquareBlock5x5GivesCorrectResult() throws Exception {
		int m = 50, n = 30, k = 40;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply50x31x40withSquareBlock5x5GivesCorrectResult() throws Exception {
		int m = 50, n = 31, k = 40;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply51x30x40withSquareBlock5x5GivesCorrectResult() throws Exception {
		int m = 51, n = 30, k = 40;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiply51x30x41withSquareBlock5x5GivesCorrectResult() throws Exception {
		int m = 51, n = 30, k = 41;
		multiplyCompare(m, n, k);
	}

	@Test
	public void testThatMultiplyColumnMajorAndColumnMajorGivesCorrectResult() throws Exception {
		multiplyWithDifferentBlockOrder(BlockOrder.COLUMNMAJOR, BlockOrder.COLUMNMAJOR);
	}

	@Test
	public void testThatMultiplyColumnMajorAndRowMajorGivesCorrectResult() throws Exception {
		multiplyWithDifferentBlockOrder(BlockOrder.COLUMNMAJOR, BlockOrder.ROWMAJOR);
	}

	@Test
	public void testThatMultiplyRowMajorAndColumnMajorGivesCorrectResult() throws Exception {
		multiplyWithDifferentBlockOrder(BlockOrder.ROWMAJOR, BlockOrder.COLUMNMAJOR);
	}

	@Test
	public void testThatMultiplyRowMajorAndRowMajorBlockGivesCorrectResult() throws Exception {
		multiplyWithDifferentBlockOrder(BlockOrder.ROWMAJOR, BlockOrder.ROWMAJOR);
	}
}
