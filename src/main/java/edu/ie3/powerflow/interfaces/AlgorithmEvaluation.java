/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.interfaces;

import java.time.Duration;

/**
 * Evaluating the performance of a given algorithm
 *
 * @author Kittl
 * @since 23.11.2018
 */
public interface AlgorithmEvaluation extends PowerFlowEvaluation {

  /**
   * Convergence threshold
   *
   * @return The convergence threshol
   */
  double getEpsilon();

  /**
   * Get the duration of the given algorithm procession
   *
   * @return The {@link Duration}
   */
  Duration getDuration();
}
