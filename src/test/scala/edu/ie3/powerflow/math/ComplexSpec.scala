/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import edu.ie3.powerflow.math.Complex.i
import edu.ie3.test.common.UnitSpec

class ComplexSpec extends UnitSpec {

  "A Complex number" should {
    "add a complex number correctly" in {
      val base = Complex(1, 3)
      val res = base + Complex(5, 1)
      res shouldBe Complex(6, 4)
    }

    "add an integer number correctly" in {
      val base = Complex(1, 3)
      val res = base + 4
      res shouldBe Complex(5, 3)
    }

    "add a float number correctly" in {
      val base = Complex(1, 3)
      val res = base + 1.5f
      res shouldBe Complex(2.5, 3)
    }

    "add a double number correctly" in {
      val base = Complex(1, 3)
      val res = base + 2.0
      res shouldBe Complex(3, 3)
    }

    "subtract a complex number correctly" in {
      val base = Complex(1, 3)
      val res = base - Complex(5, 1)
      res shouldBe Complex(-4, 2)
    }

    "subtract an integer number correctly" in {
      val base = Complex(1, 3)
      val res = base - 4
      res shouldBe Complex(-3, 3)
    }

    "subtract a float number correctly" in {
      val base = Complex(1, 3)
      val res = base - 1.5f
      res shouldBe Complex(-0.5, 3)
    }

    "subtract a double number correctly" in {
      val base = Complex(1, 3)
      val res = base - 2.0
      res shouldBe Complex(-1, 3)
    }

    "multiply a complex number correctly" in {
      val base = Complex(1, 3)
      val res = base * Complex(5, 1)
      res shouldBe Complex(2, 16)
    }

    "multiply an integer number correctly" in {
      val base = Complex(1, 3)
      val res = base * 4
      res shouldBe Complex(4, 12)
    }

    "multiply a float number correctly" in {
      val base = Complex(1, 3)
      val res = base * 1.5f
      res shouldBe Complex(1.5, 4.5)
    }

    "multiply a double number correctly" in {
      val base = Complex(1, 3)
      val res = base * 2.0
      res shouldBe Complex(2, 6)
    }

    "be divided by a complex number correctly" in {
      val base = Complex(1, 3)
      val res = base / Complex.i
      res shouldBe Complex(3, -1)
    }

    "be divided by an integer number correctly" in {
      val base = Complex(1, 3)
      val res = base / 4
      res shouldBe Complex(0.25, 0.75)
    }

    "be divided by a float number correctly" in {
      val base = Complex(1, 3)
      val res = base / 1.5f
      res.real shouldBe 0.6666666666666666 +- 1e-3
      res.imag shouldBe 2.0
    }

    "be divided by a double number correctly" in {
      val base = Complex(1, 3)
      val res = base / 2.0
      res shouldBe Complex(0.5, 1.5)
    }

    "perform an unary negate correctly" in {
      val base = Complex(1, 3)
      -base shouldBe Complex(-1, -3)
    }

    "return the absolute value correctly" in {
      val base = Complex(1, 3)
      base.abs shouldBe math.sqrt(10)
    }

    "be conjugated correctly" in {
      val base = Complex(1, 3)
      base.conjugate shouldBe Complex(1, -3)
    }
  }

  "An integer" should {
    "be converted into a complex number correctly" in {
      1.i shouldBe Complex(0, 1)
    }

    "add a complex number correctly" in {
      1 + Complex(1, 3) shouldBe Complex(2, 3)
    }

    "subtract a complex number correctly" in {
      1 - Complex(1, 3) shouldBe Complex(0, -3)
    }

    "multiply a complex number correctly" in {
      1 * Complex(1, 3) shouldBe Complex(1, 3)
    }

    "be divided by a complex number correctly" in {
      1 / Complex(1, 3) shouldBe Complex(0.1, -0.3)
    }
  }

  "A float" should {
    "be converted into a complex number correctly" in {
      1.5f.i shouldBe Complex(0, 1.5)
    }

    "add a complex number correctly" in {
      1.5f + Complex(1, 3) shouldBe Complex(2.5, 3)
    }

    "subtract a complex number correctly" in {
      1.5f - Complex(1, 3) shouldBe Complex(0.5, -3)
    }

    "multiply a complex number correctly" in {
      1.5f * Complex(1, 3) shouldBe Complex(1.5, 4.5)
    }

    "be divided by a complex number correctly" in {
      1.5f / Complex(1, 3) shouldBe Complex(0.15, -0.45)
    }
  }

  "A double" should {
    "be converted into a complex number correctly" in {
      2.0.i shouldBe Complex(0, 2.0)
    }

    "add a complex number correctly" in {
      2.0 + Complex(1, 3) shouldBe Complex(3, 3)
    }

    "subtract a complex number correctly" in {
      2.0 - Complex(1, 3) shouldBe Complex(1, -3)
    }

    "multiply a complex number correctly" in {
      2.0 * Complex(1, 3) shouldBe Complex(2, 6)
    }

    "be divided by a complex number correctly" in {
      2.0 / Complex(1, 3) shouldBe Complex(0.2, -0.6)
    }
  }

}
