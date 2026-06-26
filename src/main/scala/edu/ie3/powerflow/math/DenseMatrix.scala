/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import edu.ie3.powerflow.libraries.blas.Blas
import edu.ie3.powerflow.libraries.lapack.Lapack
import edu.ie3.powerflow.math.NumericOperations.{Mul, Solve, Split, Sub}

import scala.reflect.ClassTag

/** A dense matrix with column major memory layout if [[isTransposed]] is false,
  * else with row major memory layout.
  *
  * @param cols
  *   The number of matrix columns.
  * @param rows
  *   The number of matrix rows.
  * @param data
  *   The actual data of the matrix
  * @param majorStride
  *   The distance separating two column (or rows, if transposed). Should have
  *   absolute value >= rows (or cols, if transposed)
  * @param isTransposed
  *   True, if data is given in row mayor layout.
  * @tparam V
  *   The type of data this matrix holds.
  */
final class DenseMatrix[@specialized(Double) V: ClassTag](
    val rows: Int,
    val cols: Int,
    private val data: Array[V],
    private val majorStride: Int,
    val isTransposed: Boolean = false,
) extends NumericOperations[DenseMatrix[V]] {

  def linearSize: Int = rows * cols

  def apply(row: Int, col: Int): V = data(linearIndex(row, col))

  def update(row: Int, col: Int, value: V): Unit = {
    data(linearIndex(row, col)) = value
  }

  def valueAt(row: Int, column: Int): V = data(linearIndex(row, column))

  def linearIndex(row: Int, column: Int): Int = {
    if isTransposed then {
      row * majorStride + column
    } else {
      column * majorStride + row
    }
  }

  def rowAndColumnFromLinearIndex(index: Int): (Int, Int) = {
    val r = index % majorStride
    val c = index / majorStride
    if isTransposed then {
      (c, r)
    } else {
      (r, c)
    }
  }

  def iterator: Iterator[((Int, Int), V)] = data.zipWithIndex.map {
    case (value, idx) => (rowAndColumnFromLinearIndex(idx), value)
  }.iterator

  def columnIterator: Iterator[DenseVector[V]] = if !isTransposed then {
    data.grouped(cols).map(DenseVector.apply)
  } else {
    Iterator.range(0, cols).map { col =>
      val vec = new DenseVector(rows, Array.ofDim(rows))

      for row <- 0 until rows do {
        vec(row) = valueAt(row, col)
      }

      vec
    }
  }

  def rowIterator: Iterator[DenseVector[V]] = if isTransposed then {
    data.grouped(cols).map(DenseVector.apply)
  } else {
    Iterator.range(0, cols).map { row =>
      val vec = new DenseVector(rows, Array.ofDim(rows))

      for col <- 0 until rows do {
        vec(col) = valueAt(row, col)
      }

      vec
    }
  }

  def map[R: ClassTag](f: V => R): DenseMatrix[R] =
    new DenseMatrix(rows, cols, data.map(f), majorStride, isTransposed)

  def foreach[U](f: ((Int, Int), V) => U): Unit =
    data.zipWithIndex.foreach { case (value, idx) =>
      f(rowAndColumnFromLinearIndex(idx), value)
    }

  def forall(p: V => Boolean): Boolean = data.forall(p)

  def exist(p: V => Boolean): Boolean = data.exists(p)

}

object DenseMatrix {
  lazy val lapack: Lapack = Lapack.lib
  lazy val blas: Blas = Blas.lib

  def filled[V: ClassTag](
      rows: Int,
      cols: Int,
      value: V,
      isTransposed: Boolean = false,
  ): DenseMatrix[V] = {
    val length = cols * rows
    val data = Array.fill(length)(value)
    new DenseMatrix(cols, rows, data, rows, isTransposed)
  }

  def apply[R <: Seq[V], V: ClassTag](rows: R*): DenseMatrix[V] = {
    val nRows = rows.length
    val nCols = rows.headOption match {
      case Some(firstRow) => firstRow.length
      case None           => 0
    }

    val matrix =
      new DenseMatrix(nRows, nCols, Array.ofDim(nRows * nCols), nCols)

    rows.zipWithIndex.foreach { case (rowData, row) =>
      rowData.zipWithIndex.foreach { case (value, col) =>
        matrix(row, col) = value
      }
    }

    matrix
  }

  given SPLIT_CM
      : Split[DenseMatrix[Complex], DenseMatrix[Double], DenseMatrix[Double]] =
    matrix => {
      val cols = matrix.cols
      val rows = matrix.rows

      val realPart = filled(cols, rows, 0d, matrix.isTransposed)
      val imagPart = filled(cols, rows, 0d, matrix.isTransposed)

      matrix.foreach { case ((row, col), value) =>
        realPart(row, col) = value.real
        imagPart(row, col) = value.imag
      }

      (realPart, imagPart)
    }

  given SUB_DMDM
      : Sub[DenseMatrix[Double], DenseMatrix[Double], DenseMatrix[Double]] =
    (matrix1, matrix2) => {
      val res = DenseMatrix.filled(matrix1.rows, matrix1.cols, 0d)

      matrix1.iterator.foreach { case ((row, col), value) =>
        res(row, col) = value - matrix2(row, col)
      }

      res
    }

  given MUL_RMRV
      : Mul[DenseMatrix[Double], DenseVector[Double], DenseVector[Double]] =
    (matrix, vec) => {
      val y = blas.dgemv(
        if matrix.isTransposed then "T" else "N",
        if matrix.isTransposed then matrix.cols else matrix.rows,
        if matrix.isTransposed then matrix.rows else matrix.cols,
        1.0,
        matrix.data,
        matrix.majorStride,
        vec.data,
        1,
        0.0,
        Array.fill(vec.length)(0),
        1,
      )

      DenseVector(y)
    }

  given MUL_CMCV
      : Mul[DenseMatrix[Complex], DenseVector[Complex], DenseVector[Complex]] =
    (matrix, vec) => {
      val data = matrix.rowIterator.map { row =>
        row * vec
      }.toArray

      DenseVector(data)
    }

  given SOLVE_DMDV
      : Solve[DenseMatrix[Double], DenseVector[Double], DenseVector[Double]] =
    (matrix, vec) => {
      val n = matrix.cols
      val ipiv = Array.fill(n)(0)

      val b = lapack.dgesv(
        n,
        1,
        matrix.data,
        n,
        ipiv,
        vec.toArray,
        n,
        0,
      )

      DenseVector(b)
    }

}
