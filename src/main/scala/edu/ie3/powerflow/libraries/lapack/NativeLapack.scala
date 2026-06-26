/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries.lapack

import edu.ie3.powerflow.libraries.Native

import java.lang.foreign.Arena
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
    // using == try-with-resource
    withArena { arena =>
      given Arena = arena

      // allocate memory and set values
      val bPtr = b.toStack

      // call lapack function
      dgesvHandle.invoke(
        n.asPtr,
        nrhs.asPtr,
        a.toStack,
        lda.asPtr,
        ipiv.toStack,
        bPtr,
        ldb.asPtr,
        info.asPtr,
      )

      bPtr.toArray(JAVA_DOUBLE)
    }
  }

}
