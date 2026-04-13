/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import edu.ie3.powerflow.model.enums.NodeType.{PQ, PQ_INTERMEDIATE, PV, SL}

/** Mapping between the index in the admittance matrix and the index with some
  * of the nodes. This is used to reduce the number of needed calculations for
  * the jacobian matrix.
  * @param indexesWithoutSlack
  *   Maps the full index to the index without slack nodes.
  * @param indexesWithoutSlackAndPV
  *   Maps the full index to the index of only PV and PQ_INTERMEDIATE nodes.
  * @param indexesWithoutSlackAndPQ
  *   Maps the full index to the index of only PQ nodes.
  */
final case class IndexMapping(
    indexesWithoutSlack: Map[Int, Int],
    indexesWithoutSlackAndPV: Map[Int, Int],
    indexesWithoutSlackAndPQ: Map[Int, Int],
) {

  val nodeCountWithoutSlack: Int = indexesWithoutSlack.size
  val nodeCountPQ: Int = indexesWithoutSlackAndPV.size
  val nodeCountPV: Int = indexesWithoutSlackAndPQ.size
}

object IndexMapping {

  def apply(data: Array[? <: NodeData]): IndexMapping = {
    val indexesWithoutSlack = data
      .filterNot(_.nodeType == SL)
      .zipWithIndex
      .map { case (n, idx) => idx -> n.index }
      .toMap

    val indexesWithoutSlackAndPV = data
      .filter(_.nodeType == PQ)
      .zipWithIndex
      .map { case (n, idx) => idx -> n.index }
      .toMap

    val indexesWithoutSlackAndPQ = data
      .filter(n => n.nodeType == PV || n.nodeType == PQ_INTERMEDIATE)
      .zipWithIndex
      .map { case (n, idx) => idx -> n.index }
      .toMap

    new IndexMapping(
      indexesWithoutSlack,
      indexesWithoutSlackAndPV,
      indexesWithoutSlackAndPQ,
    )
  }

}
