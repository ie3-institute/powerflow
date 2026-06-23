/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.lapack

import edu.ie3.powerflow.libraries.LibraryLoader
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/** Trait that defines all supported LAPACK methods.
  */
trait Lapack {

  /** Method for solving real double precision matrices.
    *
    * @param n
    *   The number of linear equations (matrix rows).
    * @param nrhs
    *   The number of right hand sides (columns of b)
    * @param a
    *   The matrix A as a linear array (column major is assumed).
    * @param lda
    *   The leading dimension of matrix A (normally n).
    * @param ipiv
    *   An array for pivot indices.
    * @param b
    *   The vector b or matrix B (column major is assumed).
    * @param ldb
    *   The leading dimension of b.
    * @param info
    *   Integer holding some information.
    * @return
    *   An array with the solution.
    */
  def dgesv(
      n: Int,
      nrhs: Int,
      a: Array[Double],
      lda: Int,
      ipiv: Array[Int],
      b: Array[Double],
      ldb: Int,
      info: Int,
  ): Array[Double]

}

object Lapack {

  val lib: Lapack = {
    val overridePath = Try(System.getProperty("lapack")).toOption

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

    LibraryLoader.load(overridePath, fallback, NativeLapack.apply, JavaLapack)
  }
}
