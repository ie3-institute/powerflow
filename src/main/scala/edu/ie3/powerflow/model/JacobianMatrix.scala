/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import breeze.linalg.{Axis, DenseMatrix}
import breeze.math.Complex
import edu.ie3.powerflow.model.NodeData.StateData
import edu.ie3.powerflow.model.enums.NodeType
import edu.ie3.powerflow.util.exceptions.PowerFlowException

/** Data model for the sub matrices of partial derivations
  * @param dPdF
  *   Sub matrix of partial deviation of active power to imaginary part of
  *   voltage
  * @param dPdE
  *   Sub matrix of partial deviation of active power to real part of voltage
  * @param dQdF
  *   Sub matrix of partial deviation of reactive power to imaginary part of
  *   voltage
  * @param dQdE
  *   Sub matrix of partial deviation of reactive power to real part of voltage
  * @param dV2dF
  *   Sub matrix of partial deviation of squared voltage magnitude to imaginary
  *   part of voltage
  * @param dV2dE
  *   Sub matrix of partial deviation of squared voltage magnitude to real part
  *   of voltage
  */
final case class JacobianMatrix(
    dPdF: DenseMatrix[Double],
    dPdE: DenseMatrix[Double],
    dQdF: DenseMatrix[Double],
    dQdE: DenseMatrix[Double],
    dV2dF: DenseMatrix[Double],
    dV2dE: DenseMatrix[Double]
)

case object JacobianMatrix {
  def apply(nodeAmount: Int): JacobianMatrix = {
    new JacobianMatrix(
      DenseMatrix.zeros(nodeAmount, nodeAmount),
      DenseMatrix.zeros(nodeAmount, nodeAmount),
      DenseMatrix.zeros(nodeAmount, nodeAmount),
      DenseMatrix.zeros(nodeAmount, nodeAmount),
      DenseMatrix.zeros(nodeAmount, nodeAmount),
      DenseMatrix.zeros(nodeAmount, nodeAmount)
    )
  }

  /** Building the jacobian matrix for Taylor series calculation
    * @param lastState
    *   The last known state of the grid
    * @param admittanceMatrix
    *   The dimensionless admittance matrix describing the grid structure
    * @return
    *   A [[DenseMatrix]] describing the gradients in each direction
    */
  def buildJacobianMatrix(
      lastState: Array[StateData],
      admittanceMatrix: DenseMatrix[Complex]
  ): DenseMatrix[Double] = {
    val voltages = StateData.extractVoltageVector(lastState)
    val nodeCount = admittanceMatrix.rows
    val fullMatrix = JacobianMatrix(nodeCount)

    for (
      row <- 0 until nodeCount; rowType = lastState(row).nodeType;
      col <- 0 until nodeCount
    ) {
      rowType match {
        case NodeType.PQ | NodeType.PQ_INTERMEDIATE | NodeType.PV =>
          val gij = admittanceMatrix.valueAt(row, col).real
          val bij = admittanceMatrix.valueAt(row, col).imag
          val ei = voltages(row).real
          val ej = voltages(col).real
          val fi = voltages(row).imag
          val fj = voltages(col).imag
          if (row == col) {
            fullMatrix.dPdF(row, col) += 2 * fi * gij // dPi/dfi
            fullMatrix.dPdE(row, col) += 2 * ei * gij // dPi/dei
            fullMatrix.dQdF(row, col) += -2 * fi * bij // dQi/dfi
            fullMatrix.dQdE(row, col) += -2 * ei * bij // dQi/dei
            fullMatrix.dV2dF(row, col) += 2 * fi // dU²i/dfi
            fullMatrix.dV2dE(row, col) += 2 * ei // dU²i/dei
          } else {
            fullMatrix.dPdF(row, col) += -ei * bij + fi * gij // dPi/dfj
            fullMatrix.dPdF(row, row) += fj * gij + ej * bij // dPi/dfi
            fullMatrix.dPdE(row, col) += ei * gij + fi * bij // dPi/dej
            fullMatrix.dPdE(row, row) += ej * gij - fj * bij // dPi/dei
            fullMatrix.dQdF(row, col) += -fi * bij - ei * gij // dQi/dfj
            fullMatrix.dQdF(row, row) += ej * gij - fj * bij // dQi/dfi
            fullMatrix.dQdE(row, col) += fi * gij - ei * bij // dQi/dej
            fullMatrix.dQdE(row, row) += -(fj * gij + ej * bij) // dQi/dei
          }
        case NodeType.SL => /* Leave out the line of the slack node */
      }
    }

    /* Reduce the matrix by deleting the unneeded columns and rows */
    val slIdx = lastState
      .filter(_.nodeType == NodeType.SL)
      .map(_.index)
      .toIndexedSeq
    val pqIdx = lastState
      .filter(_.nodeType == NodeType.PQ)
      .map(_.index)
      .toIndexedSeq
    val pvIdx = lastState
      .filter(_.nodeType == NodeType.PV)
      .map(_.index)
      .toIndexedSeq

    JacobianMatrix.reduceAndCombine(fullMatrix, slIdx, pqIdx, pvIdx)
  }

  /** Finally build the jacobian matrix based on the sub matrices of partial
    * deviations. The final form is:
    *
    * dPdF | dPdE
    * ------------- dQdF | dQdF
    * ------------- dV2dF | dV2dE
    *
    * To represent the correct linearised equations of the grid, several rows
    * and columns have to be deleted: 1) The columns at the position of the
    * slack node 2) In all submatrices => The row of the slack node 3) In dQd*
    * \=> The rows of the voltage regulated nodes (PV nodes) 4) In dV2d* => The
    * rows of the nodes with fixed power (PQ nodes)
    *
    * @param matrix
    *   The matrices to concatenate
    * @param slIdx
    *   Indices of the slack nodes
    * @param pqIdx
    *   Indices of the PQ nodes
    * @param pvIdx
    *   Indices of the PV nodes
    * @return
    *   The concatenated total jacobian matrix
    */
  private def reduceAndCombine(
      matrix: JacobianMatrix,
      slIdx: IndexedSeq[Int],
      pqIdx: IndexedSeq[Int],
      pvIdx: IndexedSeq[Int]
  ): DenseMatrix[Double] = {
    val dP = DenseMatrix
      .horzcat(
        matrix.dPdF.delete(slIdx, Axis._1),
        matrix.dPdE.delete(slIdx, Axis._1)
      )
      .delete(slIdx, Axis._0)
    val dQ = DenseMatrix
      .horzcat(
        matrix.dQdF.delete(slIdx, Axis._1),
        matrix.dQdE.delete(slIdx, Axis._1)
      )
      .delete(slIdx ++ pvIdx, Axis._0)
    val dV2 = DenseMatrix
      .horzcat(
        matrix.dV2dF.delete(slIdx, Axis._1),
        matrix.dV2dE.delete(slIdx, Axis._1)
      )
      .delete(slIdx ++ pqIdx, Axis._0)

    (dQ.rows, dV2.rows) match {
      case (0, 0) =>
        throw new PowerFlowException(
          "There are neither submatrices for PQ as well as for PV nodes."
        )
      case (_, 0) => DenseMatrix.vertcat(dP, dQ)
      case (0, _) => DenseMatrix.vertcat(dP, dV2)
      case _ =>
        DenseMatrix.vertcat(
          DenseMatrix.vertcat(dP, dQ),
          dV2
        )
    }
  }
}
