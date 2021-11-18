/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.model.enums;

/**
 * This enum serves as a reference in which condition the power flow calculation has finished.
 *
 * @author Kittl
 * @since 02.02.2018
 */
public enum PowerFlowCondition {
  /** The calculation has finished successfully */
  FINAL,
  /** The calculation has finished in an error stat */
  FAILURE
}
