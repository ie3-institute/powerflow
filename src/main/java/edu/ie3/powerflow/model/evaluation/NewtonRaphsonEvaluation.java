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
 * This class holds all information to assess the success of a Newton Raphson power flow calculation
 *
 * @author Kittl
 * @since 22.10.2018
 */
public class NewtonRaphsonEvaluation extends Throwable implements AlgorithmEvaluation {
  private static final long serialVersionUID = 5177719754857936523L;

  /* Solved successfully? */
  private final boolean successful;

  /* Evaluate the single iterations */
  private final LinkedList<PowerFlowEvaluation> iterations;

  /* Convergence threshold */
  private final double epsilon;

  /* Duration of the process step */
  private final Duration duration;

  public NewtonRaphsonEvaluation(
      boolean successful,
      LinkedList<PowerFlowEvaluation> iterations,
      double epsilon,
      Duration duration) {
    this.successful = successful;
    this.iterations = iterations;
    this.epsilon = epsilon;
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
        .append("NewtonRaphsonEvaluation [successful = ")
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
