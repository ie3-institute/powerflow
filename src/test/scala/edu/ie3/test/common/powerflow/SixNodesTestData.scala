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
  * Lines: (0,1) -> 0.75 km -> g = 458.49661, b = -373.51212, g/2 = 0, b/2 =
  * 0.0048 (1,2) -> 1.00 km -> g = 343.87246, b = -280.13409, g/2 = 0, b/2 =
  * 0.0065 (0,3) -> 0.50 km -> g = 687.74492, b = -560.26817, g/2 = 0, b/2 =
  * 0.0032 (3,4) -> 0.25 km -> g = 1375.48984, b = -1120.53635, g/2 = 0, b/2 =
  * 0.0016 (3,5) -> 1.25 km -> g = 275.09797, b = -224.10727, g/2 = 0, b/2 =
  * 0.0081
  */
trait SixNodesTestData {
  protected val admittanceMatrix: DenseMatrix[Complex] = DenseMatrix(
    (
      Complex(1146.24153, -933.77229),
      Complex(-458.49661, 373.51212),
      Complex.zero,
      Complex(-687.74492, 560.26817),
      Complex.zero,
      Complex.zero,
    ),
    (
      Complex(-458.49661, 373.51212),
      Complex(802.36907, -653.63491),
      Complex(-343.87246, 280.13409),
      Complex.zero,
      Complex.zero,
      Complex.zero,
    ),
    (
      Complex.zero,
      Complex(-343.87246, 280.13409),
      Complex(343.87246, -280.12759),
      Complex.zero,
      Complex.zero,
      Complex.zero,
    ),
    (
      Complex(-687.74492, 560.26817),
      Complex.zero,
      Complex.zero,
      Complex(2338.33273, -1904.89889),
      Complex(-1375.48984, 1120.53635),
      Complex(-275.09797, 224.10727),
    ),
    (
      Complex.zero,
      Complex.zero,
      Complex.zero,
      Complex(-1375.48984, 1120.53635),
      Complex(1375.48984, -1120.53475),
      Complex.zero,
    ),
    (
      Complex.zero,
      Complex.zero,
      Complex.zero,
      Complex(-275.09797, 224.10727),
      Complex.zero,
      Complex(275.09797, -224.09917),
    ),
  )

  protected val expectedJacobianMatrix: DenseMatrix[Double] = DenseMatrix(
    (
      653.64621, -280.13409, 0.0, 0.0, 0.0, 802.36907, -343.87246, 0.0, 0.0, 0.0,
    ),
    (
      -280.13409, 280.13409, 0.0, 0.0, 0.0, -343.87246, 343.87246, 0.0, 0.0, 0.0,
    ),
    (
      0.0, 0.0, 1904.91179, -1120.53635, -224.10727, 0.0, 0.0,
      2338.3327300000005, -1375.48984, -275.09797,
    ),
    (
      0.0, 0.0, -1120.53635, 1120.53635, 0.0, 0.0, 0.0, -1375.48984, 1375.48984,
      0.0,
    ),
    (
      0.0, 0.0, -224.10727, 0.0, 224.10727, 0.0, 0.0, -275.09797, 0.0, 275.09797,
    ),
    (
      -802.36907, 343.87246, 0.0, 0.0, 0.0, 653.62361, -280.13409, 0.0, 0.0, 0.0,
    ),
    (
      343.87246, -343.87246, 0.0, 0.0, 0.0, -280.13409, 280.12109, 0.0, 0.0, 0.0,
    ),
    (
      0.0, 0.0, 275.09797, 0.0, -275.09797, 0.0, 0.0, -224.10727, 0.0,
      224.09106999999997,
    ),
    (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0),
    (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0),
  )

  protected val nodeCount: Int = admittanceMatrix.rows

