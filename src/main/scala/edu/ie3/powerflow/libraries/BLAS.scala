/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import org.netlib.blas.Dgemv

import java.lang.foreign.FunctionDescriptor.ofVoid
import java.lang.foreign.ValueLayout.{ADDRESS, JAVA_DOUBLE}
import scala.util.Try

/** Object that defines all supported BLAS functions.
  */
object BLAS extends Native with MemoryManagementUtils {

  override def getLibrary: Try[String] = Try(System.getProperty("blasLib"))
    .orElse(Try(System.getProperty("blasPath")))

  private lazy val dgemvHandle = buildHandle(
    "dgemv_",
    ofVoid(
      ADDRESS,
      ADDRESS,
      ADDRESS,
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

  /** Method for multiplying a real double precision matrix with a real double
    * precision vector.
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
  ): Array[Double] = dgemvHandle match {
    case Some(native) =>
      // allocate memory and set values
      val yPtr = y.toStack

      native.invoke(
        trans.asPtr,
        m.asPtr,
        n.asPtr,
        alpha.asPtr,
        a.toStack,
        lda.asPtr,
        x.toStack,
        incx.asPtr,
        beta.asPtr,
        yPtr,
        incy.asPtr,
      )

      yPtr.toArray(JAVA_DOUBLE)

    case None =>
      Dgemv.dgemv(trans, m, n, alpha, a, 0, lda, x, 0, incx, beta, y, 0, incy)
      y
  }

}
