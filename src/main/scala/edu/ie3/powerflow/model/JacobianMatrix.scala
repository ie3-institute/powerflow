/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.math.Complex
import breeze.storage.Zero
import edu.ie3.powerflow.model.NodeData.StateData

import scala.reflect.ClassTag

object JacobianMatrix {

  /** Method for calculating the jacobian matrix.
    * @param indexMapping
    *   Information for mapping the index.
    * @param lastState
    *   The last state of the calculation.
    * @param admittanceMatrix
    *   The complex admittance matrix.
    * @return
    *   The jacobian matrix.
    */
  def buildJacobianMatrix(
      indexMapping: IndexMapping,
      lastState: Array[StateData],
      admittanceMatrix: DenseMatrix[Complex],
  ): DenseMatrix[Double] = {
    val voltages = StateData.extractVoltageVector(lastState)

    val countNoSlack = indexMapping.nodeCountWithoutSlack
    val countNoSlackAndPV = indexMapping.nodeCountPQ
    val countNoSlackAndPQ = indexMapping.nodeCountPV

    val indexesWithoutSlack = indexMapping.indexesWithoutSlack
    val indexesWithoutSlackAndPV = indexMapping.indexesWithoutSlackAndPV
    val indexesWithoutSlackAndPQ = indexMapping.indexesWithoutSlackAndPQ

    val dim = 2 * countNoSlack
    val fullMatrix: DenseMatrix[Double] = DenseMatrix.zeros(dim, dim)

    val (gif, bie, gie, bif) = getSums(admittanceMatrix, voltages)

    // offsets
    val offsetJ3 = countNoSlack
    val offsetJ5 = offsetJ3 + countNoSlackAndPV
    val offsetJ2 = dim * countNoSlack
    val offsetJ4 = offsetJ2 + countNoSlack
    val offsetJ6 = offsetJ4 + countNoSlackAndPV

    for
      row <- 0 until countNoSlack
      col <- 0 until countNoSlack
    do {
      val oldRow = indexesWithoutSlack(row)
      val oldCol = indexesWithoutSlack(col)

      val c = admittanceMatrix.valueAt(oldRow, oldCol)
      val bij = c.imag
      val gij = c.real

      val v = voltages(oldRow)
      val fi = v.imag
      val ei = v.real

      val (rowJ1, colJ1) = calculateActualIndex(row, col, dim, 0)
      val (rowJ2, colJ2) = calculateActualIndex(row, col, dim, offsetJ2)

      fullMatrix(rowJ1, colJ1) = -ei * bij + fi * gij
      fullMatrix(rowJ2, colJ2) = ei * gij + fi * bij

      if row == col then {
        fullMatrix(rowJ1, colJ1) += gif(oldRow) + bie(oldRow)
        fullMatrix(rowJ2, colJ2) += gie(oldRow) - bif(oldRow)
      }
    }

    for
      row <- 0 until countNoSlackAndPV
      col <- 0 until countNoSlack
    do {
      val oldRow = indexesWithoutSlackAndPV(row)
      val oldCol = indexesWithoutSlack(col)

      val c = admittanceMatrix.valueAt(oldRow, oldCol)
      val bij = c.imag
      val gij = c.real

      val v = voltages(oldRow)
      val fi = v.imag
      val ei = v.real

      val (rowJ3, colJ3) = calculateActualIndex(row, col, dim, offsetJ3)
      val (rowJ4, colJ4) = calculateActualIndex(row, col, dim, offsetJ4)

      fullMatrix(rowJ3, colJ3) = -ei * gij - fi * bij
      fullMatrix(rowJ4, colJ4) = -ei * bij + fi * gij

      if row == col then {
        fullMatrix(rowJ3, colJ3) =
          -fi * bij + gie(oldRow) - bif(oldRow) - ei * gij
        fullMatrix(rowJ4, colJ4) =
          -ei * bij - gif(oldRow) - bie(oldRow) + fi * gij
      }
    }

    val pqOffset = indexMapping.nodeCountPQ - 1
    for
      row <- 0 until countNoSlackAndPQ
      col <- 0 until countNoSlack
    do {

      if row == col then {
        val v = voltages(indexesWithoutSlackAndPV(row))

        val (rowJ5, colJ5) =
          calculateActualIndex(row, col + pqOffset, dim, offsetJ5)
        val (rowJ6, colJ6) =
          calculateActualIndex(row, col + pqOffset, dim, offsetJ6)

        fullMatrix(rowJ5, colJ5) = 2 * v.imag
        fullMatrix(rowJ6, colJ6) = 2 * v.real
      }
    }

    fullMatrix
  }

  /** Method to calculat the index inside the full matrix based on the index of
    * the submatrix.
    * @param row
    *   The row of the submatrix.
    * @param col
    *   The column of the submatrix.
    * @param cols
    *   The number of columns in the full matrix.
    * @param offset
    *   The offset for the submatrix.
    * @return
    *   The row and column of the element in the full matrix.
    */
  private def calculateActualIndex(
      row: Int,
      col: Int,
      cols: Int,
      offset: Int,
  ): (Int, Int) = {
    val linIdx = col * cols + row + offset

    val r = linIdx % cols
    val c = linIdx / cols

    (r, c)
  }

  /** Method for calculating some common sums.
    * @param matrix
    *   The complex admittance matrix.
    * @param voltages
    *   The complex voltage vector.
    * @return
    *   The following vectors: G(i,:)*f , B(i,:)*e , G(i,:)*e , B(i,:)*f
    */
  private def getSums(
      matrix: DenseMatrix[Complex],
      voltages: DenseVector[Complex],
  ): (
      DenseVector[Double],
      DenseVector[Double],
      DenseVector[Double],
      DenseVector[Double],
  ) = {
    val size = voltages.size

    val gif: DenseVector[Double] = DenseVector.zeros(size)
    val bie: DenseVector[Double] = DenseVector.zeros(size)
    val gie: DenseVector[Double] = DenseVector.zeros(size)
    val bif: DenseVector[Double] = DenseVector.zeros(size)

    matrix.activeIterator.foreach { case ((i, _), value) =>
      val vi = voltages(i)
      val fi = vi.imag
      val ei = vi.real
      val bij = value.imag
      val gij = value.real

      gif(i) += gij * fi
      bie(i) += bij * ei
      gie(i) += gij * ei
      bif(i) += bij * fi
    }

    (gif, bie, gie, bif)
  }
}
