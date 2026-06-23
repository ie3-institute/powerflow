/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.lapack

import edu.ie3.powerflow.libraries.Native

import java.lang.foreign.FunctionDescriptor.ofVoid
import java.lang.foreign.ValueLayout.*

final case class NativeLapack(
    override val libName: String
) extends Lapack
    with Native {

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
    // allocate memory and set values
    val bInMemory = b.toStack

    // call lapack function
    dgesvHandle.invoke(
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
  }

}
