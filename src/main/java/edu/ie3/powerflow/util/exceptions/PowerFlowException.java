/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.util.exceptions;

/**
 * @author Kittl
 * @since 07.12.2017
 */
public class PowerFlowException extends Exception {
  private static final long serialVersionUID = 6494961905626032837L;

  public PowerFlowException() {
    super();
  }

  public PowerFlowException(String message) {
    super(message);
  }

  public PowerFlowException(String message, Throwable cause) {
    super(message, cause);
  }

  public PowerFlowException(Throwable cause) {
    super(cause);
  }

  protected PowerFlowException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
