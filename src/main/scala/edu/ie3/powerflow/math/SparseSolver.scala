/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.{ADDRESS, JAVA_DOUBLE}

trait SparseSolver {

  def solve(
      matrix: CSCMatrix,
      b: DenseVector[Double],
  ): DenseVector[Double]
}
