/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import dev.ludovic.netlib.blas.BLAS
import dev.ludovic.netlib.lapack.LAPACK
import edu.ie3.powerflow.math.NumericOperations.{Mul, Solve, Split, Sub}
import edu.ie3.powerflow.{ffor, fforA}
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
final case class DenseMatrix[@specialized(Double) V: ClassTag](
    rows: Int,
    cols: Int,
    private val data: Array[V],
    private val majorStride: Int,
    isTransposed: Boolean = false,
) extends NumericOperations[DenseMatrix[V]] {

  def linearSize: Int = rows * cols

  def apply(row: Int, col: Int): V = data(linearIndex(row, col))

  def getCols: DenseVector[DenseVector[V]] = DenseVector(columnIterator.toArray)

  def asArray2D: Array[Array[V]] = colIteratorInternal.toArray[Array[V]]

  def update(row: Int, col: Int, value: V): Unit = {
    data(linearIndex(row, col)) = value
  }

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

  def iterator: Iterator[((Int, Int), V)] = data.iterator.zipWithIndex.map {
    case (value, idx) => (rowAndColumnFromLinearIndex(idx), value)
  }

  def columnIterator: Iterator[DenseVector[V]] =
    colIteratorInternal.map(DenseVector.apply)

  private def colIteratorInternal: Iterator[Array[V]] = if !isTransposed then {
    data.grouped(cols)
  } else {
    Iterator.range(0, cols).map { col =>
      fforA[V](0, rows) { row => apply(row, col) }
    }
  }

  def rowIterator: Iterator[DenseVector[V]] =
    rowIteratorInternal.map(DenseVector.apply)

  private def rowIteratorInternal: Iterator[Array[V]] = if isTransposed then {
    data.grouped(cols)
  } else {
    Iterator.range(0, cols).map { row =>
      fforA[V](0, rows) { col => apply(row, col) }
    }
  }

  def map[R: ClassTag](f: V => R): DenseMatrix[R] =
    new DenseMatrix(rows, cols, data.map(f), majorStride, isTransposed)

  def foreach[U](f: ((Int, Int), V) => U): Unit = ffor(0, data.length) { idx =>
    f(rowAndColumnFromLinearIndex(idx), data(idx))
  }

  def forall(p: V => Boolean): Boolean = data.forall(p)

  def exists(p: V => Boolean): Boolean = data.exists(p)

}

object DenseMatrix {
  private lazy val blas = BLAS.getInstance()
  private lazy val lapack = LAPACK.getInstance()

  def empty[V: ClassTag](
      rows: Int,
      cols: Int,
      isTransposed: Boolean = false,
  ): DenseMatrix[V] = {
    val length = cols * rows
    val data = Array.ofDim[V](length)
    new DenseMatrix(rows, cols, data, cols, isTransposed)
  }

  def filled[V: ClassTag](
      rows: Int,
      cols: Int,
      value: V,
      isTransposed: Boolean = false,
  ): DenseMatrix[V] = {
    val length = cols * rows
    val data: Array[V] = fforA(0, length) { _ => value }
    new DenseMatrix(rows, cols, data, cols, isTransposed)
  }

  def apply[R <: Seq[V], V: ClassTag](rows: R*): DenseMatrix[V] = {
    val nRows = rows.length
    val nCols = rows.headOption match {
      case Some(firstRow) => firstRow.length
      case None           => 0
    }

    val matrix =
      new DenseMatrix(nRows, nCols, Array.ofDim[V](nRows * nCols), nCols)

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

      ffor(0, length) { idx =>
        val el: Double = data(idx)

        if el != 0d then {
          nonZeroEl += 1
        }
      }

      nonZeroEl
    }

    def isSparse(nonZeroElementCount: Int): Boolean =
      nonZeroElementCount < matrix.linearSize / 10 * 4

