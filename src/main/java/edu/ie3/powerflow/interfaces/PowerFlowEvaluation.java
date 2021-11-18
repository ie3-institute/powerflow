/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.interfaces;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Interface to describe the general functionality of an arbitrary power flow evaluation
 *
 * @author Kittl
 * @since 24.10.2018
 */
public interface PowerFlowEvaluation extends Serializable {
  /**
   * Has the given power flow process step been successful?
   *
   * @return true if the step has been calculated successfully
   */
  boolean isSuccessful();

  /**
   * Give a debug string
   *
   * @return A string for debugging purposes
   */
  String toDebugString();

  /**
   * Get the subordinate iterations
   *
   * @return The evaluations of the subordinate iterations
   */
  LinkedList<PowerFlowEvaluation> getIterations();
}
