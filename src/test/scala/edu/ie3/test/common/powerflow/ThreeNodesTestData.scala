/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.test.common.powerflow

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.math.Complex
import edu.ie3.powerflow.model.NodeData.{DeviationData, PresetData, StateData}
import edu.ie3.powerflow.model.enums.NodeType

/** This trait describes data needed for testing the power flow calculation by
  * means of Newton Raphson algorithm. The following assumptions apply:
  *
  * Reference System: 400 kVA @ 10 kV --> Reference admittance: 4 mS
  *
  * Line type: r = 0.437 Ω/km, x = 0.356 Ω/km, g = 0 S/km, b = 25.8 nS/km
  *
  * Lines: (3,4) -> 0.25 km -> g = 1375.48984, b = -1120.53635, g/2 = 0, b/2 =
  * 0.0016 (3,5) -> 1.25 km -> g = 275.09797, b = -224.10727, g/2 = 0, b/2 =
  * 0.0081
  */
trait ThreeNodesTestData {
  protected val admittanceMatrix: DenseMatrix[Complex] = DenseMatrix(
    (
      Complex(1650.58781, -1344.63392),
      Complex(-1375.48984, 1120.53635),
      Complex(-275.09797, 224.10727),
    ),
    (
      Complex(-1375.48984, 1120.53635),
      Complex(1375.48984, -1120.53475),
      Complex.zero,
    ),
    (
      Complex(-275.09797, 224.10727),
      Complex.zero,
      Complex(275.09797, -224.09917),
    ),
  )

  protected val expectedJacobianMatrix: DenseMatrix[Double] = DenseMatrix(
    (1120.53635, 0.0, 1375.48984, 0.0),
    (0.0, 224.10727, 0.0, 275.09797),
    (-1375.48984, 0.0, 1120.53315, 0.0),
    (0.0, -275.09797, 0.0, 224.09106999999997),
  )

  protected val nodeCount: Int = admittanceMatrix.rows

  protected val operationPoint: Array[PresetData] = Array(
    PresetData(0, NodeType.SL, Complex(0, 0)), /* Node 3 */
    PresetData(1, NodeType.PQ, Complex(5, 0)), /* Node 4 */
    PresetData(2, NodeType.PQ, Complex(5, 0)), /* Node 5 */
  )

  protected val initialState: Array[StateData] =
    operationPoint.map(op => StateData(op))

  protected val lastState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(5.0, 0.0)),
    StateData(1, NodeType.PQ, Complex(0.975, 0), Complex(5.0, 0.0)),
    StateData(2, NodeType.PQ, Complex(0.95, 0), Complex(5.0, 0.0)),
  )

  protected val expectedNewState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(5.0, 0.0)),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.9971905667, -0.0017806957),
      Complex(5.0, 0.0),
    ),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.9865890674, -0.0089172679),
      Complex(5.0, 0.0),
    ),
  )

  protected val expectedLastStateWitIteratedPower = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(-48.1421445, -39.20907225)),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.975, 0),
      Complex(33.52756485, 27.31459453),
    ),
    StateData(
      2,
      NodeType.PQ,
      Complex(0.95, 0),
      Complex(13.06715358, 10.65240558),
    ),
  )

  protected val expectedDeviation: Array[DeviationData] = Array(
    DeviationData(0, NodeType.SL, Complex(48.1421445, 39.20907225), 0.0),
    DeviationData(
      1,
      NodeType.PQ,
      Complex(-28.52756485, -27.31459453),
      -0.049375,
    ),
    DeviationData(2, NodeType.PQ, Complex(-8.06715358, -10.65240558), -0.0975),
  )

  protected val expectedDeviationVector: DenseVector[Double] =
    DenseVector[Double](
      -28.52756485,
      -8.06715358,
      -27.31459453,
      -10.65240558,
    )

  protected val expectedFinalState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(-10.06682837, -0.03522682)),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.99780702, -0.00178000),
      Complex(5.0, 0.0),
    ),
    StateData(
      2,
      NodeType.PQ,
      Complex(0.98887201, -0.00890001),
      Complex(5.0, 0.0),
    ),
  )
}