    def toSparse(nonZeroElementCount: Int): CSCMatrix = {
      val rows: Int = matrix.rows
      val cols: Int = matrix.cols
      val data: Array[Double] = matrix.data

      val columnOffset: Array[Int] = Array.ofDim[Int](matrix.cols + 1)
      val rowIndices: Array[Int] = Array.ofDim[Int](nonZeroElementCount)
      val values: Array[Double] = Array.ofDim[Double](nonZeroElementCount)

      var dataIdx = 0
      var count = 0

      ffor(0, cols) { colIdx =>
        columnOffset(colIdx) = count
        val offset = colIdx * rows

        ffor(0, rows) { rowIdx =>
          val element: Double = data(offset + rowIdx)

          if element != 0.0 then {
            rowIndices(dataIdx) = rowIdx
            values(dataIdx) = element

            dataIdx += 1
            count += 1
          }
        }
      }

      columnOffset(cols) = count

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
      val data = matrix.data
      val majorStride = matrix.majorStride

      val len = matrix.linearSize

      val real = Array.ofDim[Double](len)
      val imag = Array.ofDim[Double](len)

      ffor(0, len) { idx =>
        val c: Complex = data(idx)
        real(idx) = c.real
        imag(idx) = c.imag
      }

      (
        DenseMatrix(rows, cols, real, majorStride),
        DenseMatrix(rows, cols, imag, majorStride),
      )
    }

  given SUB_DMDM
      : Sub[DenseMatrix[Double], DenseMatrix[Double], DenseMatrix[Double]] =
    (matrix1, matrix2) => {
      val array: Array[Double] = Array.ofDim[Double](matrix1.linearSize)
      val data1: Array[Double] = matrix1.data
      val data2: Array[Double] = matrix2.data

      if matrix1.isTransposed == matrix2.isTransposed then {
        ffor(0, data1.length) { idx =>
          array(idx) = data1(idx) - data2(idx)
        }
      } else {
        ffor(0, data1.length) { idx =>
          val (r, c) = matrix1.rowAndColumnFromLinearIndex(idx)
          val data2Idx = matrix2.linearIndex(r, c)
          array(idx) = data1(idx) - data2(data2Idx)
        }
      }

      DenseMatrix(
        matrix1.rows,
        matrix2.cols,
        array,
        matrix1.majorStride,
        matrix1.isTransposed,
      )
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
        0,
        matrix.majorStride,
        vec.data,
        0,
        1,
        0.0,
        y.data,
        0,
        1,
      )

      y
    }

  given MUL_RMRM
      : Mul[DenseMatrix[Double], DenseMatrix[Double], DenseMatrix[Double]] =
    (matrixA, matrixB) => {
      val transA = if matrixA.isTransposed then "T" else "N"
      val transB = if matrixB.isTransposed then "T" else "N"

      val m = if matrixA.isTransposed then matrixA.cols else matrixA.rows
      val n = if matrixB.isTransposed then matrixB.rows else matrixB.cols

      val dataC: Array[Double] = Array.ofDim[Double](m * n)

      blas.dgemm(
        transA,
        transB,
        m,
        n,
        if matrixA.isTransposed then matrixA.rows else matrixA.cols,
        1.0,
        matrixA.data,
        0,
        matrixA.majorStride,
        matrixB.data,
        0,
        matrixB.majorStride,
        0.0,
        dataC,
        0,
        m,
      )

      new DenseMatrix[Double](n, m, dataC, m)
    }

  given MUL_CMCV
      : Mul[DenseMatrix[Complex], DenseVector[Complex], DenseVector[Complex]] =
    (matrix, vec) => {
      val realArray: Array[Double] = Array.ofDim[Double](vec.length)
      val imagArray: Array[Double] = Array.ofDim[Double](vec.length)

      val vecData: Array[Complex] = vec.data
      val matrixData: Array[Complex] = matrix.data
      val majorStride: Int = matrix.majorStride

      val len = matrixData.length

      ffor(0, len) { idx =>
        val r = idx % majorStride
        val c = idx / majorStride

        val mValue = matrixData(idx)
        val vValue = vecData(c)

        realArray(r) += mValue.real * vValue.real - mValue.imag * vValue.imag
        imagArray(r) += mValue.real * vValue.imag + mValue.imag * vValue.real
      }

      Array.ofDim[Complex](vec.length)

      val lenArr = realArray.length

      val array: Array[Complex] = fforA(0, lenArr) { n =>
        Complex(realArray(n), imagArray(n))
      }

      DenseVector(lenArr, array)
    }

  given SOLVE_DMDV
      : Solve[DenseMatrix[Double], DenseVector[Double], DenseVector[Double]] =
    (matrix, vec) => {
      val n = matrix.cols
      val ipiv: Array[Int] = Array.ofDim[Int](n)
      val b: Array[Double] = vec.data

      lapack.dgesv(
        n,
        1,
        matrix.data,
        0,
        n,
        ipiv,
        0,
        b,
        0,
        n,
        new intW(0),
      )

      DenseVector(b)
    }

}
