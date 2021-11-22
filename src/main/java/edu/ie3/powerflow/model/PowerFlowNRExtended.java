/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model;

import edu.ie3.powerflow.util.exceptions.PowerFlowException;
import edu.ie3.util.ArrayHelper;
import java.util.Arrays;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended Newton Raphson power flow algorithm, guessing the start vector of nodal voltages based
 * on the last known Jacobian matrix and the last nodal power consumption. If the standard {@code
 * calc} method is used, the last known system state is NOT reused.
 *
 * @author Kittl
 * @since 26.10.2018
 * @deprecated This class needs refactoring. Therefore, it is marked as deprecated.
 */
@Deprecated
public class PowerFlowNRExtended extends PowerFlowNR {

  private static final Logger logger = LoggerFactory.getLogger("powerFlowEvaluation");

  private static final double DEVIATION_WARN_THRESHOLD = 0.1;

  private Complex[] lastIteratedS = null;
  private Complex[] lastIteratedV = null;

  public PowerFlowNRExtended(double epsilon, int maxIterations) throws PowerFlowException {
    super(epsilon, maxIterations);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS) throws PowerFlowException {
    Complex[] v = new Complex[nodeCount];
    Complex[] decrS = new Complex[nodeCount];
    Complex[] incrS = new Complex[nodeCount];
    Arrays.fill(v, new Complex(1d, 0d));
    Arrays.fill(decrS, new Complex(0d, 0d));
    Arrays.fill(decrS, new Complex(0d, 0d));
    return calc(actualS, decrS, incrS, v, false);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS, Complex[] decrS, Complex[] incrS)
      throws PowerFlowException {
    Complex[] v = new Complex[nodeCount];
    Arrays.fill(v, new Complex(1d, 0d));
    return calc(actualS, decrS, incrS, v, false);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS, Complex[] v) throws PowerFlowException {
    Complex[] decrS = new Complex[nodeCount];
    Complex[] incrS = new Complex[nodeCount];
    Arrays.fill(decrS, new Complex(0d, 0d));
    Arrays.fill(decrS, new Complex(0d, 0d));
    return calc(actualS, decrS, incrS, v, false);
  }

  @Override
  public PowerFlowResultJava calc(Complex[] actualS, Complex[] decrS, Complex[] incrS, Complex[] v)
      throws PowerFlowException {
    return calc(actualS, decrS, incrS, v, false);
  }

  /**
   * Calculate the power flow situation based on the well known Newton Raphson algorithm. The last
   * known system state is recycled based on the decision taken with {@code foreceRefresh}.
   *
   * @param actualS Vector of actual and current nodal power consumption
   * @param decrS Capability to alter the power at the nodes in a negative direction
   * @param incrS Capability to alter the power at the nodes in a positive direction
   * @param v Externally given voltage vector
   * @param forceFlatStart Force not to reuse the last known system state, when set to {@code true}.
   * @return The calculated {@link PowerFlowResultJava}
   * @throws PowerFlowException if somethings goes wrong
   */
  public PowerFlowResultJava calc(
      Complex[] actualS, Complex[] decrS, Complex[] incrS, Complex[] v, boolean forceFlatStart)
      throws PowerFlowException {
    if (actualS == null)
      throw new PowerFlowException("The array of actual power consumption may not be null!");
    if (actualS.length != nodeCount)
      throw new PowerFlowException(
          "The amount of nodal powers does not match the amount of nodes covered by the admittance matrix.");

    PowerFlowResultJava result;
    if (jacobianMatrix == null
        || lastIteratedS == null
        || lastIteratedS.length != actualS.length
        || admittanceMatrixChanged
        || forceFlatStart
        || v != null) {
      logger.debug("Cannot reuse the information from last iteration...");
      /*
       * There is no chance to reuse the last calculation or the user wished not to do so (either a voltage vector
       * is given or the user forces a flatStart)
       */
      result = super.calc(actualS, decrS, incrS, forceFlatStart ? null : v);
    } else {
      /* Reuse the last known Jacobian matrix as well as the last known power consumption */
      Complex[] deltaS = ArrayHelper.add(actualS, lastIteratedS);
      for (int cnt = 0; cnt < actualS.length; cnt++) {
        if (deltaS[cnt].abs() / lastIteratedS[cnt].abs() > DEVIATION_WARN_THRESHOLD) {
          logger.warn(
              "There is at least one nodal power value deviating more than "
                  + DEVIATION_WARN_THRESHOLD * 100
                  + " % from the previously one. Maybe the new initial guess is not good either. Force a flat start.");
          return super.calc(actualS, decrS, incrS, null);
        }
      }

      double[] targetV2 =
          ArrayHelper.pow(
              Arrays.stream(super.getvTarget()).map(Complex::abs).toArray(Double[]::new), 2d);
      double[] lastIteratedV2 =
          ArrayHelper.pow(
              Arrays.stream(lastIteratedV).map(Complex::abs).toArray(Double[]::new), 2d);
      double[] deltaV2 = ArrayHelper.subtract(lastIteratedV2, targetV2);
      RealVector delta = reduceDeviationVector(deltaS, deltaV2);
      ArrayRealVector startGuessDev = solveSystemOfEquations(jacobianMatrix, delta);
      Complex[] startGuess = applyCorrectionToVoltages(lastIteratedV, startGuessDev);

      for (int cnt = 0; cnt < lastIteratedV.length; cnt++) {
        if (Math.abs((startGuess[cnt].getArgument() / lastIteratedV[cnt].getArgument()) - 1)
            > DEVIATION_WARN_THRESHOLD) {
          logger.warn(
              "There is at least one nodal voltage angle deviating more than "
                  + DEVIATION_WARN_THRESHOLD * 100
                  + " % from the previously one. Maybe the new initial guess is not good either.");
          break;
        }

        if (Math.abs((startGuess[cnt].abs() / lastIteratedV[cnt].abs()) - 1)
            > DEVIATION_WARN_THRESHOLD) {
          logger.warn(
              "There is at least one nodal voltage magnitude value deviating more than "
                  + DEVIATION_WARN_THRESHOLD * 100
                  + " % from the previously one. Maybe the new initial guess is not good either. Force a flat start.");
          return super.calc(actualS, decrS, incrS, null);
        }
      }

      result = super.calc(actualS, decrS, incrS, startGuess);
    }

    lastIteratedS = Arrays.copyOf(result.getIteratedS(), result.getIteratedS().length);
    lastIteratedV = Arrays.copyOf(result.getV(), result.getV().length);

    return result;
  }
}
