/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import edu.ie3.powerflow.math.DenseVector.filled
import edu.ie3.test.common.UnitSpec

class DenseVectorSpec extends UnitSpec {

  "A DenseVector" should {
    "be updated correctly" in {
      val vec = filled(3, 0d)
      vec.forall(_ == 0d) shouldBe true

      vec(2) = 5
      vec.forall(_ == 0d) shouldBe false

      vec(2) shouldBe 5
    }

    "use map correctly" in {
      val vec = filled(4, 0d)
      val updatedVec = vec.map(_ + Complex.i)
      updatedVec.forall(_ == Complex(0, 1))
    }

    "use foreach correctly" in {
      val vec = filled(4, 1d)
      val array = Array.fill(4)(1d)

      vec.foreach { case (idx, value) =>
        array(idx) -= value
      }

      array shouldBe Array.fill(4)(0d)
    }

  }

}
