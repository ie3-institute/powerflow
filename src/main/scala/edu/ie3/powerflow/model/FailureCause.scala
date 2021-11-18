/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

sealed trait FailureCause

object FailureCause {
  case object MaxIterationsReached extends FailureCause
  case object CalculationFailed extends FailureCause
}
