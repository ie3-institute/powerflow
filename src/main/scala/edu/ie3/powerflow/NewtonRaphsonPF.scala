/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.math.Complex
import breeze.numerics.{abs, pow}
import com.typesafe.scalalogging.LazyLogging
import edu.ie3.powerflow.model.FailureCause.{
  CalculationFailed,
  MaxIterationsReached,
}
import edu.ie3.powerflow.model.NodeData.{DeviationData, PresetData, StateData}
import edu.ie3.powerflow.model.PowerFlowResult.FailedPowerFlowResult.FailedNewtonRaphsonPFResult
import edu.ie3.powerflow.model.PowerFlowResult.SuccessFullPowerFlowResult.ValidNewtonRaphsonPFResult
import edu.ie3.powerflow.model.StartData.{
  WithForcedStartVoltages,
  WithLastState,
}
import edu.ie3.powerflow.model.enums.NodeType
import edu.ie3.powerflow.model.{
  JacobianMatrix,
  NodeData,
  PowerFlowResult,
  StartData,
}
import edu.ie3.powerflow.util.exceptions.PowerFlowException

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** Representation of the Newton-Raphson algorithm to solve the system of
  * non-linear equations, that do describe a power grid.
  *
  * ATTENTION: It is the users responsibility, that the data fed in here and in
  * the calculate method actually does fit together in terms of the node
  * indices. A sanity check is done when feeding something in, but it is not
  * possible to detect all logical failures.
  *
  * @param epsilon
  *   Permissible deviation between actual (given) use of the grid (P, Q, |V|)
  *   and those values determined during iterative solving of the system of
  *   equations
  * @param maxIterations
  *   Maximum amount of iterations
  * @param admittanceMatrix
  *   The dimensionless admittance matrix describing the grid structure
  * @param intendedIndexOrder
  *   A Vector of node indices denoting the intended order of nodes being use
  *   for sanity checks
  */
