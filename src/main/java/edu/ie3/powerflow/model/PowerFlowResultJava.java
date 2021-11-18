/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model;

import edu.ie3.powerflow.interfaces.PowerFlowEvaluation;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.complex.Complex;

/**
 * This is the output of the power flow calculation TODO: Improve in terms of debugging classes
 *
 * @author roemer
 * @version 0.1
 * @since 20.09.2017
 */
public class PowerFlowResultJava<E extends PowerFlowEvaluation> {
  private final boolean valid;
  private final E evaluation;
  private final Complex[] v;
  private final Complex[] iteratedS;
  private final Complex slackS;

  public PowerFlowResultJava(
      boolean valid, Complex[] v, Complex[] iteratedS, E evaluation, Complex slackS) {
    this.valid = valid;
    this.evaluation = evaluation;
    this.v = v;
    this.iteratedS = iteratedS;
    this.slackS = slackS;
  }

  /**
   * Dummy constructor for an result object returned by a non successful calculation
   *
   * @param evaluation Evaluation of what happend
   */
  public PowerFlowResultJava(E evaluation) {
    this.valid = false;
    this.v = new Complex[0];
    this.iteratedS = new Complex[0];
    this.slackS = new Complex(0d);
    this.evaluation = evaluation;
  }

  public E getEvaluation() {
    return evaluation;
  }

  public Complex[] getV() {
    return v;
  }

  public Complex[] getIteratedS() {
    return iteratedS;
  }

  public Complex getSlackS() {
    return slackS;
  }

  public boolean isValid() {
    return valid;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(31, 1).append(valid).append(iteratedS).append(v).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PowerFlowResultJava other = (PowerFlowResultJava) obj;
    return new EqualsBuilder()
        .append(this.v, other.v)
        .append(this.iteratedS, other.iteratedS)
        .append(this.evaluation, other.evaluation)
        .isEquals();
  }

  @Override
  public String toString() {
    return "PowerFlowResult [valid = "
        + valid
        + ", v="
        + Arrays.stream(v).map(Complex::toString).collect(Collectors.joining(", "))
        + ", iteratedS="
        + Arrays.stream(iteratedS).map(Complex::toString).collect(Collectors.joining(", "))
        + ", slackS = "
        + slackS
        + ", evaluation="
        + evaluation
        + "]";
  }

  public String toDebugString() {
    return "PowerFlow[valid = " + valid + "]";
  }
}
