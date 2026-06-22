/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import org.netlib.lapack.Dgesv
import org.netlib.util.intW

import java.lang.foreign.FunctionDescriptor.ofVoid
import java.lang.foreign.ValueLayout.{ADDRESS, JAVA_DOUBLE}
import scala.util.Try

/** Object that defines all supported LAPACK functions.
  */
object LAPACK extends Native with MemoryManagementUtils {

  override def getLibrary: Try[String] = Try(System.getProperty("lapackLib"))
    .orElse(Try(System.getProperty("lapackPath")))

  private lazy val dgesvHandle = buildHandle(
    "dgesv_",
    ofVoid(
      ADDRESS,
      ADDRESS,
      ADDRESS,
      ADDRESS,
      ADDRESS,
      ADDRESS,
      ADDRESS,
      ADDRESS,
    ),
  )

  /** Method for solving real double precision matrices.
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
  ): Array[Double] = dgesvHandle match {
    case Some(native) =>
      // allocate memory and set values
      val bInMemory = b.toStack

      // call lapack function
      native.invoke(
        n.asPtr,
        nrhs.asPtr,
        a.toStack,
        lda.asPtr,
        ipiv.toStack,
        bInMemory,
        ldb.asPtr,
        info.asPtr,
      )

      bInMemory.toArray(JAVA_DOUBLE)

    case None =>
      Dgesv.dgesv(n, nrhs, a, 0, lda, ipiv, 0, b, 0, ldb, new intW(info))
      b
  }
}
