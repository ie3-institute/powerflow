/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.blas

import edu.ie3.powerflow.libraries.LibraryLoader
import edu.ie3.powerflow.libraries.lapack.{JavaLapack, Lapack, NativeLapack}
import edu.ie3.powerflow.math.Complex

import scala.util.Try

/** Trait that contains all supported BLAS method.
  */
trait Blas {

  /** Method for multiplying a real double precision matrix with a real double
    * precision vector.
    *
    * @param trans
    *   Defines the operations to be performed.
    * @param m
    *   The number or rows of matrix A.
    * @param n
    *   The number of column of matrix A.
    * @param alpha
    *   The scalar of the matrix A.
    * @param a
    *   The matrix A (column major is assumed).
    * @param lda
    *   The first dimension of A.
    * @param x
    *   The vector that is multiplied.
    * @param incx
    *   Specifies the increment for the elements of x.
    * @param beta
    *   The scalar used for the vector y.
    * @param y
    *   THe vector y that is added to the product of A and x.
    * @param incy
    *   Specifies the increment for the elements of y.
    * @return
    */
  def dgemv(
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
  ): Array[Double]

}

object Blas {

  val lib: Blas = {
    val overridePath = Try(System.getProperty("blas")).toOption

    val fallback = {
      val osName = System.getProperty("os.name").toLowerCase()
      if osName == null || osName.isEmpty then
        throw new RuntimeException("Unable to determine operating system.")

      if osName.contains("win") then {
        "libopenblas.dll"
      } else if osName.contains("mac") then {
        "/System/Library/Frameworks/Accelerate.framework/Accelerate"
      } else {
        "liblapack.so.3"
      }
    }

    LibraryLoader.load(overridePath, fallback, NativeBlas.apply, JavaBlas)
  }
}
