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
 * This class holds all information necessary to evaluate the performance of an inner slack
 * iteration
 *
 * @author Kittl
 * @since 22.10.2018
 */
public class SlackBalancingEvaluation extends Throwable implements AlgorithmEvaluation {
  private static final long serialVersionUID = -1229315399875010047L;

  private final boolean successful;

  private final double epsilon;

  private final LinkedList<PowerFlowEvaluation> iterations;

  private final Duration duration;

  public SlackBalancingEvaluation(
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
  public boolean isSuccessful() {
    return successful;
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
  public LinkedList<PowerFlowEvaluation> getIterations() {
    return iterations;
  }

  @Override
  public String toDebugString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
        .append("SlackBalancingEvaluation [successful = ")
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
