/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

/** A representation of a sparse matrix. This class is only a container for some
  * data.
  * @param cols
  *   The number of matrix columns.
  * @param rows
  *   The number of matrix rows.
  * @param colOffset
  *   The start index of a column in the value array.
  * @param rowIndices
  *   The row index of a value in the value array.
  * @param values
  *   The non-zero elements of the matrix.
  */
final case class CSCMatrix(
    rows: Int,
    cols: Int,
    colOffset: Array[Int],
    rowIndices: Array[Int],
    values: Array[Double],
)
