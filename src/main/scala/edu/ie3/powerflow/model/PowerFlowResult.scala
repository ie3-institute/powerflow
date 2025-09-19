/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import breeze.linalg.DenseMatrix
import edu.ie3.powerflow.model.NodeData.StateData

sealed trait PowerFlowResult {
  val iteration: Int
}

object PowerFlowResult {
  sealed trait SuccessFullPowerFlowResult extends PowerFlowResult {
    val nodeData: Array[StateData]
  }

  object SuccessFullPowerFlowResult {
    final case class ValidNewtonRaphsonPFResult(
        iteration: Int,
        nodeData: Array[StateData],
        jacobianMatrix: DenseMatrix[Double],
    ) extends SuccessFullPowerFlowResult
  }

  sealed trait FailedPowerFlowResult extends PowerFlowResult {
    val cause: FailureCause
  }

  object FailedPowerFlowResult {
    final case class FailedNewtonRaphsonPFResult(
        iteration: Int,
        cause: FailureCause,
    ) extends FailedPowerFlowResult
  }
}
