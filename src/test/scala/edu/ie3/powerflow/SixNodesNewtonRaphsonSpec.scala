/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import breeze.linalg.DenseVector
import breeze.numerics.abs
import edu.ie3.powerflow.model.PowerFlowResult
import edu.ie3.powerflow.model.NodeData.{DeviationData, StateData}
import edu.ie3.powerflow.model.PowerFlowResult.FailedPowerFlowResult.FailedNewtonRaphsonPFResult
import edu.ie3.powerflow.model.PowerFlowResult.SuccessFullPowerFlowResult.ValidNewtonRaphsonPFResult
import edu.ie3.test.common.UnitSpec
import edu.ie3.test.common.powerflow.SixNodesTestData

class SixNodesNewtonRaphsonSpec extends UnitSpec with SixNodesTestData {
  val tolerance = 1e-12
  val testTolerance = 1e-3
  val finalResultTolerance = 1e-4
  val nr =
    NewtonRaphsonPF(tolerance, 50, admittanceMatrix)

  "Six node grid - The Newton Raphson power flow algorithm" should {
    "calculate the iterated power correctly" in {
      val method =
        PrivateMethod[Array[StateData]](
          Symbol("calculateIterationApparentPower")
        )

      val actual: Array[StateData] = NewtonRaphsonPF invokePrivate method(
        lastState,
        admittanceMatrix,
      )

      val eval = (actual zip expectedLastStateWitIteratedPower).forall(
        actualVsExpected =>
          abs(
            actualVsExpected._1.power.real - actualVsExpected._2.power.real
          ) < testTolerance && abs(
            actualVsExpected._1.power.imag - actualVsExpected._2.power.imag
          ) < testTolerance
      )

      eval should be(true)
    }

    "calculate the deviation to the actual state correctly" in {
      val method =
        PrivateMethod[Array[DeviationData]](
          Symbol("calculateDeviationToActualState")
        )
      val expectedIteratedPower =
        StateData.extractPowerVector(expectedLastStateWitIteratedPower)
      val actual: Array[DeviationData] = NewtonRaphsonPF invokePrivate method(
        operationPoint,
        StateData.extractVoltageVector(lastState),
        expectedIteratedPower,
      )

      val eval =
        expectedDeviation.zipWithIndex.foldLeft(true)((eval, currentEntry) => {
          eval && (currentEntry._1.power.real - actual(
            currentEntry._2
          ).power.real).abs < testTolerance && (currentEntry._1.power.imag - actual(
            currentEntry._2
          ).power.imag).abs < testTolerance && abs(
            currentEntry._1.squaredVoltageMagnitude - actual(
              currentEntry._2
            ).squaredVoltageMagnitude
          ) < testTolerance
        })

      eval should be(true)
    }

    "build the combined deviation vector correctly" in {
      val method =
        PrivateMethod[DenseVector[Double]](
          Symbol("buildCombinedDeviationVector")
        )
      val actual = NewtonRaphsonPF invokePrivate method(expectedDeviation)

      actual.length should be(2 * nodeCount - 2)

      val eval =
        expectedDeviationVector.toScalaVector.zipWithIndex
          .forall(currentEntry =>
            (currentEntry._1 - actual(currentEntry._2)).abs < 1e-4
          )

      eval should be(true)
    }

    "apply the voltage correction correctly" in {
      val method =
        PrivateMethod[Option[Array[StateData]]](Symbol("correctVoltages"))
      val actualOpt = NewtonRaphsonPF invokePrivate method(
        lastState,
        expectedDeviationVector,
        expectedJacobianMatrix,
      )

      actualOpt.isDefined should be(true)
      val actual = actualOpt.getOrElse(
        throw new RuntimeException(
          "Cannot get the corrected voltages, although they are present!"
        )
      )

      val eval = expectedNewState.zipWithIndex.forall(currentEntry =>
        (currentEntry._1.voltage - actual(
          currentEntry._2
        ).voltage).abs < testTolerance
      )

      eval should be(true)
    }

    "calculate the final power flow correctly" in {
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
