/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import dev.ludovic.netlib.blas.BLAS
import dev.ludovic.netlib.lapack.LAPACK
import edu.ie3.powerflow.math.NumericOperations.{Mul, Solve, Split, Sub}
import org.netlib.util.intW

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

  private def checkIndex(row: Int, col: Int): Unit = {
    if row < 0 || row >= rows then
      throw new IndexOutOfBoundsException(
        s"${(row, col)} not in [0,$rows) x [0,$cols)"
      )
    if col < 0 || col >= cols then
      throw new IndexOutOfBoundsException(
        s"${(row, col)} not in [0,$rows) x [0,$cols)"
      )
  }

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

  def iterator: Iterator[((Int, Int), V)] = data.zipWithIndex.map { case (value, idx) => (rowAndColumnFromLinearIndex(idx), value) }.iterator

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
    Iterator.range(0, rows).map { row =>
      val vec = new DenseVector(cols, Array.ofDim(cols))

      for col <- 0 until cols do {
        vec(col) = valueAt(row, col)
      }

      vec
    }
  }

  def map[R: ClassTag](f: V => R): DenseMatrix[R] =
    new DenseMatrix(rows, cols, data.map(f), majorStride, isTransposed)

  def foreach[U](f: ((Int, Int), V) => U): Unit =
    data.zipWithIndex.iterator.map { case (value, idx) =>
      f(rowAndColumnFromLinearIndex(idx), value)
    }

  def forall(p: V => Boolean): Boolean = data.forall(p)

}

object DenseMatrix {
  lazy val blas = BLAS.getInstance()
  lazy val lapack = LAPACK.getInstance()

  def filled[V: ClassTag](rows: Int, cols: Int, value: V): DenseMatrix[V] = {
    val length = cols * rows
    val data = Array.fill(length)(value)
    new DenseMatrix(cols, rows, data, rows)
  }

  def apply[R <: Seq[V], V: ClassTag](rows: R*): DenseMatrix[V] = {
    val nRows = rows.length
    val nCols = rows.headOption match {
      case Some(firstRow) => firstRow.length
      case None => 0
    }

    val matrix = new DenseMatrix(nRows, nCols, Array.ofDim(nRows * nCols), nCols)

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

      val (real, imag) = if matrix.isTransposed then {
        (
          filled(cols, rows, 0d),
          filled(cols, rows, 0d),
        )
      } else {
        (
          filled(cols, rows, 0d),
          filled(cols, rows, 0d),
        )
      }

      matrix.foreach { case ((col, row), value) =>
        real(row, col) = value.real
        imag(row, col) = value.imag
      }

      (real, imag)
    }

  given SUB_DMDM: Sub[DenseMatrix[Double], DenseMatrix[Double], DenseMatrix[Double]] = (matrix1, matrix2) => {
    val res = DenseMatrix.filled(matrix1.rows, matrix1.cols, 0d)

    matrix1.iterator.foreach { case ((row, col), value) =>
     res(row, col) = value - matrix2(row, col)
    }

    res
  }

  given MUL_RMRV
      : Mul[DenseMatrix[Double], DenseVector[Double], DenseVector[Double]] =
    (matrix, vec) => {
      val trans = if matrix.isTransposed then "T" else "N"
      val y = DenseVector.filled(vec.length, 0d)

      blas.dgemv(trans, matrix.rows, matrix.cols, 1.0, matrix.data, matrix.rows, vec.toArray, 1, 0.0, y.toArray, 1)

      y
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
      val b = vec.toArray

      lapack.dgesv(
        n,
        1,
        matrix.data,
        n,
        ipiv,
        b,
        n,
        new intW(0),
      )

      DenseVector(b)
    }

}
