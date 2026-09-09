/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

/** A representation of a sparse matrix. This class is only a container for some
  * data.
  * @param rows
  *   The number of matrix rows
  * @param cols
  *   The number of matrix columns.
  * @param colOffset
  *   An array containing the starting indices in [[rowIndices]] and [[values]]
  *   for each column.
  * @param rowIndices
  *   An array containing the row indices corresponding to each non-zero value.
  * @param values
  *   An array containing the actual non-zero elements.
  */
final case class CSCMatrix(
    rows: Int,
    cols: Int,
    colOffset: Array[Int],
    rowIndices: Array[Int],
    values: Array[Double],
)
