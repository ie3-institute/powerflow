/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.lapack

import org.netlib.lapack.Dgesv
import org.netlib.util.intW

object JavaLapack extends Lapack {

  override def dgesv(
      n: Int,
      nrhs: Int,
      a: Array[Double],
      lda: Int,
      ipiv: Array[Int],
      b: Array[Double],
      ldb: Int,
      info: Int,
  ): Array[Double] = {
    Dgesv.dgesv(n, nrhs, a, 0, lda, ipiv, 0, b, 0, ldb, new intW(info))
    b
  }

}
