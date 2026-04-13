/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import edu.ie3.powerflow.math.NumericOperations.{Mul, MulElementWise, Split, Sub}

import scala.reflect.ClassTag

class DenseVector[@specialized(Double) V: ClassTag](
    val length: Int,
    private val data: Array[V],
) extends NumericOperations[DenseVector[V]] {

  def apply(index: Int): V = {
    if index > length then {
      throw new IndexOutOfBoundsException(s"$index not in [0,$length)")
    }

    data(index)
  }

  def update(index: Int, value: V): Unit = data(index) = value

  def slice(from: Int, until: Int): DenseVector[V] = DenseVector(
    data.slice(from, until)
  )

  def toArray: Array[V] = Array.from(data)

  def map[R: ClassTag](f: V => R): DenseVector[R] =
    new DenseVector(length, data.map(f))

  def foreach[U](f: (Int, V) => U): Unit =
    data.zipWithIndex.iterator.map { case (value, idx) =>
      f(idx, value)
    }

  def forall(p: V => Boolean): Boolean = data.forall(p)

}

object DenseVector {

  def filled[V: ClassTag](length: Int, value: V): DenseVector[V] = {
    val data = Array.fill(length)(value)
    new DenseVector(length, data)
  }

  def apply[V: ClassTag](array: Array[V]): DenseVector[V] =
    new DenseVector(array.length, array)

  def apply[V: ClassTag](xs: V*): DenseVector[V] = {
    val length = xs.size
    new DenseVector(length, xs.toArray)
  }
  
  given SPLIT_CV
      : Split[DenseVector[Complex], DenseVector[Double], DenseVector[Double]] =
    vec => {
      val length = vec.length

      val real = filled(length, 0d)
      val imag = filled(length, 0d)

      vec.data.zipWithIndex.foreach { case (value, idx) =>
        real(idx) = value.real
        imag(idx) = value.imag
      }

      (real, imag)
    }

  given SUB_CVCV
      : Sub[DenseVector[Complex], DenseVector[Complex], DenseVector[Complex]] =
    (vec1, vec2) => {
      val data = vec1.data.zip(vec2.data).map((d1, d2) => d1 - d2)
      DenseVector(data)
    }

  given MUL_CVCV: Mul[DenseVector[Complex], DenseVector[Complex], Complex] =
    (vec1, vec2) => {
      var tmp = Complex.zero

      for idx <- 0 until vec1.length do {
        tmp += vec1(idx) * vec2(idx)
      }

      tmp
    }

  given MUL_DVDV_EW: MulElementWise[DenseVector[Complex], DenseVector[
    Complex
  ], DenseVector[Complex]] = (vec1, vec2) => {
    val y = DenseVector.filled(vec1.length, Complex.zero)

    for idx <- 0 until vec1.length do {
      y(idx) = vec1(idx) * vec2(idx)
    }

    y
  }

}