  protected val operationPoint: Array[PresetData] = Array(
    PresetData(0, NodeType.SL, Complex(0, 0)), /* Node 0 */
    PresetData(1, NodeType.PQ, Complex(5, 0)), /* Node 1 */
    PresetData(2, NodeType.PQ, Complex(5, 0)), /* Node 2 */
    PresetData(3, NodeType.PV, Complex(5, 0)), /* Node 3 */
    PresetData(4, NodeType.PV, Complex(5, 0)), /* Node 4 */
    PresetData(5, NodeType.PQ, Complex(5, 0)), /* Node 5 */
  )

  protected val initialState: Array[StateData] =
    operationPoint.map(op => StateData(op))

  protected val lastState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(5.0, 0.0)),
    StateData(1, NodeType.PQ, Complex(0.985, 0), Complex(5.0, 0.0)),
    StateData(2, NodeType.PQ, Complex(0.975, 0), Complex(5.0, 0.0)),
    StateData(3, NodeType.PV, Complex.one, Complex(5.0, 0.0)),
    StateData(4, NodeType.PV, Complex.one, Complex(5.0, 0.0)),
    StateData(5, NodeType.PQ, Complex(0.95, 0), Complex(5.0, 0.0)),
  )

  protected val expectedLastStateWitIteratedPower = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(-6.87744915, -5.5946818)),
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
    StateData(3, NodeType.PV, Complex.one, Complex(-13.7548985, -11.1924635)),
    StateData(4, NodeType.PV, Complex.one, Complex(0, 1.6e-3)),
    StateData(
      5,
      NodeType.PQ,
      Complex(0.95, 0),
      Complex(13.06715358, 10.65240558),
    ),
  )

  protected val expectedDeviation: Array[DeviationData] = Array(
    DeviationData(0, NodeType.SL, Complex(6.87744915, 5.5946818), 0.0),
    DeviationData(
      1,
      NodeType.PQ,
      Complex(1.612856318, -2.770284329),
      -0.029775,
    ),
    DeviationData(2, NodeType.PQ, Complex(1.647243015, -2.73748644), -0.049375),
    DeviationData(3, NodeType.PV, Complex(18.7548985, 11.1924635), 0.0),
    DeviationData(4, NodeType.PV, Complex(5.0, -1.6e-3), 0.0),
    DeviationData(5, NodeType.PQ, Complex(-8.06715358, -10.65240558), -0.0975),
  )

  protected val expectedDeviationVector: DenseVector[Double] =
    DenseVector[Double](
      1.612856318, 1.647243015, 18.7548985, 5.0, -8.06715358, -2.770284329,
      -2.73748644, -10.65240558, 0.0, 0.0,
    )

  protected val expectedNewState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(5.0, 0.0)),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.986608384359005, -0.010702565981),
      Complex(5.0, 0.0),
    ),
    StateData(
      2,
      NodeType.PQ,
      Complex(0.977627232935143, -0.017833425996),
      Complex(5.0, 0.0),
    ),
    StateData(
      3,
      NodeType.PV,
      Complex(1.0, -0.02800042151243394),
      Complex(5.0, 0.0),
    ),
    StateData(
      4,
      NodeType.PV,
      Complex(1.0, -0.032462570375342316),
      Complex(5.0, 0.0),
    ),
    StateData(
      5,
      NodeType.PQ,
      Complex(0.9865890673875589, -0.036917689490892025),
      Complex(5.0, 0.0),
    ),
  )

  protected val expectedFinalState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(-25.78079699, 18.49898950)),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.98649405, -0.01068001),
      Complex(5.0, 0.0),
    ),
    StateData(
      2,
      NodeType.PQ,
      Complex(0.97742277, -0.01779929),
      Complex(5.0, 0.0),
    ),
    StateData(
      3,
      NodeType.PV,
      Complex(0.99962531, -0.02738150),
      Complex(4.99999999, -12.92327040),
    ),
    StateData(
      4,
      NodeType.PV,
      Complex(0.99949285, -0.03185405),
      Complex(5.00000000, -6.16415937),
    ),
    StateData(
      5,
      NodeType.PQ,
      Complex(0.98825801, -0.03597386),
      Complex(5.0, 0.0),
    ),
  )
}
