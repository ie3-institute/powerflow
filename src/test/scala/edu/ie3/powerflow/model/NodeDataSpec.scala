/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import breeze.linalg.DenseVector
import breeze.math.Complex
import edu.ie3.powerflow.model.NodeData.{PresetData, StateData}
import edu.ie3.powerflow.model.enums.NodeType
import edu.ie3.powerflow.util.exceptions.PowerFlowException
import edu.ie3.test.common.UnitSpec
import edu.ie3.test.common.powerflow.SixNodesTestData

class NodeDataSpec extends UnitSpec with SixNodesTestData {
  "The NodeData" should {
    "pick the correct element by index" in {
      val expected = operationPoint(4)
      val actual = NodeData.getByIndex(operationPoint, 4)

      actual should be(expected)
    }

    "throw an PowerFlowException, if the element is not apparent" in {
      val thrown = intercept[PowerFlowException] {
        NodeData.getByIndex(operationPoint, -1)
      }

      thrown.getMessage should be("Cannot find a node with index -1")
    }

    "throw an PowerFlowException, if the intended and actual node length does not match" in {
      val intendedOrder = Option.apply((0 to 6).toVector)

      val thrown = intercept[PowerFlowException] {
        NodeData.correctOrder(operationPoint, intendedOrder)
      }

      thrown.getMessage should be(
        "The provided data has not the expected length (expected = 7, actual = 6)"
      )
    }

    "correct the order of an input array according to the specified order" in {
      val intendedOrder = Option.apply(Vector(0, 1, 3, 5, 2, 4))

      val expected: Array[PresetData] = Array(
        PresetData(0, NodeType.SL, Complex(0, 0)), /* Node 0 */
        PresetData(1, NodeType.PQ, Complex(5, 0)), /* Node 1 */
        PresetData(3, NodeType.PV, Complex(5, 0)), /* Node 3 */
        PresetData(5, NodeType.PQ, Complex(5, 0)), /* Node 5 */
        PresetData(2, NodeType.PQ, Complex(5, 0)), /* Node 2 */
        PresetData(4, NodeType.PV, Complex(5, 0)), /* Node 4 */
      )

      val actual = NodeData.correctOrder(operationPoint, intendedOrder)

      actual should be(expected)
    }
  }

  "The PresetData" should {
    "extract a power vector correctly" in {
      val expected = DenseVector(
        Complex.zero,
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
      )
      val actual = PresetData.extractPowerVector(operationPoint)

      actual should be(expected)
    }
  }

  "The StateData" should {
    "extract a voltage vector correctly" in {
      val expected = DenseVector(
        Complex.one,
        Complex(0.985, 0.0),
        Complex(0.975, 0.0),
        Complex.one,
        Complex.one,
        Complex(0.95, 0.0),
      )
      val actual = StateData.extractVoltageVector(lastState)

      actual should be(expected)
    }

    "extract a power vector correctly" in {
      val expected = DenseVector(
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
        Complex(5.0, 0.0),
      )
      val actual = StateData.extractPowerVector(lastState)

      actual should be(expected)
    }
  }
}
