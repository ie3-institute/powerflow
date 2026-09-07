/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import edu.ie3.powerflow.math.{Complex, DenseMatrix, DenseVector}
import edu.ie3.powerflow.model.NodeData.StateData

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
    val voltages: Array[Complex] = lastState.map(_.voltage)
    val admittanceCols: Array[Array[Complex]] = admittanceMatrix.asArray2D

    val countNoSlack = indexMapping.nodeCountWithoutSlack
    val countNoSlackAndPV = indexMapping.nodeCountPQ
    val countNoSlackAndPQ = indexMapping.nodeCountPV

    val indexesWithoutSlack: Array[Int] = indexMapping.indexesWithoutSlack
    val indexesWithoutSlackAndPV: Array[Int] =
      indexMapping.indexesWithoutSlackAndPV
    val indexesWithoutSlackAndPQ: Array[Int] =
      indexMapping.indexesWithoutSlackAndPQ

    val dim = 2 * countNoSlack
    val array: Array[Double] = Array.ofDim[Double](dim * dim)

    val (gif, bie, gie, bif)
        : (Array[Double], Array[Double], Array[Double], Array[Double]) =
      getSums(admittanceMatrix, DenseVector(voltages))

    // offsets
    val offsetJ3 = countNoSlack
    val offsetJ5 = offsetJ3 + countNoSlackAndPV
    val offsetJ2 = dim * countNoSlack
    val offsetJ4 = offsetJ2 + countNoSlack
    val offsetJ6 = offsetJ4 + countNoSlackAndPV
    val pqOffset = indexMapping.nodeCountPQ - 1

    var col = 0
    while col < countNoSlack do {
      val oldCol = admittanceCols(indexesWithoutSlack(col))

      // for J1 and J2
      var row = 0
      while row < countNoSlack do {
        val oldRow: Int = indexesWithoutSlack(row)

        val c: Complex = oldCol(oldRow)
        val bij = c.imag
        val gij = c.real

        val v: Complex = voltages(oldRow)
        val fi = v.imag
        val ei = v.real

        val idxJ1 = calculateActualIndex(row, col, dim, 0)
        val idxJ2 = calculateActualIndex(row, col, dim, offsetJ2)

        array(idxJ1) = -ei * bij + fi * gij
        array(idxJ2) = ei * gij + fi * bij

        if row == col then {
          val gf: Double = gif(oldRow)
          val be: Double = bie(oldRow)
          val ge: Double = gie(oldRow)
          val bf: Double = bif(oldRow)

          array(idxJ1) += gf + be
          array(idxJ2) += ge - bf
        }

        row += 1
      }

      // for J3 and J4
      row = 0
      while row < countNoSlackAndPV do {
        val oldRow: Int = indexesWithoutSlackAndPV(row)

        val idxJ3 = calculateActualIndex(row, col, dim, offsetJ3)
        val idxJ4 = calculateActualIndex(row, col, dim, offsetJ4)

        val c: Complex = oldCol(oldRow)
        val bij = c.imag
        val gij = c.real

        val v: Complex = voltages(oldRow)
        val fi = v.imag
        val ei = v.real

        val ei_gij: Double = ei * gij
        val ei_bij: Double = ei * bij
        val fi_gij: Double = fi * gij
        val fi_bij: Double = fi * bij

        array(idxJ3) = -ei_gij - fi_bij
        array(idxJ4) = -ei_bij + fi_gij

        if row == col then {
          val gf: Double = gif(oldRow)
          val be: Double = bie(oldRow)
          val ge: Double = gie(oldRow)
          val bf: Double = bif(oldRow)

          array(idxJ3) = -fi_bij + ge - bf - ei_gij
          array(idxJ4) = -ei_bij - gf - be + fi_gij
        }

        row += 1
      }

      // for J5 and J6
      row = 0
      while row < countNoSlackAndPQ do {
        if row == col then {
          val v: Complex = voltages(indexesWithoutSlackAndPV(row))

          val idxJ5 = calculateActualIndex(row, col + pqOffset, dim, offsetJ5)
          val idxJ6 = calculateActualIndex(row, col + pqOffset, dim, offsetJ6)

          array(idxJ5) = 2 * v.imag
          array(idxJ6) = 2 * v.real
        }

        row += 1
      }

      col += 1
    }

    new DenseMatrix[Double](dim, dim, array, dim)
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
  ): Int = col * cols + row + offset

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
      Array[Double],
      Array[Double],
      Array[Double],
      Array[Double],
  ) = {
    val (g, b) = matrix.split
    val voltageMatrix: DenseMatrix[Double] = voltages.transform

    val gief: Array[Array[Double]] = (g * voltageMatrix).asArray2D
    val bief: Array[Array[Double]] = (b * voltageMatrix).asArray2D

    val gie = gief(0)
    val gif = gief(1)
    val bie = bief(0)
    val bif = bief(1)

    (gif, bie, gie, bif)
  }
}
