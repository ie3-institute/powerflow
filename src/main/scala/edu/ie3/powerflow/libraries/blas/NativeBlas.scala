/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.blas

import edu.ie3.powerflow.libraries.Native

import java.lang.foreign.FunctionDescriptor.ofVoid
import java.lang.foreign.ValueLayout.*

final case class NativeBlas(
    override val libName: String
) extends Blas
    with Native {

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
    // allocate memory and set values
    val yPtr = y.toStack

    dgemvHandle.invoke(
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

  }

}
