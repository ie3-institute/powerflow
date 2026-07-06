/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import dev.ludovic.netlib.blas.BLAS
import dev.ludovic.netlib.lapack.LAPACK
import edu.ie3.powerflow.math.NumericOperations.{
  Mul,
  Solve,
  Split,
  Sub,
  Transform,
}
import org.netlib.util.intW

import scala.reflect.ClassTag

/** A dense matrix with column major memory layout if [[isTransposed]] is false,
  * else with row major memory layout.
  * @param rows
  *   The number of matrix rows.
  * @param cols
  *   The number of matrix columns.
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

  def columnIterator: Iterator[DenseVector[V]] =
    colIteratorInternal.map(DenseVector.apply)

  private def colIteratorInternal: Iterator[Array[V]] = if !isTransposed then {
    data.grouped(cols)
  } else {
    Iterator.range(0, cols).map { col =>
      val vec: Array[V] = Array.ofDim[V](rows)

      for row <- 0 until rows do {
        vec(row) = valueAt(row, col)
      }

      vec
    }
  }

  def rowIterator: Iterator[DenseVector[V]] =
    rowIteratorInternal.map(DenseVector.apply)

  private def rowIteratorInternal: Iterator[Array[V]] = if isTransposed then {
    data.grouped(cols)
  } else {
    Iterator.range(0, cols).map { row =>
      val vec: Array[V] = Array.ofDim[V](rows)

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

  def exists(p: V => Boolean): Boolean = data.exists(p)

}

object DenseMatrix {
  private lazy val blas = BLAS.getInstance()
  private lazy val lapack = LAPACK.getInstance()

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

  extension (matrix: DenseMatrix[Double]) {

    def countNonZeroElements: Int = {
      var nonZeroEl: Int = 0
      val data: Array[Double] = matrix.data
      val length = data.length
      var idx: Int = 0

      while idx < length do {
        val el: Double = data(idx)

        if el != 0d then {
          nonZeroEl += 1
        }

        idx += 1
      }

      nonZeroEl
    }

    def isSparse(nonZeroElementCount: Int): Boolean =
      nonZeroElementCount < matrix.linearSize / 10 * 4

    def toSparse(nonZeroElementCount: Int): CSCMatrix = {
      val rows: Int = matrix.rows
      val cols: Int = matrix.cols
      val data: Array[Double] = matrix.data
      val length = data.length
      var idx: Int = 0

      val columnOffset: Array[Int] = Array.ofDim[Int](matrix.cols + 1)
      val rowIndices: Array[Int] = Array.ofDim[Int](nonZeroElementCount)
      val values: Array[Double] = Array.ofDim[Double](nonZeroElementCount)

      var colIdx = 0
      var dataIdx = 0
      var count = 0
      var col = 0

      while colIdx < cols do {
        columnOffset(colIdx) = count

        var rowIdx = 0
        val offset = colIdx * rows

        while rowIdx < rows do {
          val element: Double = data(offset + rowIdx)

          if element != 0.0 then {
            rowIndices(dataIdx) = rowIdx
            values(dataIdx) = element

            dataIdx += 1
            count += 1
          }

          rowIdx += 1
        }

        colIdx += 1
      }

      columnOffset(colIdx) = count

      CSCMatrix(
        rows,
        cols,
        columnOffset,
        rowIndices,
        values,
      )
    }
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
      val trans = if matrix.isTransposed then "T" else "N"
      val y = DenseVector.filled(vec.length, 0d)

      blas.dgemv(
        trans,
        if matrix.isTransposed then matrix.cols else matrix.rows,
        if matrix.isTransposed then matrix.rows else matrix.cols,
        1.0,
        matrix.data,
        matrix.majorStride,
        vec.data,
        1,
        0.0,
        y.data,
        1,
      )

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
