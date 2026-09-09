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

/** Companion object for the sparse solver.
  */
object UMFPACK {

  /** Returns an option for UMFPACK.
    */
  def get: Option[UMFPACK] = {
    val overridePath = Try(System.getProperty("umfpack")).toOption

    // only works for unix systems
    val fallback = "libumfpack.so.6.3.2"

    LibraryLoader.load(overridePath, fallback, UMFPACK.apply)
  }

}

/** UMFPACK is a fast sparse matrix solver written in C/C++. This class provides
  * the necessary bindings, to call UMFPACK from the JVM.
  *
  * <p> NOTE: The current implementation only uses doubles as values and ints as
  * indices. No implementation for complex numbers or long indices is planed.
  *
  * @param libName
  *   The name or full path of the library.
  */
final case class UMFPACK(override val libName: String)
    extends Native,
      SparseSolver {

  /** A method for solving a system of equations using a sparse (CSC) matrix.
    * @param matrix
    *   The sparse matrix to solve.
    * @param b
    *   The right side of the system.
    * @return
    *   The solution of the system.
    */
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

  /** Low level method for solving a system of equations using a sparse (CSC)
    * matrix.
    * @param nRow
    *   The number of rows of the matrix.
    * @param nCol
    *   The number of columns of the matrix.
    * @param Ap
    *   An array containing the starting indices in [[Ax]] for each column.
    * @param Ai
    *   An array containing the row indices corresponding to each non-zero
    *   value.
    * @param Ax
    *   An array containing the actual non-zero values.
    * @param b
    *   The right side of the system.
    * @return
    *   The solution of the system.
    */
  def solve(
      nRow: Int,
      nCol: Int,
      Ap: Array[Int],
      Ai: Array[Int],
      Ax: Array[Double],
      b: Array[Double],
  ): Array[Double] =
    withArena { arena =>
      given Arena = arena

      // create a memory segment that contains the symbolic address
      // the symbolic address is initialized with a null pointer
      // the actual address and size will be determined by the symbolic method
      val symbolicAddress = arena.allocate(ADDRESS)
      symbolicAddress.setAtIndex(ADDRESS, 0, NULL_PTR)

      // create a memory segment that contains the numeric address
      // the numeric address is initialized with a null pointer
      // the actual address and size will be determined by the numeric method
      val numericAddress = arena.allocate(ADDRESS)
      numericAddress.setAtIndex(ADDRESS, 0, NULL_PTR)

      // allocate native memory and writing the data to it
      val x = arena.allocate(JAVA_DOUBLE, nCol)
      val ApPtr = Ap.toStack
      val AiPtr = Ai.toStack
      val AxPtr = Ax.toStack

      // symbolic method that constructs the symbolic data structure
      // after the construction get a pointer to the actual address by retrieving the address
      umfpack_di_symbolic(nRow, nCol, ApPtr, AiPtr, AxPtr, symbolicAddress)
      val symbolic = symbolicAddress.getAtIndex(ADDRESS, 0)

      // numeric method that constructs the numeric data structure that is needed for the solving method
      // after the construction get a pointer to the actual address by retrieving the address
      umfpack_di_numeric(ApPtr, AiPtr, AxPtr, symbolic, numericAddress)
      val numeric = numericAddress.getAtIndex(ADDRESS, 0)

      // since we no longer need the symbolic structure, we free the memory
      umfpack_di_free_symbolic(symbolicAddress)

      // we can now call the actual solving method
      umfpack_di_solve(0, ApPtr, AiPtr, AxPtr, x, b.toStack, numeric)

      // since the solving is finished, we can now free the memory of the numeric structure
      umfpack_di_free_numeric(numericAddress)

      // lastly, we retrieve the solution from the memory
      x.toArray(JAVA_DOUBLE)
    }

  /** Method handle for the symbolic method.
    */
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

  /** Method to create the symbolic data and updating the address in
    * [[symbolicPtr]].
    * @param nRow
    *   The number of rows of the matrix.
    * @param nCol
    *   The number of columns of the matrix.
    * @param Ap
    *   An array containing the starting indices in [[Ax]] for each column.
    * @param Ai
    *   An array containing the row indices corresponding to each non-zero
    *   value.
    * @param Ax
    *   An array containing the actual non-zero values.
    * @param symbolicPtr
    *   A pointer to the address for the symbolic data. (It is initialized as a
    *   null pointer)
    * @return
    *   The status of the method.
    */
  private def umfpack_di_symbolic(
      nRow: Int,
      nCol: Int,
      Ap: MemorySegment,
      Ai: MemorySegment,
      Ax: MemorySegment,
      symbolicPtr: MemorySegment,
  ): Int =
    diSymbolicHandle.invoke(
      nRow,
      nCol,
      Ap,
      Ai,
      Ax,
      symbolicPtr,
      NULL_PTR,
      NULL_PTR,
    )

  /** Method handle for the numeric method.
    */
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

  /** Method to create the numeric data and updating the address in
    * [[numericPtr]].
    * @param Ap
    *   An array containing the starting indices in [[Ax]] for each column.
    * @param Ai
    *   An array containing the row indices corresponding to each non-zero
    *   value.
    * @param Ax
    *   An array containing the actual non-zero values.
    * @param symbolic
    *   The address (pointer) of the symbolic data.
    * @param numericPtr
    *   A pointer to the address for the numeric data. (It is initialized as a
    *   null pointer)
    * @return
    *   The status of the method.
    */
  private def umfpack_di_numeric(
      Ap: MemorySegment,
      Ai: MemorySegment,
      Ax: MemorySegment,
      symbolic: MemorySegment,
      numericPtr: MemorySegment,
  ): Int =
    diNumericHandle.invoke(
      Ap,
      Ai,
      Ax,
      symbolic,
      numericPtr,
      NULL_PTR,
      NULL_PTR,
    )

  /** Method handle for the free symbolic method.
    */
  private val diSymbolicFreeHandle =
    buildHandle("umfpack_di_free_symbolic", ofVoid(C_POINTER))

  /** Method to free symbolic data in the native memory.
    * @param symbolic
    *   The address (pointer) of the symbolic data.
    */
  private def umfpack_di_free_symbolic(symbolic: MemorySegment): Unit =
    diSymbolicFreeHandle.invoke(symbolic)

  /** Method handle for the solve method.
    */
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

  /** @param sys
    *   An integer that specifies the type of system to solve (0: Ax=b).
    * @param Ap
    *   An array containing the starting indices in [[Ax]] for each column.
    * @param Ai
    *   An array containing the row indices corresponding to each non-zero
    *   value.
    * @param Ax
    *   An array containing the actual non-zero values.
    * @param x
    *   An empty array that will contain the solution of the system.
    * @param b
    *   An array containing the right side of the system.
    * @param numeric
    *   The address (pointer) of the numeric data.
    * @return
    *   The status of the method.
    */
  private def umfpack_di_solve(
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

  /** Method handle for the free numeric method.
    */
  private val diNumericFreeHandle =
    buildHandle("umfpack_di_free_numeric", ofVoid(C_POINTER))

  /** Method to free numeric data in the native memory.
    *
    * @param numeric
    *   The address (pointer) of the numeric data.
    */
  private def umfpack_di_free_numeric(numeric: MemorySegment): Unit =
    diNumericFreeHandle.invoke(numeric)
}
