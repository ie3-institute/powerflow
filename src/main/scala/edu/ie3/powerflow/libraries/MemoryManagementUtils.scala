/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import java.lang.foreign.ValueLayout.{JAVA_DOUBLE, JAVA_INT, JAVA_LONG}
import java.lang.foreign.{Arena, MemoryLayout, MemorySegment}

trait MemoryManagementUtils {

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
