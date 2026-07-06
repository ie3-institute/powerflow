/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import edu.ie3.powerflow.libraries.Native
import edu.ie3.powerflow.math.{CSCMatrix, DenseVector, SparseSolver}

import java.lang.foreign.FunctionDescriptor.{of, ofVoid}
import java.lang.foreign.ValueLayout.*
import java.lang.foreign.{Arena, MemorySegment}
import scala.util.Try

object UMFPACK {

  def get: Option[UMFPACK] = {
    val overridePath = Try(System.getProperty("umfpack")).toOption

    // only works for unix systems
    val fallback = "libumfpack.so.6.3.2"

    LibraryLoader.load(overridePath, fallback, UMFPACK.apply)
  }

}

final case class UMFPACK(override val libName: String)
    extends Native,
      SparseSolver {

  override def solve(
      matrix: CSCMatrix,
      b: DenseVector[Double],
  ): DenseVector[Double] = DenseVector(
    solve(
      matrix.rows,
      matrix.cols,
      matrix.colOffset,
      matrix.rowIndices,
      matrix.values,
      b.data,
    )
  )

  def solve(
      n_row: Int,
      n_col: Int,
      Ap: Array[Int],
      Ai: Array[Int],
      Ax: Array[Double],
      b: Array[Double],
  ): Array[Double] =
    withArena { arena =>
      given Arena = arena

      val symbolicAddress = arena.allocate(ADDRESS)
      symbolicAddress.setAtIndex(ADDRESS, 0, NULL_PTR)

      val numericAddress = arena.allocate(ADDRESS)
      numericAddress.setAtIndex(ADDRESS, 0, NULL_PTR)

      val x = arena.allocate(JAVA_DOUBLE, n_col)
      val ApPtr = Ap.toStack
      val AiPtr = Ai.toStack
      val AxPtr = Ax.toStack

      val symbolicStatus =
        umfpack_di_symbolic(n_row, n_col, ApPtr, AiPtr, AxPtr, symbolicAddress)
      val symbolic = symbolicAddress.getAtIndex(ADDRESS, 0)

      val numericStatus =
        umfpack_di_numeric(ApPtr, AiPtr, AxPtr, symbolic, numericAddress)
      val numeric = numericAddress.getAtIndex(ADDRESS, 0)

      umfpack_di_free_symbolic(symbolicAddress)

      val solveStatus =
        umfpack_di_solve(0, ApPtr, AiPtr, AxPtr, x, b.toStack, numeric)

      umfpack_di_free_numeric(numericAddress)

      x.toArray(JAVA_DOUBLE)
    }

  private val diSymbolicHandle = buildHandle(
    "umfpack_di_symbolic",
    of(
      JAVA_INT,
      JAVA_INT,
      JAVA_INT,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
    ),
  )

  def umfpack_di_symbolic(
      n_row: Int,
      n_col: Int,
      Ap: MemorySegment,
      Ai: MemorySegment,
      Ax: MemorySegment,
      symbolicPtr: MemorySegment,
  ): Int =
    diSymbolicHandle.invoke(
      n_row,
      n_col,
      Ap,
      Ai,
      Ax,
      symbolicPtr,
      NULL_PTR,
      NULL_PTR,
    )

  private val diNumericHandle = buildHandle(
    "umfpack_di_numeric",
    of(
      JAVA_INT,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
    ),
  )

  def umfpack_di_numeric(
      Ap: MemorySegment,
      Ai: MemorySegment,
      Ax: MemorySegment,
      symbolic: MemorySegment,
      numeric: MemorySegment,
  ): Int =
    diNumericHandle.invoke(
      Ap,
      Ai,
      Ax,
      symbolic,
      numeric,
      NULL_PTR,
      NULL_PTR,
    )

  private val diSymbolicFreeHandle =
    buildHandle("umfpack_di_free_symbolic", ofVoid(C_POINTER))

  def umfpack_di_free_symbolic(symbolic: MemorySegment): Unit =
    diSymbolicFreeHandle.invoke(symbolic)

  private val diSolveHandle = buildHandle(
    "umfpack_di_solve",
    of(
      JAVA_INT,
      JAVA_INT,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
    ),
  )

  def umfpack_di_solve(
      sys: Int,
      Ap: MemorySegment,
      Ai: MemorySegment,
      Ax: MemorySegment,
      x: MemorySegment,
      b: MemorySegment,
      numeric: MemorySegment,
  ): Int = diSolveHandle.invoke(
    sys,
    Ap,
    Ai,
    Ax,
    x,
    b,
    numeric,
    NULL_PTR,
    NULL_PTR,
  )

  private val diNumericFreeHandle =
    buildHandle("umfpack_di_free_numeric", ofVoid(C_POINTER))

  def umfpack_di_free_numeric(numeric: MemorySegment): Unit =
    diNumericFreeHandle.invoke(numeric)
}
