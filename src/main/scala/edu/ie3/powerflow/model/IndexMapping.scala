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
    indexesWithoutSlack: Array[Int],
    indexesWithoutSlackAndPV: Array[Int],
    indexesWithoutSlackAndPQ: Array[Int],
    nodeCountPV: Int,
    countPQWithIntermediate: Int,
) {

  val nodeCountWithoutSlack: Int = indexesWithoutSlack.length
  val nodeCountPQ: Int = indexesWithoutSlackAndPV.length
}

object IndexMapping {

  def apply(data: Array[? <: NodeData]): IndexMapping = {
    val withoutSlack = data.filterNot(_.nodeType == SL)
    val withoutSlackAndPV = data.filter(_.nodeType == PQ)
    val withoutSlackAndPQ =
      data.filter(n => n.nodeType == PV || n.nodeType == PQ_INTERMEDIATE)

    val indexesWithoutSlack = Array.ofDim[Int](withoutSlack.length)
    val indexesWithoutSlackAndPV = Array.ofDim[Int](withoutSlackAndPV.length)
    val indexesWithoutSlackAndPQ = Array.ofDim[Int](withoutSlackAndPQ.length)

    var idx = 0
    withoutSlack.foreach { n =>
      indexesWithoutSlack(idx) = n.index
      idx += 1
    }

    idx = 0
    withoutSlackAndPV.foreach { n =>
      indexesWithoutSlackAndPV(idx) = n.index
      idx += 1
    }

    idx = 0
    withoutSlackAndPQ.foreach { n =>
      indexesWithoutSlackAndPQ(idx) = n.index
      idx += 1
    }

    new IndexMapping(
      indexesWithoutSlack,
      indexesWithoutSlackAndPV,
      indexesWithoutSlackAndPQ,
      data.count(_.nodeType == PV),
      data.count(n => n.nodeType == PQ || n.nodeType == PQ_INTERMEDIATE),
    )
  }

}
