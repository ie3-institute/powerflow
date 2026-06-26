/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.{JAVA_DOUBLE, JAVA_INT, JAVA_LONG}
import java.lang.invoke.MethodHandle
import scala.util.{Try, Using}

/** Trait that contains some methods and extensions to simplify the process of
  * writing wrappers.
  */
trait Native {

  private val arena: Arena = Arena.global()
  private val linker: Linker = Linker.nativeLinker()

  val libName: String

  val library: SymbolLookup = SymbolLookup.libraryLookup(libName, arena)

  def buildHandle(
      fcnName: String,
      descriptor: FunctionDescriptor,
  ): MethodHandle = {
    val ptr = library.findOrThrow(fcnName)
    linker.downcallHandle(ptr, descriptor)
  }

  def withArena[R](f: Arena => R): R = Using(Arena.ofConfined())(f).get

  // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
  // some extensions

  extension (value: Double) {
    def asPtr(using arena: Arena): MemorySegment = {
      val segment = arena.allocate(JAVA_DOUBLE)
      segment.setAtIndex(JAVA_DOUBLE, 0, value)
      segment
    }
  }

  extension (value: Int) {
    def asPtr(using arena: Arena): MemorySegment = {
      val segment = arena.allocate(JAVA_INT)
      segment.setAtIndex(JAVA_INT, 0, value)
      segment
    }
  }

  extension (value: Long) {
    def asPtr(using arena: Arena): MemorySegment = {
      val segment = arena.allocate(JAVA_LONG)
      segment.setAtIndex(JAVA_LONG, 0, value)
      segment
    }
  }

  extension (str: String) {
    def asPtr(using arena: Arena): MemorySegment = arena.allocateFrom(str)
  }

  extension (arr: Array[Double]) {
    def toHeap: MemorySegment = MemorySegment.ofArray(arr)

    def toStack(using arena: Arena): MemorySegment = {
      val segment =
        arena.allocate(MemoryLayout.sequenceLayout(arr.length, JAVA_DOUBLE))
      segment.copyFrom(toHeap)
      segment
    }
  }

  extension (arr: Array[Int]) {
    def toHeap: MemorySegment = MemorySegment.ofArray(arr)

    def toStack(using arena: Arena): MemorySegment = {
      val segment =
        arena.allocate(MemoryLayout.sequenceLayout(arr.length, JAVA_INT))
      segment.copyFrom(toHeap)
      segment
    }
  }
}
