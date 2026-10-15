/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import edu.ie3.powerflow.math.SparseSolver

/** Representation of the Newton-Raphson algorithm to solve the system of
  * non-linear equations, that do describe a heat grid.
  *
  * ATTENTION: It is the users responsibility, that the data fed in here and in
  * the calculate method actually does fit together in terms of the bus indices.
  *
  * @param epsilon
  *   Permissible deviation between actual (given) use of the grid (P, Q, |V|)
  *   and those values determined during iterative solving of the system of
  *   equations.
  * @param maxIterations
  *   Maximum amount of iterations.
  * @param admittanceMatrix
  *   The dimensionless admittance matrix describing the grid structure.
  * @param sparseSolver
  *   An option for a sparse matrix solver.
  */
final case class NewtonRaphsonHF(
    epsilon: Double,
    maxIterations: Integer,
    sparseSolver: Option[SparseSolver] = None,
) {}
