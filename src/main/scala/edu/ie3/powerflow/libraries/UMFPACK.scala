package edu.ie3.powerflow.libraries

import edu.ie3.powerflow.libraries.Native

import java.lang.foreign.FunctionDescriptor.{of, ofVoid}
import java.lang.foreign.ValueLayout.*
import java.lang.foreign.{Arena, MemorySegment, ValueLayout}

final case class UMFPACK(override val libName: String) extends Native {

  def solve(
      n: Long,
      Ap: Array[Long],
      Ai: Array[Long],
      Ax: Array[Double],
      b: Array[Double],
           ): Array[Double] =
    withArena { arena =>
      given Arena = arena

      val symbolicAddress = arena.allocate(ADDRESS)
      symbolicAddress.setAtIndex(ADDRESS, 0, NULL_PTR)

      val numericAddress = arena.allocate(ADDRESS)
      numericAddress.setAtIndex(ADDRESS, 0, NULL_PTR)

      val x = arena.allocate(JAVA_DOUBLE, n)
      val ApPtr = Ap.toStack    
      val AiPtr = Ai.toStack
      val AxPtr = Ax.toStack

      val symbolicStatus = umfpack_dl_symbolic(n, n, ApPtr, AiPtr, AxPtr, symbolicAddress)
      val symbolic = symbolicAddress.getAtIndex(ADDRESS, 0)

      val numericStatus = umfpack_dl_numeric(ApPtr, AiPtr, AxPtr, symbolic, numericAddress)
      val numeric = numericAddress.getAtIndex(ADDRESS, 0)

      umfpack_dl_free_symbolic(symbolicAddress)

      val solveStatus = umfpack_dl_solve(0, ApPtr, AiPtr, AxPtr, x, b.toStack, numeric)

      umfpack_dl_free_numeric(numericAddress)
      
      x.toArray(JAVA_DOUBLE)
    }
  
  private val dlSymbolicHandle = buildHandle(
    "umfpack_dl_symbolic",
    of(
      JAVA_INT,
      JAVA_LONG,
      JAVA_LONG,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
    )
  )
  
  def umfpack_dl_symbolic(
      n_row: Long,
      n_col: Long,
      Ap: MemorySegment,
      Ai: MemorySegment,
      Ax: MemorySegment,
      symbolicPtr: MemorySegment
                         ): Int =
    dlSymbolicHandle.invoke(
      n_row,
      n_col,
      Ap,
      Ai,
      Ax,
      symbolicPtr,
      NULL_PTR,
      NULL_PTR
    )

  private val dlNumericHandle = buildHandle(
    "umfpack_dl_numeric",
    of(
      JAVA_INT,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
      C_POINTER,
    )
  )

  def umfpack_dl_numeric(
                           Ap: MemorySegment,
                           Ai: MemorySegment,
                           Ax: MemorySegment,
                           symbolic: MemorySegment,
                           numeric: MemorySegment
                         ): Int =
    dlNumericHandle.invoke(
      Ap,
      Ai,
      Ax,
      symbolic,
      numeric,
      NULL_PTR,
      NULL_PTR
    )

  private val dlSymbolicFreeHandle = buildHandle("umfpack_dl_free_symbolic", ofVoid(C_POINTER))

  def umfpack_dl_free_symbolic(symbolic: MemorySegment): Unit = dlSymbolicFreeHandle.invoke(symbolic)


  private val dlSolveHandle = buildHandle("umfpack_dl_solve", of(
    JAVA_INT,
    JAVA_LONG,
    C_POINTER,
    C_POINTER,
    C_POINTER,
    C_POINTER,
    C_POINTER,
    C_POINTER,
    C_POINTER,
    C_POINTER
  ))

  def umfpack_dl_solve(
      sys: Long,
                        Ap: MemorySegment,
                        Ai: MemorySegment,
                        Ax: MemorySegment,
                        x: MemorySegment,
                        b: MemorySegment,
                        numeric: MemorySegment
                      ): Int = dlSolveHandle.invoke(
    sys,
    Ap,
    Ai,
    Ax,
    x,
    b,
    numeric,
    NULL_PTR,
    NULL_PTR
  )

  private val dlNumericFreeHandle = buildHandle("umfpack_dl_free_numeric", ofVoid(C_POINTER))

  def umfpack_dl_free_numeric(numeric: MemorySegment): Unit = dlNumericFreeHandle.invoke(numeric)
}
