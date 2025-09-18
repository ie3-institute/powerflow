/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import breeze.linalg.DenseMatrix
import breeze.numerics.abs
import edu.ie3.powerflow.model.JacobianMatrix
import edu.ie3.test.common.UnitSpec
import edu.ie3.test.common.powerflow.SixNodesTestData

class SixNodesJacobianMatrixSpec extends UnitSpec with SixNodesTestData {
  val testTolerance = 1e-3

  "Six node grid - The jacobian matrix" should {
    "calculate the correct jacobian matrix" in {
      val method =
        PrivateMethod[DenseMatrix[Double]](Symbol("buildJacobianMatrix"))

      val actual = JacobianMatrix invokePrivate method(
        initialState,
        admittanceMatrix,
      )

      actual.rows should be(expectedJacobianMatrix.rows)
      actual.cols should be(expectedJacobianMatrix.cols)

      /* Prompt whats wrong to understand where the bomb explodes */
      for
        rowIdx <- 0 until expectedJacobianMatrix.rows;
        colIdx <- 0 until expectedJacobianMatrix.rows
      do {
        if abs(
            actual.valueAt(rowIdx, colIdx) - expectedJacobianMatrix
              .valueAt(rowIdx, colIdx)
          ) > testTolerance
        then
          logger.debug(s"Mismatch in (${rowIdx}, ${colIdx}): Actual = ${actual
              .valueAt(rowIdx, colIdx)}, expected = ${expectedJacobianMatrix
              .valueAt(rowIdx, colIdx)}")
      }

      val toleranceRespected =
        (actual - expectedJacobianMatrix).forall(entry =>
          abs(entry) < testTolerance
        )
      toleranceRespected should be(true)
    }
  }
}