@SerialVersionUID(1L)
final case class NewtonRaphsonPF(
    epsilon: Double,
    maxIterations: Integer,
    admittanceMatrix: DenseMatrix[Complex],
    intendedIndexOrder: Option[Vector[Int]] = None,
) extends LazyLogging {
  if admittanceMatrix.rows != admittanceMatrix.cols then
    throw new PowerFlowException("Have a square matrix ready!")
  intendedIndexOrder match {
    case Some(intendedOrder) if intendedOrder.length != admittanceMatrix.cols =>
      throw new PowerFlowException(
        "The intended amount of nodes does not fit the dimensions of the admittance matrix!"
      )
    case None =>
      logger.debug(
        "You did not provide an intended node ordering, therefore no sanity checks will be performed"
      )
    case _ =>
  }

  /** Do calculate the final power flow result of the given operation point
    * under consideration of the given external initialisation data. The single
    * node results' power field does contain the nodal residual power calculated
    * with the nodal current sum based on the admittance matrix and the given
    * result voltages.
    *
    * ATTENTION: The ordering in initData has to meet the order of the
    * admittance matrix provided with the constructor!
    *
    * @param operationPointUnordered
    *   The given operation point, with which the grid is used
    * @param initData
    *   Externally provided initialisation data, see [[calculate()]] for details
    *   on which data can be provided
    * @return
    *   The result of the calculation based as [[PowerFlowResult]]
    */
  def calculate(
      operationPointUnordered: Array[PresetData],
      initData: Option[StartData] = None,
  ): PowerFlowResult = {
    val operationPoint =
      NodeData.correctOrder(operationPointUnordered, intendedIndexOrder)
    val startDataUnordered: Array[StateData] =
      NewtonRaphsonPF.getInitialState(operationPoint, initData)
    val startData =
      NodeData.correctOrder(startDataUnordered, intendedIndexOrder)

    /* Solve the inner iterations recursively */
    solveIterationStepsRecursively(0, operationPoint, startData)
  }

  /** Solving the iteration steps recursively
    *
    * @param iterationCount
    *   Current number of iterations
    * @param operationPoint
    *   Given operation point, with which the grid is used
    * @param lastState
    *   Last known state of the grid
    * @return
    *   [[PowerFlowResult]] of that calculation
    */
  @tailrec
  private def solveIterationStepsRecursively(
      iterationCount: Integer = 0,
      operationPoint: Array[PresetData],
      lastState: Array[StateData],
  ): PowerFlowResult = {
    /* calculate nodal voltage magnitude and apparent power deviation */
    val lastStateWithIterationPower = NewtonRaphsonPF
      .calculateIterationApparentPower(lastState, admittanceMatrix)
    val iterationPower =
      StateData.extractPowerVector(lastStateWithIterationPower)
    val iterationVoltage = StateData.extractVoltageVector(lastState)
    val intermediateOperationPoint = NewtonRaphsonPF
      .composeIntermediateOperationPoint(operationPoint, iterationPower)
      .getOrElse(operationPoint)
    val nodalDeviation =
      NewtonRaphsonPF.calculateDeviationToActualState(
        intermediateOperationPoint,
        iterationVoltage,
        iterationPower,
      )
    val deviationVector =
      NewtonRaphsonPF.buildCombinedDeviationVector(nodalDeviation)
    val converged = deviationVector.forall(abs(_) < epsilon)
    val jacobianMatrix =
      JacobianMatrix.buildJacobianMatrix(
        lastStateWithIterationPower,
        admittanceMatrix,
      )

    NewtonRaphsonPF.correctVoltages(
      lastStateWithIterationPower,
      deviationVector,
      jacobianMatrix,
    ) match {
      case Some(correctedState) if converged =>
        logger.debug(
          s"Power flow calculation converged in iteration $iterationCount"
        )
        ValidNewtonRaphsonPFResult(
          iterationCount,
          correctedState,
          jacobianMatrix,
        )
      case Some(correctedState)
          if !converged && iterationCount < maxIterations =>
        solveIterationStepsRecursively(
          iterationCount + 1,
          operationPoint,
          correctedState,
        )
      case Some(_) if !converged & iterationCount >= maxIterations =>
        logger.debug(
          s"Power flow finally did not converge after $iterationCount iterations"
        )
        FailedNewtonRaphsonPFResult(iterationCount, MaxIterationsReached)
      case None =>
        logger.debug(
          s"Voltages cannot be corrected in iteration $iterationCount"
        )
        FailedNewtonRaphsonPFResult(iterationCount, CalculationFailed)
    }
  }
}

/** The companion object embodying the general functionality of such kind of a
  * class
  */
