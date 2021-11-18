/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.interfaces;

import edu.ie3.powerflow.model.PowerFlowResultJava;
import edu.ie3.powerflow.util.exceptions.PowerFlowException;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.FieldMatrix;

/**
 * Interface to describe the basic functionalities that a power flow algorithm has to implement.
 *
 * @author Kittl
 * @since 26.10.2018
 */
public interface PowerFlowAlgorithm {

  /**
   * Get the applied convergence threshold
   *
   * @return The convergence threshold
   */
  double getEpsilon();

  /**
   * Set the applied convergence threshold
   *
   * @param epsilon The convergence threshold
   */
  void setEpsilon(double epsilon);

  /**
   * Get the used admittance matrix
   *
   * @return The admittance matrix
   */
  FieldMatrix<Complex> getAdmittanceMatrix();

  /**
   * Set the used admittance matrix
   *
   * @param admittanceMatrix The admittance matrix
   * @throws PowerFlowException If the matrix is malformed
   */
  void setAdmittanceMatrix(FieldMatrix<Complex> admittanceMatrix) throws PowerFlowException;

  /**
   * Calculate the result for a given situation {@code actualS} with a default vector of start
   * voltages. The capability to alter the power behaviour at the nodes is set to zero.
   *
   * @param actualS Current nodal residual apparent power
   * @return The result of the calculation
   * @throws PowerFlowException If something is fucked up
   */
  PowerFlowResultJava calc(Complex[] actualS) throws PowerFlowException;

  /**
   * Calculate the result for a given situation {@code actualS} with a default vector of start
   * voltages
   *
   * @param actualS Current nodal residual apparent power
   * @param decrS Capability to alter the power at the nodes in a negative direction
   * @param incrS Capability to alter the power at the nodes in a positive direction
   * @return The result of the calculation
   * @throws PowerFlowException If something is fucked up
   */
  PowerFlowResultJava calc(Complex[] actualS, Complex[] decrS, Complex[] incrS)
      throws PowerFlowException;

  /**
   * Calculate the result for a given situation {@code actualS} with a given vector of start
   * voltages {@code v}. The capability to alter the power behaviour at the nodes is set to zero.
   *
   * @param actualS Current nodal residual apparent power
   * @param v Current nodal voltages
   * @return The result of the calculation
   * @throws PowerFlowException If something is fucked up
   */
  PowerFlowResultJava calc(Complex[] actualS, Complex[] v) throws PowerFlowException;

  /**
   * Calculate the result for a given situation {@code actualS} with a given vector of start
   * voltages {@code v}
   *
   * @param actualS Current nodal residual apparent power
   * @param decrS Capability to alter the power at the nodes in a negative direction
   * @param incrS Capability to alter the power at the nodes in a positive direction
   * @param v Current nodal voltages
   * @return The result of the calculation
   * @throws PowerFlowException If something is fucked up
   */
  PowerFlowResultJava calc(Complex[] actualS, Complex[] decrS, Complex[] incrS, Complex[] v)
      throws PowerFlowException;

  /**
   * Set the target voltages at each node
   *
   * @param vTarget Vector of target voltages at each node
   */
  void setvTarget(Complex[] vTarget);

  /**
   * Get the target voltages at each node
   *
   * @return Vector of target voltages at each node
   */
  Complex[] getvTarget();
}
