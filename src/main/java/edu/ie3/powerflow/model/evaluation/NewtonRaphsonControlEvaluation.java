/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model.evaluation;

import edu.ie3.powerflow.interfaces.AlgorithmEvaluation;
import edu.ie3.powerflow.interfaces.PowerFlowEvaluation;
import java.time.Duration;
import java.util.LinkedList;

/**
 * This evaluation is created for outer control of different algorithms (like when relaxing the
 * convergence threshold or trying to use a different start vector)
 *
 * @author Kittl
 * @since 23.11.2018
 */
public class NewtonRaphsonControlEvaluation extends Throwable implements AlgorithmEvaluation {
  private static final long serialVersionUID = -5653559243427600949L;

  private final boolean successful;

  private final double epsilon;

  private final LinkedList<PowerFlowEvaluation> iterations;

  private final Duration duration;

  public NewtonRaphsonControlEvaluation(
      boolean successful,
      double epsilon,
      LinkedList<PowerFlowEvaluation> iterations,
      Duration duration) {
    this.successful = successful;
    this.epsilon = epsilon;
    this.iterations = iterations;
    this.duration = duration;
  }

  @Override
  public double getEpsilon() {
    return epsilon;
  }

  @Override
  public Duration getDuration() {
    return duration;
  }

  @Override
  public boolean isSuccessful() {
    return successful;
  }

  @Override
  public LinkedList<PowerFlowEvaluation> getIterations() {
    return iterations;
  }

  @Override
  public String toDebugString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
        .append("NewtonRaphsonControlEvaluation [successful = ")
        .append(successful)
        .append(", epsilon = ")
        .append(epsilon)
        .append(", duration = [")
        .append(duration)
        .append("], iterations = [\n\t");

    for (PowerFlowEvaluation iteration : iterations) {
      stringBuilder.append(iteration.toDebugString().replaceAll("\n", "\n\t"));
    }

    stringBuilder.append("]\n]\n");
    return stringBuilder.toString();
  }
}
