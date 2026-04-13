/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import scala.math.floor

/** Immutable complex number.
  * @param real
  *   The real part of this complex number.
  * @param imag
  *   The imaginary part of this complex number.
  */
case class Complex(real: Double, imag: Double) {
  override def toString: String = s"$real + ${imag}i"

  def +(that: Complex) = Complex(real + that.real, imag + that.imag)

  def +(that: Int) = Complex(real - that, imag)

  def +(that: Float) = Complex(real - that, imag)

  def +(that: Double) = Complex(real - that, imag)

  def -(that: Complex) = Complex(real - that.real, imag - that.imag)

  def -(that: Int) = Complex(real - that, imag)

  def -(that: Float) = Complex(real - that, imag)

  def -(that: Double) = Complex(real - that, imag)

  def *(that: Complex) = Complex(
    real * that.real - imag * that.imag,
    real * that.imag + imag * that.real,
  )

  def *(that: Int) = Complex(real * that, imag * that)

  def *(that: Float) = Complex(real * that, imag * that)

  def *(that: Double) = Complex(real * that, imag * that)

  def /(that: Complex) = {
    val denom = that.real * that.real + that.imag * that.imag
    Complex(
      (real * that.real + imag * that.imag) / denom,
      (imag * that.real - real * that.imag) / denom,
    )
  }

  def /(that: Int) =
    Complex(real / that, imag / that)

  def /(that: Float) = Complex(real / that, imag / that)

  def /(that: Double) = Complex(real / that, imag / that)

  def %(that: Complex) = {
    val div = this / that
    this - (Complex(floor(div.real), floor(div.imag)) * div)
  }

  def %(that: Int): Complex = %(Complex(that, 0))

  def %(that: Float): Complex = %(Complex(that, 0))

  def %(that: Double): Complex = %(Complex(that, 0))

  def unary_- =
    Complex(-real, -imag)

  def abs = math.sqrt(real * real + imag * imag)

  def conjugate = Complex(real, -imag)

  def log = Complex(math.log(abs), math.atan2(imag, real))

  def exp = {
    val expreal = math.exp(real)
    Complex(expreal * math.cos(imag), expreal * math.sin(imag))
  }

  def pow(b: Double): Complex = pow(Complex(b, 0))

  def pow(b: Complex): Complex = {
    if b == Complex.zero then Complex.one
    else if this == Complex.zero then {
      if b.imag != 0.0 || b.real < 0.0 then Complex.nan
      else Complex.zero
    } else {
      val c = log * b
      val expReal = math.exp(c.real)
      Complex(expReal * math.cos(c.imag), expReal * math.sin(c.imag))
    }
  }

  override def equals(that: Any) = that match {
    case that: Complex => this.real == that.real && this.imag == that.imag
    case real: Double  => this.real == real && this.imag == 0
    case real: Int     => this.real == real && this.imag == 0
    case real: Short   => this.real == real && this.imag == 0
    case real: Long    => this.real == real && this.imag == 0
    case real: Float   => this.real == real && this.imag == 0
    case _             => false
  }

  // ensure hashcode contract is maintained for comparison to non-Complex numbers
  // x ^ 0 is x
  override def hashCode() = real.## ^ imag.##
}

object Complex {

  /** Constant Complex(0,0). */
  val zero = new Complex(0, 0)

  /** Constant Complex(1,0). */
  val one = new Complex(1, 0)

  /** Constant Complex(NaN, NaN). */
  val nan = new Complex(Double.NaN, Double.NaN)

  /** Constant Complex(0,1). */
  val i = new Complex(0, 1)

  extension (num: Int) {
    def i: Complex = Complex(0d, num)

    def +(that: Complex) = Complex(num + that.real, that.imag)

    def -(that: Complex) = Complex(num - that.real, that.imag)

    def *(that: Complex) = that * num

    def /(that: Complex) = Complex(num, 0d) / that
  }

  extension (num: Float) {
    def i: Complex = Complex(0d, num)

    def +(that: Complex) = Complex(num + that.real, that.imag)

    def -(that: Complex) = Complex(num - that.real, that.imag)

    def *(that: Complex) = that * num

    def /(that: Complex) = Complex(num, 0d) / that
  }

  extension (num: Double) {
    def i: Complex = Complex(0d, num)

    def +(that: Complex) = Complex(num + that.real, that.imag)

    def -(that: Complex) = Complex(num - that.real, that.imag)

    def *(that: Complex) = that * num

    def /(that: Complex) = Complex(num, 0d) / that
  }

}
