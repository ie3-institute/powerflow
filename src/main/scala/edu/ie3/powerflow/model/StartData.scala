/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import edu.ie3.powerflow.math.{DenseMatrix, Complex}
import edu.ie3.powerflow.model.NodeData.StateData

sealed trait StartData

object StartData {
  final case class WithForcedStartVoltages(nodes: Array[StateData])
      extends StartData

  final case class WithLastState(
      nodes: Array[StateData],
      slVoltage: Complex,
      jacobianMatrix: DenseMatrix[Double],
  ) extends StartData
}
