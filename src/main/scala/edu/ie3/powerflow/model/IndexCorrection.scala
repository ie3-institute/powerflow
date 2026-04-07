/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import edu.ie3.powerflow.model.enums.NodeType.{PQ, PQ_INTERMEDIATE, PV, SL}

// maps new index to old index
final case class IndexCorrection(
    indexesWithoutSlack: Map[Int, Int],
    indexesWithoutSlackAndPV: Map[Int, Int],
    indexesWithoutSlackAndPQ: Map[Int, Int],
) {

  val nodeCountWithoutSlack: Int = indexesWithoutSlack.size
  val nodeCountWithoutSlackAndPV: Int = indexesWithoutSlackAndPV.size
  val nodeCountWithoutSlackAndPQ: Int = indexesWithoutSlackAndPQ.size
  val countPQ: Int = nodeCountWithoutSlackAndPV
}

object IndexCorrection {

  def apply(data: Array[? <: NodeData]): IndexCorrection = {
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

    new IndexCorrection(
      indexesWithoutSlack,
      indexesWithoutSlackAndPV,
      indexesWithoutSlackAndPQ,
    )
  }

}
