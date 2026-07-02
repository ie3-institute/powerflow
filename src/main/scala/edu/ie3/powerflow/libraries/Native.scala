/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import java.lang.Long.MAX_VALUE
import java.lang.foreign.*
import java.lang.foreign.ValueLayout.{JAVA_BYTE, JAVA_DOUBLE, JAVA_INT, JAVA_LONG}
import java.lang.invoke.MethodHandle
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Using

/** Trait that contains some methods and extensions to simplify the process of
  * writing wrappers.
  */
trait Native {

  private val arena: Arena = Arena.global()
  private val linker: Linker = Linker.nativeLinker()
  val canonicalLayouts: Map[String, MemoryLayout] = linker.canonicalLayouts.asScala.toMap
  val C_POINTER: AddressLayout = canonicalLayouts("void*").asInstanceOf[AddressLayout].withTargetLayout(MemoryLayout.sequenceLayout(MAX_VALUE, JAVA_BYTE))


  val libName: String

  val library: SymbolLookup = SymbolLookup.libraryLookup(libName, arena)

  protected val NULL_PTR: MemorySegment = MemorySegment.NULL
  
  def buildHandle(
      fcnName: String,
      descriptor: FunctionDescriptor,
  ): MethodHandle = {
    val address = library.findOrThrow(fcnName)
    linker.downcallHandle(address, descriptor)
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

    def toStack(using arena: Arena): MemorySegment = arena.allocateFrom(JAVA_DOUBLE, arr*)
  }

  extension (arr: Array[Int]) {
    def toHeap: MemorySegment = MemorySegment.ofArray(arr)

    def toStack(using arena: Arena): MemorySegment = arena.allocateFrom(JAVA_INT, arr *)
  }

  extension (arr: Array[Long]) {
    def toHeap: MemorySegment = MemorySegment.ofArray(arr)

    def toStack(using arena: Arena): MemorySegment = arena.allocateFrom(JAVA_LONG, arr*)
  }
}
