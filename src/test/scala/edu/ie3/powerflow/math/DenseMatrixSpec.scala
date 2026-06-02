/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.math

import edu.ie3.test.common.UnitSpec

class DenseMatrixSpec extends UnitSpec {

  "A DenseMatrix" should {
    "be updated correctly" in {
      val matrix = DenseMatrix.filled(3, 3, 0d)
      matrix.forall(_ == 0d) shouldBe true

      matrix(2, 2) = 5
      matrix.forall(_ == 0d) shouldBe false

      matrix(2, 2) shouldBe 5
    }

    "calculate the correct linear index correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d)

      matrix.linearIndex(0, 0) shouldBe 0
      matrix.linearIndex(1, 0) shouldBe 1
      matrix.linearIndex(2, 0) shouldBe 2
      matrix.linearIndex(0, 1) shouldBe 3
      matrix.linearIndex(1, 1) shouldBe 4
      matrix.linearIndex(2, 1) shouldBe 5
      matrix.linearIndex(0, 2) shouldBe 6
      matrix.linearIndex(1, 2) shouldBe 7
      matrix.linearIndex(2, 2) shouldBe 8
      matrix.linearIndex(0, 3) shouldBe 9
      matrix.linearIndex(1, 3) shouldBe 10
      matrix.linearIndex(2, 3) shouldBe 11
    }

    "calculate the correct linear index for a transposed matrix correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d, true)

      matrix.linearIndex(0, 0) shouldBe 0
      matrix.linearIndex(0, 1) shouldBe 1
      matrix.linearIndex(0, 2) shouldBe 2
      matrix.linearIndex(1, 0) shouldBe 3
      matrix.linearIndex(1, 1) shouldBe 4
      matrix.linearIndex(1, 2) shouldBe 5
      matrix.linearIndex(2, 0) shouldBe 6
      matrix.linearIndex(2, 1) shouldBe 7
      matrix.linearIndex(2, 2) shouldBe 8
      matrix.linearIndex(3, 0) shouldBe 9
      matrix.linearIndex(3, 1) shouldBe 10
      matrix.linearIndex(3, 2) shouldBe 11
    }

    "calculate the row and column from a linear index correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d)

      matrix.rowAndColumnFromLinearIndex(0) shouldBe (0, 0)
      matrix.rowAndColumnFromLinearIndex(1) shouldBe (1, 0)
      matrix.rowAndColumnFromLinearIndex(2) shouldBe (2, 0)
      matrix.rowAndColumnFromLinearIndex(3) shouldBe (0, 1)
      matrix.rowAndColumnFromLinearIndex(4) shouldBe (1, 1)
      matrix.rowAndColumnFromLinearIndex(5) shouldBe (2, 1)
      matrix.rowAndColumnFromLinearIndex(6) shouldBe (0, 2)
      matrix.rowAndColumnFromLinearIndex(7) shouldBe (1, 2)
      matrix.rowAndColumnFromLinearIndex(8) shouldBe (2, 2)
      matrix.rowAndColumnFromLinearIndex(9) shouldBe (0, 3)
      matrix.rowAndColumnFromLinearIndex(10) shouldBe (1, 3)
      matrix.rowAndColumnFromLinearIndex(11) shouldBe (2, 3)
    }

    "calculate the row and column from a linear index for a transposed matrix correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d, true)

      matrix.rowAndColumnFromLinearIndex(0) shouldBe (0, 0)
      matrix.rowAndColumnFromLinearIndex(1) shouldBe (0, 1)
      matrix.rowAndColumnFromLinearIndex(2) shouldBe (0, 2)
      matrix.rowAndColumnFromLinearIndex(3) shouldBe (1, 0)
      matrix.rowAndColumnFromLinearIndex(4) shouldBe (1, 1)
      matrix.rowAndColumnFromLinearIndex(5) shouldBe (1, 2)
      matrix.rowAndColumnFromLinearIndex(6) shouldBe (2, 0)
      matrix.rowAndColumnFromLinearIndex(7) shouldBe (2, 1)
      matrix.rowAndColumnFromLinearIndex(8) shouldBe (2, 2)
      matrix.rowAndColumnFromLinearIndex(9) shouldBe (3, 0)
      matrix.rowAndColumnFromLinearIndex(10) shouldBe (3, 1)
      matrix.rowAndColumnFromLinearIndex(11) shouldBe (3, 2)
    }

    "return an iterator correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d)

      for i <- 0 until matrix.linearSize do {
        val (r, c) = matrix.rowAndColumnFromLinearIndex(i)
        matrix(r, c) = i
      }

      matrix.iterator.toList shouldBe List(
        ((0, 0), 0),
        ((1, 0), 1),
        ((2, 0), 2),
        ((0, 1), 3),
        ((1, 1), 4),
        ((2, 1), 5),
        ((0, 2), 6),
        ((1, 2), 7),
        ((2, 2), 8),
        ((0, 3), 9),
        ((1, 3), 10),
        ((2, 3), 11),
      )
    }

    "return an iterator for a transposed matrix correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d, true)

      for i <- 0 until matrix.linearSize do {
        val (r, c) = matrix.rowAndColumnFromLinearIndex(i)
        matrix(r, c) = i
      }

      matrix.iterator.toList shouldBe List(
        ((0, 0), 0),
        ((0, 1), 1),
        ((0, 2), 2),
        ((1, 0), 3),
        ((1, 1), 4),
        ((1, 2), 5),
        ((2, 0), 6),
        ((2, 1), 7),
        ((2, 2), 8),
        ((3, 0), 9),
        ((3, 1), 10),
        ((3, 2), 11),
      )
    }

    "return a column iterator correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d)

      for i <- 0 until matrix.linearSize do {
        val (r, c) = matrix.rowAndColumnFromLinearIndex(i)
        matrix(r, c) = i
      }

      val list = matrix.columnIterator.toList
      list.head.data shouldBe Array(0, 1, 2)
      list(1).data shouldBe Array(3, 4, 5)
      list(2).data shouldBe Array(6, 7, 8)
      list(3).data shouldBe Array(9, 10, 11)
    }

    "return a column iterator for a transposed matrix correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d, true)

      for i <- 0 until matrix.linearSize do {
        val (r, c) = matrix.rowAndColumnFromLinearIndex(i)
        matrix(r, c) = i
      }

      val list = matrix.columnIterator.toList
      list.head.data shouldBe Array(0, 3, 6, 9)
      list(1).data shouldBe Array(1, 4, 7, 10)
      list(2).data shouldBe Array(2, 5, 8, 11)
    }

    "return a row iterator correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d)

      for i <- 0 until matrix.linearSize do {
        val (r, c) = matrix.rowAndColumnFromLinearIndex(i)
        matrix(r, c) = i
      }

      val list = matrix.rowIterator.toList
      list.head.data shouldBe Array(0, 3, 6, 9)
      list(1).data shouldBe Array(1, 4, 7, 10)
      list(2).data shouldBe Array(2, 5, 8, 11)
    }

    "return a row iterator for a transposed matrix correctly" in {
      val matrix = DenseMatrix.filled(3, 4, 0d, true)

      for i <- 0 until matrix.linearSize do {
        val (r, c) = matrix.rowAndColumnFromLinearIndex(i)
        matrix(r, c) = i
      }

      val list = matrix.rowIterator.toList
      list.head.data shouldBe Array(0, 1, 2)
      list(1).data shouldBe Array(3, 4, 5)
      list(2).data shouldBe Array(6, 7, 8)
      list(3).data shouldBe Array(9, 10, 11)
    }

  }

}
