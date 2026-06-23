/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.blas

import org.netlib.blas.Dgemv

object JavaBlas extends Blas {

  override def dgemv(
      trans: String,
      m: Int,
      n: Int,
      alpha: Double,
      a: Array[Double],
      lda: Int,
      x: Array[Double],
      incx: Int,
      beta: Double,
      y: Array[Double],
      incy: Int,
  ): Array[Double] = {
    Dgemv.dgemv(trans, m, n, alpha, a, 0, lda, x, 0, incx, beta, y, 0, incy)
    y
  }

}
