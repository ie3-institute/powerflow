/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model.evaluation;

import edu.ie3.powerflow.interfaces.PowerFlowEvaluation;
import java.util.LinkedList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class holds validation values to assess the performance of the inner loop of the Newton
 * Raphson algorithm
 *
 * @author Kittl
 * @since 22.10.2018
 */
public class NewtonRaphsonIterationEvaluation extends Throwable implements PowerFlowEvaluation {
  private static final long serialVersionUID = -8802829731218327744L;

  /* Iteration count */
  private final int count;

  /* Successfully solved? */
  private final boolean successful;

  /* Maximum value of the active power deviation */
  private final double deltaPMax;

  /* Maximum value of the reactive power deviation */
  private final double deltaQMax;

  /* Maximum value of the suquared voltage magnitude deviation */
  private final double deltaU2Max;

  /* Maximum value of the real part of the nodal voltage deviation */
  private final double deltaEMax;

  /* Maximum value of the imaginary part of the nodal voltage deviation */
  private final double deltaFMax;

  public NewtonRaphsonIterationEvaluation(
      int count,
      boolean successful,
      double deltaPMax,
      double deltaQMax,
      double deltaU2Max,
      double deltaEMax,
      double deltaFMax) {
    this.count = count;
    this.successful = successful;
    this.deltaPMax = deltaPMax;
    this.deltaQMax = deltaQMax;
    this.deltaU2Max = deltaU2Max;
    this.deltaEMax = deltaEMax;
    this.deltaFMax = deltaFMax;
  }

  public NewtonRaphsonIterationEvaluation(int count, boolean successful) {
    this.count = count;
    this.successful = successful;
    this.deltaPMax = Double.POSITIVE_INFINITY;
    this.deltaQMax = Double.POSITIVE_INFINITY;
    this.deltaU2Max = Double.POSITIVE_INFINITY;
    this.deltaEMax = Double.POSITIVE_INFINITY;
    this.deltaFMax = Double.POSITIVE_INFINITY;
  }

  public int getCount() {
    return count;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public double getDeltaPMax() {
    return deltaPMax;
  }

  public double getDeltaQMax() {
    return deltaQMax;
  }

  public double getDeltaU2Max() {
    return deltaU2Max;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(3, 21)
        .append(count)
        .append(successful)
        .append(deltaPMax)
        .append(deltaQMax)
        .append(deltaU2Max)
        .append(deltaEMax)
        .append(deltaFMax)
        .hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!(obj instanceof NewtonRaphsonIterationEvaluation)) return false;

    NewtonRaphsonIterationEvaluation other = (NewtonRaphsonIterationEvaluation) obj;

    return new EqualsBuilder()
        .append(other.count, this.count)
        .append(other.successful, this.successful)
        .append(other.deltaPMax, this.deltaPMax)
        .append(other.deltaU2Max, this.deltaU2Max)
        .append(other.deltaEMax, this.deltaEMax)
        .append(other.deltaFMax, this.deltaFMax)
        .isEquals();
  }

  @Override
  public String toString() {
    return "NewtonRaphsonIterationEvaluation [count="
        + count
        + ", successful="
        + successful
        + ", deltPMax="
        + String.format("%#.5e", deltaPMax)
        + ", deltaQMax="
        + String.format("%#.5e", deltaQMax)
        + ", deltaU2Max="
        + String.format("%#.5e", deltaU2Max)
        + ", deltaEMax = "
        + String.format("%#.5e", deltaEMax)
        + ", deltaFMax = "
        + String.format("%#.5e", deltaFMax)
        + "]";
  }

  public String toDebugString() {
    return toString() + "\n";
  }

  @Override
  public LinkedList getIterations() {
    return null;
  }
}
