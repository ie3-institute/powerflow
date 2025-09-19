/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import breeze.math.Complex
import breeze.numerics.abs
import edu.ie3.powerflow.model.NodeData.StateData
import edu.ie3.powerflow.model.StartData.{
  WithForcedStartVoltages,
  WithLastState,
}
import edu.ie3.powerflow.model.enums.NodeType
import edu.ie3.test.common.UnitSpec
import edu.ie3.test.common.powerflow.SixNodesTestData

class InititalStatePreparationSpec extends UnitSpec with SixNodesTestData {
  val testTolerance = 1e-3

  "The initial state preparation" should {
    "apply a flat start, if no data is given" in {
      val expected = Array(
        StateData(0, NodeType.SL, Complex.one, Complex(0, 0)), /* Node 0 */
        StateData(1, NodeType.PQ, Complex.one, Complex(5, 0)), /* Node 1 */
        StateData(2, NodeType.PQ, Complex.one, Complex(5, 0)), /* Node 2 */
        StateData(3, NodeType.PV, Complex.one, Complex(5, 0)), /* Node 3 */
        StateData(4, NodeType.PV, Complex.one, Complex(5, 0)), /* Node 4 */
        StateData(5, NodeType.PQ, Complex.one, Complex(5, 0)), /* Node 5 */
      )
      val actual = NewtonRaphsonPF.getInitialState(operationPoint, None)

      actual should be(expected)
    }

    "apply a fixed, given voltage vector, if it is fully provided" in {
      val withForcedState = Option.apply(
        WithForcedStartVoltages(
          Array(
            StateData(0, NodeType.SL, Complex.one, Complex.zero),
            StateData(1, NodeType.PQ, Complex(0.995, 0.0), Complex.zero),
            StateData(2, NodeType.PQ, Complex(0.99, 0.0), Complex.zero),
            StateData(3, NodeType.PV, Complex(0.985, 0.0), Complex.zero),
            StateData(4, NodeType.PV, Complex(0.98, 0.0), Complex.zero),
            StateData(5, NodeType.PQ, Complex(0.975, 0.0), Complex.zero),
          )
        )
      )

      val expected = Array(
        StateData(0, NodeType.SL, Complex.one, Complex.zero),
        StateData(1, NodeType.PQ, Complex(0.995, 0.0), Complex.zero),
        StateData(2, NodeType.PQ, Complex(0.99, 0.0), Complex.zero),
        StateData(3, NodeType.PV, Complex(0.985, 0.0), Complex.zero),
        StateData(4, NodeType.PV, Complex(0.98, 0.0), Complex.zero),
        StateData(5, NodeType.PQ, Complex(0.975, 0.0), Complex.zero),
      )
      val actual =
        NewtonRaphsonPF.getInitialState(operationPoint, withForcedState)

      actual should be(expected)
    }

    "apply a flat start and only fix the slack voltage, if only the slack voltage is provided" in {
      val withForcedSlackVoltage = Option.apply(
        WithForcedStartVoltages(
          Array(
            StateData(0, NodeType.SL, Complex(0.9985, 0.0125), Complex.zero)
          )
        )
      )
      val expected = Array(
        StateData(0, NodeType.SL, Complex(0.9985, 0.0125), Complex(0, 0)),
        /* Node 0 */
        StateData(1, NodeType.PQ, Complex.one, Complex(5, 0)), /* Node 1 */
        StateData(2, NodeType.PQ, Complex.one, Complex(5, 0)), /* Node 2 */
        StateData(3, NodeType.PV, Complex.one, Complex(5, 0)), /* Node 3 */
        StateData(4, NodeType.PV, Complex.one, Complex(5, 0)), /* Node 4 */
        StateData(5, NodeType.PQ, Complex.one, Complex(5, 0)), /* Node 5 */
      )
      val actual =
        NewtonRaphsonPF.getInitialState(operationPoint, withForcedSlackVoltage)

      actual should be(expected)
    }

    "apply a voltage estimation based on the last state and the jacobian matrix" in {
      val lastState = Array(
        StateData(
          0,
          NodeType.SL,
          Complex(0.9985, 0.0125),
          Complex(-6.87744915, -5.5946818),
        ),
        StateData(
          1,
          NodeType.PQ,
          Complex(0.985, 0),
          Complex(3.387143682, 2.770284329),
        ),
        StateData(
          2,
          NodeType.PQ,
          Complex(0.975, 0),
          Complex(3.352756985, 2.73748644),
        ),
        StateData(
          3,
          NodeType.PV,
          Complex.one,
          Complex(-13.7548985, -11.1924635),
        ),
        StateData(4, NodeType.PV, Complex.one, Complex(0, 1.6e-3)),
        StateData(
          5,
          NodeType.PQ,
          Complex(0.95, 0),
          Complex(13.06715358, 10.65240558),
        ),
      )
      val withLastState =
        Option.apply(
          WithLastState(lastState, Complex.one, expectedJacobianMatrix)
        )

      val actual =
        NewtonRaphsonPF.getInitialState(operationPoint, withLastState)

      val expectedNewState = Array(
        StateData(
          0,
          NodeType.SL,
          Complex.one,
          Complex(-6.87744915, -5.5946818),
        ),
        StateData(
          1,
          NodeType.PQ,
          Complex(0.986608384359005, -0.010702565981),
          Complex(3.387143682, 2.770284329),
        ),
        StateData(
          2,
          NodeType.PQ,
          Complex(0.977627232935143, -0.017833425996),
          Complex(3.352756985, 2.73748644),
        ),
        StateData(
          3,
          NodeType.PV,
          Complex(1.0, -0.02800042151243394),
          Complex(-13.7548985, -11.1924635),
        ),
        StateData(
          4,
          NodeType.PV,
          Complex(1.0, -0.032462570375342316),
          Complex(0, 1.6e-3),
        ),
        StateData(
          5,
          NodeType.PQ,
          Complex(0.9865890673875589, -0.036917689490892025),
          Complex(13.06715358, 10.65240558),
        ),
      )

      val toleranceMet =
        (actual zip expectedNewState).forall(actualVsExpected => {
          val actual = actualVsExpected._1
          val expected = actualVsExpected._2
          abs(actual.power.real - expected.power.real) < testTolerance &&
          abs(actual.power.imag - expected.power.imag) < testTolerance &&
          abs(actual.voltage.real - expected.voltage.real) < testTolerance &&
          abs(actual.voltage.imag - expected.voltage.imag) < testTolerance
        })

      toleranceMet should be(true)
    }
  }
}
