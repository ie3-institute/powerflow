/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import edu.ie3.powerflow.math.NumericOperations.*

trait NumericOperations[V1] {
  this: V1 =>

  def +[V2, R](that: V2)(using op: Add[V1, V2, R]): R = op(this, that)

  def +:+[V2, R](that: V2)(using op: AddElementWise[V1, V2, R]): R = op(this, that)

  def -[V2, R](that: V2)(using op: Sub[V1, V2, R]): R = op(this, that)

  def -:-[V2, R](that: V2)(using op: SubElementWise[V1, V2, R]): R = op(this, that)

  def *[V2, R](that: V2)(using op: Mul[V1, V2, R]): R = op(this, that)

  def *:*[V2, R](vec: V2)(using op: MulElementWise[V1, V2, R]): R = op(this, vec)

  def /[V2, R](that: V2)(using op: Mul[V1, V2, R]): R = op(this, that)

  def /:/[V2, R](vec: V2)(using op: MulElementWise[V1, V2, R]): R = op(this, vec)

  def split[R1, R2](using op: Split[V1, R1, R2]): (R1, R2) = op(this)

  def \[V2, R](that: V2)(using op: Solve[V1, V2, R]): R = op(this, that)

}

object NumericOperations {

  trait Add[V1, V2, R] extends ((V1, V2) => R)

  trait AddElementWise[V1, V2, R] extends ((V1, V2) => R)

  trait Sub[V1, V2, R] extends ((V1, V2) => R)

  trait SubElementWise[V1, V2, R] extends ((V1, V2) => R)

  trait Mul[V1, V2, R] extends ((V1, V2) => R)

  trait MulElementWise[V1, V2, R] extends ((V1, V2) => R)

  trait Div[V1, V2, R] extends ((V1, V2) => R)

  trait DivElementWise[V1, V2, R] extends ((V1, V2) => R)

  trait Split[V, R1, R2] extends (V => (R1, R2))

  trait Solve[V1, V2, R] extends ((V1, V2) => R)
}
