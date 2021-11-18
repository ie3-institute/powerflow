/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.test.common.powerflow

import breeze.linalg.DenseMatrix
import breeze.math.Complex
import edu.ie3.powerflow.model.NodeData.{PresetData, StateData}
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
trait SixNodesQLimitTestData {
  protected val admittanceMatrix: DenseMatrix[Complex] = DenseMatrix(
    (
      Complex(1146.24153, -933.77229),
      Complex(-458.49661, 373.51212),
      Complex.zero,
      Complex(-687.74492, 560.26817),
      Complex.zero,
      Complex.zero
    ),
    (
      Complex(-458.49661, 373.51212),
      Complex(802.36907, -653.63491),
      Complex(-343.87246, 280.13409),
      Complex.zero,
      Complex.zero,
      Complex.zero
    ),
    (
      Complex.zero,
      Complex(-343.87246, 280.13409),
      Complex(343.87246, -280.12759),
      Complex.zero,
      Complex.zero,
      Complex.zero
    ),
    (
      Complex(-687.74492, 560.26817),
      Complex.zero,
      Complex.zero,
      Complex(2338.33273, -1904.89889),
      Complex(-1375.48984, 1120.53635),
      Complex(-275.09797, 224.10727)
    ),
    (
      Complex.zero,
      Complex.zero,
      Complex.zero,
      Complex(-1375.48984, 1120.53635),
      Complex(1375.48984, -1120.53475),
      Complex.zero
    ),
    (
      Complex.zero,
      Complex.zero,
      Complex.zero,
      Complex(-275.09797, 224.10727),
      Complex.zero,
      Complex(275.09797, -224.09917)
    )
  )

  protected val nodeCount: Int = admittanceMatrix.rows

  protected val operationPoint: Array[PresetData] = Array(
    PresetData(0, NodeType.SL, Complex(0, 0)), /* Node 0 */
    PresetData(1, NodeType.PQ, Complex(5, 0)), /* Node 1 */
    PresetData(2, NodeType.PQ, Complex(5, 0)), /* Node 2 */
    PresetData(
      3,
      NodeType.PV,
      Complex(5, 0),
      reactivePowerMin = Some(7.5),
      reactivePowerMax = Some(7.5)
    ), /* Node 3 */
    PresetData(
      4,
      NodeType.PV,
      Complex(5, 0),
      reactivePowerMin = Some(-2.5),
      reactivePowerMax = Some(2.5)
    ), /* Node 4 */
    PresetData(5, NodeType.PQ, Complex(5, 0)) /* Node 5 */
  )

  protected val initialState: Array[StateData] =
    operationPoint.map(op => StateData(op))

  protected val expectedFinalState = Array(
    StateData(0, NodeType.SL, Complex.one, Complex(0.0, 0.0)),
    StateData(
      1,
      NodeType.PQ,
      Complex(0.986494045, -0.010680011),
      Complex(5.0, 0.0)
    ),
    StateData(
      2,
      NodeType.PQ,
      Complex(0.977422768, -0.017799289),
      Complex(5.0, 0.0)
    ),
    StateData(
      3,
      NodeType.PV,
      Complex(0.993488408, -0.01942001),
      Complex(5.0, 0.0)
    ),
    StateData(
      4,
      NodeType.PV,
      Complex(0.992118823, -0.02228457),
      Complex(5.0, 0.0)
    ),
    StateData(
      5,
      NodeType.PQ,
      Complex(0.982114474, -0.028156108),
      Complex(5.0, 0.0)
    )
  )
}
