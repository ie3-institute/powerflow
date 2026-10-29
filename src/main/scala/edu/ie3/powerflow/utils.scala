/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import java.util.function.{IntConsumer, IntFunction}
import scala.reflect.ClassTag

def ffor(start: Int, end: Int)(f: IntConsumer): Unit = ffor(start, end, 1)(f)

/** Fast for loop.
  *
  * @param start
  * @param end
  * @param step
  * @param f
  */
def ffor(start: Int, end: Int, step: Int)(f: IntConsumer): Unit = {
  var i = start
  while i < end do {
    f.accept(i)
    i += step
  }
}

def fforA[R: ClassTag](start: Int, end: Int)(f: IntFunction[R]): Array[R] =
  fforA(start, end, 1)(f)

def fforA[R: ClassTag](start: Int, end: Int, step: Int)(
    f: IntFunction[R]
): Array[R] = {
  val l = end - start
  val res = Array.ofDim[R](l)

  var i = start
  while i < end do {
    res(i) = f.apply(i)
    i += step
  }

  res
}
