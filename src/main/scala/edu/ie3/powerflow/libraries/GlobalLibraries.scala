package edu.ie3.powerflow.libraries

import dev.ludovic.netlib.blas.BLAS
import dev.ludovic.netlib.lapack.LAPACK

object GlobalLibraries {
  lazy val blas: BLAS = BLAS.getInstance()
  lazy val lapack: LAPACK = LAPACK.getInstance()


}
