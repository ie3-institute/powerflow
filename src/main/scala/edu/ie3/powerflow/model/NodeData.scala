/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model

import breeze.linalg.DenseVector
import breeze.math.Complex
import com.typesafe.scalalogging.LazyLogging
import edu.ie3.powerflow.model.enums.NodeType
import edu.ie3.powerflow.util.exceptions.PowerFlowException

import scala.reflect.ClassTag

/** Classes or objects extending this Trait describe information about
  * electrical nodes
  */
sealed trait NodeData {
  val index: Int
  val nodeType: NodeType
}

object NodeData extends LazyLogging {

  /** Searches for a node with a given id in an Array of nodes and returns it or
    * throws an [[PowerFlowException]]
    *
    * @param array
    *   Array to search within
    * @param index
    *   Node index to search for
    * @tparam T
    *   Type parameter as an extension of the Trait
    * @return
    *   The found node
    */
  def getByIndex[T <: NodeData](array: Array[T], index: Int): T = {
    array
      .find(_.index == index)
      .getOrElse(
        throw new PowerFlowException("Cannot find a node with index " + index)
      )
  }

  /** Aligns the order of a given input array with the predefined order
    *
    * @param unordered
    *   (Possibly) unordered input array
    * @param intendedOrder
    *   Optional intended order
    * @tparam T
    *   Type parameter
    * @param ct
    *   Class tag of the generic type
    * @return
    *   An Array, whose order is aligned with the intended order, if some is
    *   provided
    */
  def correctOrder[T <: NodeData](
      unordered: Array[T],
      intendedOrder: Option[Vector[Int]]
  )(implicit ct: ClassTag[T]): Array[T] = {
    intendedOrder match {
      case None =>
        logger.debug(
          "There is no intended order provided, cannot correct the order!"
        )
        unordered
      case Some(order) if order.length != unordered.length =>
        throw new PowerFlowException(
          s"The provided data has not the expected length (expected = ${order.length}, actual = ${unordered.length})"
        )
      case Some(order) =>
        order.map(index => getByIndex(unordered, index)).toArray
    }
  }

  /** Used to compose the operation point required
    * [[edu.ie3.powerflow.NewtonRaphsonPF.calculate()]] which consists of
    * several, externally provided, elements of [[PresetData]].
    *
    * NOTES:
    *   - if [[nodeType]] == [[NodeType.PQ]]
    * -> target voltage will be neglected / ignored
    *
    *   - if [[nodeType]] == [[NodeType.PV]]
    * -> target voltage can only be set as absolute value of the complex value
    *
    *   - if [[nodeType]] == [[NodeType.SL]]
    * -> target voltage can be set as absolute value, which means that the phase
    * rotation of the slack node is ignored if you want to take into account the
    * phase rotation as well, you need to provide the Complex nodalVoltage of
    * the slack node as [[StartData]] as described in
    * [[edu.ie3.powerflow.NewtonRaphsonPF.calculate()]] and
    * [[edu.ie3.powerflow.NewtonRaphsonPF.getInitialState()]]
    *
    * @param index
    *   Node index
    * @param nodeType
    *   Type of the node
    * @param power
    *   Given apparent power
    * @param targetVoltage
    *   Target voltage magnitude
    */
  final case class PresetData(
      index: Int,
      nodeType: NodeType,
      power: Complex,
      targetVoltage: Double = 1.0,
      activePowerMin: Option[Double] = None,
      activePowerMax: Option[Double] = None,
      reactivePowerMin: Option[Double] = None,
      reactivePowerMax: Option[Double] = None
  ) extends NodeData

  case object PresetData {

    /** Extract a [[DenseVector]] of nodal powers from an Array of
      * [[PresetData]]
      *
      * @param presetDatas
      *   Array of given preset information
      * @return
      *   Vector of complex nodal voltages
      */
    def extractPowerVector(
        presetDatas: Array[PresetData]
    ): DenseVector[Complex] = {
      DenseVector(
        presetDatas
          .foldLeft(Array.empty[Complex])((array, data) => array :+ data.power)
      )
    }
  }

  /** Representing the current state of the node
    *
    * @param index
    *   Node index
    * @param nodeType
    *   Type of the node
    * @param voltage
    *   Complex nodal voltage
    * @param power
    *   Apparent power (based on the voltage and grid structure; perspective:
    *   grid)
    */
  final case class StateData(
      index: Int,
      nodeType: NodeType,
      voltage: Complex,
      power: Complex
  ) extends NodeData

  case object StateData {
    def apply(presetData: PresetData): StateData = {
      val vGuess = new Complex(presetData.targetVoltage, 0d)
      new StateData(
        presetData.index,
        presetData.nodeType,
        vGuess,
        presetData.power
      )
    }

    /** Extract a [[DenseVector]] of nodal voltages from an Array of
      * [[StateData]]
      *
      * @param state
      *   Array of state information
      * @return
      *   Vector of complex nodal powers
      */
    def extractVoltageVector(state: Array[StateData]): DenseVector[Complex] = {
      DenseVector(
        state.foldLeft(Array.empty[Complex])((array, data) =>
          array :+ data.voltage
        )
      )
    }

    /** Extract a [[DenseVector]] of nodal powers from an Array of [[StateData]]
      *
      * @param state
      *   Array of given state information
      * @return
      *   Vector of complex nodal voltages
      */
    def extractPowerVector(state: Array[StateData]): DenseVector[Complex] = {
      DenseVector(
        state
          .foldLeft(Array.empty[Complex])((array, data) => array :+ data.power)
      )
    }
  }

  /** Representing the deviation between an [[PresetData]] and the information
    * resulting from [[StateData]]
    *
    * @param index
    *   Node index
    * @param nodeType
    *   Type of the node
    * @param power
    *   Deviation in nodal power
    * @param squaredVoltageMagnitude
    *   Deviation in squared voltage magnitude
    */
  final case class DeviationData(
      index: Int,
      nodeType: NodeType,
      power: Complex,
      squaredVoltageMagnitude: Double
  )

}
