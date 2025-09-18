/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import breeze.numerics.abs
import edu.ie3.powerflow.model.PowerFlowResult
import edu.ie3.powerflow.model.PowerFlowResult.FailedPowerFlowResult.FailedNewtonRaphsonPFResult
import edu.ie3.powerflow.model.PowerFlowResult.SuccessFullPowerFlowResult.ValidNewtonRaphsonPFResult
import edu.ie3.test.common.UnitSpec
import edu.ie3.test.common.powerflow.SixNodesTestData

class SixNodesNewtonRaphsonQLimitSpec extends UnitSpec with SixNodesTestData {
  val tolerance = 1e-12
  val finalResultTolerance = 1e-4
  val nr =
    NewtonRaphsonPF(tolerance, 50, admittanceMatrix)

  "Six node grid with Q limits - The Newton Raphson power flow algorithm" should {
    "calculate the iterated power correctly" in {
      val method =
        PrivateMethod[PowerFlowResult](Symbol("solveIterationStepsRecursively"))
      val actual = nr invokePrivate method(0, operationPoint, initialState)

      val eval = actual match {
        case validResult: ValidNewtonRaphsonPFResult =>
          (validResult.nodeData zip expectedFinalState).forall(node => {
            val actual = node._1
            val expected = node._2
            val result = actual.index == expected.index && abs(
              actual.voltage.real - expected.voltage.real
            ) < finalResultTolerance && abs(
              actual.voltage.imag - expected.voltage.imag
            ) < finalResultTolerance && abs(
              actual.power.real - expected.power.real
            ) < finalResultTolerance && abs(
              actual.power.imag - expected.power.imag
            ) < finalResultTolerance
            if !result then {
              logger.error(
                s"Mismatch in final result of node {${actual.index}}: actual(v = ${actual.voltage}, " +
                  s"s = ${actual.power}) vs. expected(v = ${expected.voltage}, s = ${expected.power})"
              )
            }
            result
          })
        case _: FailedNewtonRaphsonPFResult =>
          logger.error(
            "Got a non valid result, although I expected to get a valid one."
          )
          false
      }

      eval should be(true)
    }
  }
}