case object NewtonRaphsonPF extends LazyLogging {

  /** Determine initial state of the grid 1) If start data has been provided and
    * if it is a 1.1.1) forced start vector of voltages --> Take them 1.1.2)
    * only the voltage at the slack node is provided --> Do a flat start with
    * the given slack voltage 1.2) last known state --> Determine the start
    * vector 2) If nothing has been given --> flat start
    *
    * NOTE: if 1.1.2) holds true, the provided [[PresetData.targetVoltage]] of
    * the provided operation point for the slack node will be overwritten!
    *
    * @param operationPoint
    *   The given operation point, with which the grid is used
    * @param initData
    *   Externally provided initialisation data
    * @return
    *   The initial state to be used for the iterative solution of the power
    *   flow equations
    */
  def getInitialState(
      operationPoint: Array[PresetData],
      initData: Option[StartData],
  ): Array[StateData] = {
    val nodeCount = operationPoint.length

    initData match {
      case Some(WithForcedStartVoltages(forcedState))
          if forcedState.length == 1 && forcedState(
            0
          ).nodeType == NodeType.SL =>
        /* Only the slack voltage is given. Flat Start for the rest. */
        val slState = forcedState(0)
        val candidateOperationPoint =
          NodeData.getByIndex(operationPoint, slState.index)
        if candidateOperationPoint.nodeType != NodeType.SL then
          throw new PowerFlowException(
            s"Received a forced start voltage for slack node at index" +
              s"${slState.index}, but the operation point at that index is no slack node."
          )

        /* Take the provided slack state at that index, otherwise prepare a flat start */
        operationPoint.map {
          case sl if sl.index == slState.index => slState
          case op                              => StateData(op)
        }

      case Some(WithForcedStartVoltages(forcedState))
          if forcedState.length == nodeCount =>
        /* All voltages are given --> take all of them */
        forcedState

      case Some(WithForcedStartVoltages(forcedState)) =>
        throw new PowerFlowException(
          s"A vector of start voltages with the wrong length has been provided." +
            s"Expected: $nodeCount, actual: ${forcedState.length}"
        )

      case Some(WithLastState(lastState, slVoltage, jacobianMatrix)) =>
        if jacobianMatrix.cols != (2 * nodeCount - 2) ||
          jacobianMatrix.rows != (2 * nodeCount - 2) ||
          lastState.length != nodeCount
        then {
          throw new PowerFlowException(
            s"A jacobian matrix and last known power vector has " +
              s"been provided with wrong dimension. Expected: $nodeCount, actual: " +
              s"(${jacobianMatrix.cols} x ${jacobianMatrix.rows}), ${lastState.length}"
          )
        }

        /* Update the slack voltage */
        val lastStateWithUpdatedSlackVoltage = lastState.map {
          case node if node.nodeType == NodeType.SL =>
            node.copy(voltage = slVoltage)
          case node => node.copy()
        }
        val lastStateVoltages =
          StateData.extractVoltageVector(lastStateWithUpdatedSlackVoltage)
        val lastStatePower = StateData
          .extractPowerVector(lastStateWithUpdatedSlackVoltage)
        val intermediateOperationPoint = NewtonRaphsonPF
          .composeIntermediateOperationPoint(operationPoint, lastStatePower)
          .getOrElse(operationPoint)
        val deviationToLastState = calculateDeviationToActualState(
          intermediateOperationPoint,
          lastStateVoltages,
          lastStatePower,
        )
        val deviationVector =
          NewtonRaphsonPF.buildCombinedDeviationVector(deviationToLastState)
        val correctedVoltagesOpt =
          NewtonRaphsonPF.correctVoltages(
            lastStateWithUpdatedSlackVoltage,
            deviationVector,
            jacobianMatrix,
          )

        correctedVoltagesOpt.getOrElse(operationPoint.map(op => StateData(op)))
      case None =>
        operationPoint.map(op => StateData(op))
    }
  }

  /** Calculate the nodal apparent power based of the inter-iteration nodal
    * voltages and the admittance matrix. The previous state is updated with the
    * calculated power.
    *
    * @param state
    *   The state that defines the residual power at the nodes
    * @param admittanceMatrix
    *   The admittance matrix of the grid
    * @return
    *   A vector of nodal apparent power
    */
  private def calculateIterationApparentPower(
      state: Array[StateData],
      admittanceMatrix: DenseMatrix[Complex],
  ): Array[StateData] = {
    val v = StateData.extractVoltageVector(state)
    val nodalPower = v *:* (admittanceMatrix * v).map(i => i.conjugate)
    (state zip nodalPower.toScalaVector).map(stateAndPowerPair =>
      stateAndPowerPair._1.copy(power = stateAndPowerPair._2 * -1)
    )
  }

  /** Does check for reactive power limit violations. If there is any violation
    * possible, it will compose a new operation point. If there is any violation
    * actually present, the node is transformed to an intermediate PQ node and
    * the reactive power is set to the violated limit.
    *
    * @param operationPoint
    *   Former operation point
    * @param iterationPower
    *   Vector of iterated power values
    * @return
    *   None, if no violation is possible, the new operation point, if checked
    */
  private def composeIntermediateOperationPoint(
      operationPoint: Array[PresetData],
      iterationPower: DenseVector[Complex],
  ): Option[Array[PresetData]] = {
    if operationPoint.forall(node =>
        node.nodeType != NodeType.PV || (node.reactivePowerMin.isEmpty && node.reactivePowerMax.isEmpty)
      )
    then None
    else {
      val intermediateOperationPoint =
        (operationPoint zip iterationPower.toScalaVector).map(
          nodeDataWithPower => {
            val nodeData = nodeDataWithPower._1
            val power = nodeDataWithPower._2
            nodeData.nodeType match {
              case NodeType.PV
                  if nodeData.reactivePowerMin.isDefined ||
                    nodeData.reactivePowerMax.isDefined =>
                val qMin =
                  nodeData.reactivePowerMin.getOrElse(Double.NegativeInfinity)
                val qMax =
                  nodeData.reactivePowerMin.getOrElse(Double.PositiveInfinity)
                if power.imag * -1 < qMin then {
                  logger.warn(
                    s"Minimum reactive power limit violated at node ${nodeData.index} " +
                      s"(${power.imag * -1} < $qMin). Set to PQ node and limit reactive power."
                  )
                  val newApparentPower = Complex(nodeData.power.real, qMin)
                  nodeData.copy(
                    nodeType = NodeType.PQ_INTERMEDIATE,
                    power = newApparentPower,
                  )
                } else if power.imag * -1 > qMax then {
                  logger.warn(
                    s"Maximum reactive power limit violated at node ${nodeData.index} " +
                      s"(${power.imag * -1} > $qMin). Set to PQ node and limit reactive power."
                  )
                  val newApparentPower = Complex(nodeData.power.real, qMax)
                  nodeData.copy(
                    nodeType = NodeType.PQ_INTERMEDIATE,
                    power = newApparentPower,
                  )
                } else nodeData
              case _ => nodeData
            }
          }
        )

      Some(intermediateOperationPoint)
    }
  }

  /** Calculate the deviation between actual apparent nodal powers and voltages
    * as well as the values given by the current iteration
    *
    * @param operationPoint
    *   Actual (given) operation point of the grid
    * @param iterationVoltages
    *   Nodal voltages in this iteration
    * @param iterationPowers
    *   Nodal apparent power in this iteration (from the perspective of the
    *   grid)
    * @return
    *   Array of [[PresetData]] with the given deviation per node
    */
  private def calculateDeviationToActualState(
      operationPoint: Array[PresetData],
      iterationVoltages: DenseVector[Complex],
      iterationPowers: DenseVector[Complex],
  ): Array[DeviationData] = {
    for (
      spec <- operationPoint;
      nodeType = spec.nodeType;
      index = spec.index;
      iterationVoltage = iterationVoltages(index);
      actualPower = spec.power;
      iterationPower = iterationPowers(index)
    ) yield {
      val powerDeviation = actualPower - iterationPower
      val squaredVoltageMagnitudeDeviation = pow(iterationVoltage.abs, 2) - pow(
        spec.targetVoltage,
        2,
      )

      DeviationData(
        index,
        nodeType,
        powerDeviation,
        squaredVoltageMagnitudeDeviation,
      )

    }
  }

  /** Build a [[DenseVector]] of nodal value deviations needed for solving the
    * linearised system of equations
    *
    * @param deviations
    *   Array of nodal deviations.
    * @return
    *   A composed [[DenseVector]] of a sub-vector with deviation in active,
    *   reactive power and voltage magnitude
    */
  private def buildCombinedDeviationVector(
      deviations: Array[DeviationData]
  ): DenseVector[Double] = {
    val pqvDeviationArrays = deviations.foldLeft(
      (Array.empty[Double], Array.empty[Double], Array.empty[Double])
    )((arrayTuple, currentEntry) => {
      currentEntry.nodeType match {
        case NodeType.PQ | NodeType.PQ_INTERMEDIATE =>
          (
            arrayTuple._1 :+ currentEntry.power.real,
            arrayTuple._2 :+ currentEntry.power.imag,
            arrayTuple._3,
          )
        case NodeType.PV =>
          (
            arrayTuple._1 :+ currentEntry.power.real,
            arrayTuple._2,
            arrayTuple._3 :+ currentEntry.squaredVoltageMagnitude,
          )
        case NodeType.SL => (arrayTuple._1, arrayTuple._2, arrayTuple._3)
      }
    })

    DenseVector[Double](
      pqvDeviationArrays._1 ++ pqvDeviationArrays._2 ++ pqvDeviationArrays._3
    )
  }

  /** Solve the linearised system of equations and apply the result onto the
    * lastly known voltages
    *
    * @param lastState
    *   The recently know state of the nodes
    * @param deviation
    *   An array of [[PresetData]] as the nodal information
    * @param jacobianMatrix
    *   The jacobian matrix.
    * @return
    *   A vector of corrected voltages
    */
  private def correctVoltages(
      lastState: Array[StateData],
      deviation: DenseVector[Double],
      jacobianMatrix: DenseMatrix[Double],
  ): Option[Array[StateData]] = {
    val nodeCount = lastState.length

    val correction: Try[DenseVector[Double]] = Try {
      jacobianMatrix \ deviation
    }.recoverWith {
      case e: IllegalArgumentException =>
        Failure(
          new Exception(
            s"Invalid dimensions for Jacobian/deviation. Details: ${e.getMessage}",
            e,
          )
        )
      case e if e.getMessage.toLowerCase.contains("singular") =>
        Failure(
          new Exception(
            s"Failed to solve Jacobian system: matrix may be singular. Details: ${e.getMessage}",
            e,
          )
        )
      case e =>
        Failure(
          new Exception(
            s"Unexpected error while solving Jacobian system: ${e.getMessage}",
            e,
          )
        )
    }

    correction match {
      case Success(correction) =>
        val deltaF = correction.slice(0, nodeCount - 1)
        val deltaE = correction.slice(nodeCount - 1, 2 * nodeCount - 2)
        val correctionComplex =
          (deltaE.toScalaVector zip deltaF.toScalaVector)
            .map(complexPair => Complex(complexPair._1, complexPair._2))

        val indicesOfPVPQnodes =
          lastState.zipWithIndex
            .filter(currentEntry => currentEntry._1.nodeType != NodeType.SL)
            .map(currentEntry => currentEntry._2)
        val indexToCorrection = indicesOfPVPQnodes zip correctionComplex

        if indexToCorrection.length != indicesOfPVPQnodes.length then
          throw new PowerFlowException(
            s"Some correction values got lost. Found ${indicesOfPVPQnodes.length} nodes to correct," +
              s" but only ${indexToCorrection.length} matches."
          )

        val correctionFilledUp = DenseVector.zeros[Complex](nodeCount)
        for correctionPair <- indexToCorrection do {
          correctionFilledUp.update(correctionPair._1, correctionPair._2)
        }
        val newVoltages =
          StateData.extractVoltageVector(lastState) - correctionFilledUp

        /* Build the corrected voltages */
        val nextState =
          lastState.zipWithIndex
            .foldLeft(Array.empty[StateData])(
              (nodeStateArray, lastNodeStateWithIndex) => {
                val index = lastNodeStateWithIndex._2
                val nodeState = lastNodeStateWithIndex._1

                nodeState.nodeType match {
                  case NodeType.SL =>
                    nodeStateArray :+ nodeState
                  case _ =>
                    nodeStateArray :+ nodeState.copy(
                      voltage = newVoltages(index)
                    )
                }
              }
            )
        Some(nextState)
      case Failure(exception) =>
        logger.error(
          s"Error while calculating correction: ${exception.getMessage}",
          exception,
        )
        None
    }
  }
}
