/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import org.slf4j.{Logger, LoggerFactory}

import java.lang.foreign.{Arena, FunctionDescriptor, Linker, SymbolLookup}
import java.lang.invoke.MethodHandle
import scala.jdk.OptionConverters.RichOptional
import scala.util.{Failure, Success, Try}

trait Native {

  private val log: Logger = LoggerFactory.getLogger("Native")

  protected given arena: Arena = Arena.ofAuto()
  private val linker: Linker = Linker.nativeLinker()

  private val library: Option[SymbolLookup] = Try {
    val path = getLibrary.getOrElse {
      val osName = System.getProperty("os.name").toLowerCase()
      if osName == null || osName.isEmpty then
        throw new RuntimeException("Unable to determine operating system.")

      if osName.contains("win") then {
        "libopenblas.dll"
      } else if osName.contains("mac") then {
        "/System/Library/Frameworks/Accelerate.framework/Accelerate"
      } else {
        "liblapack.so.3"
      }
    }

    SymbolLookup.libraryLookup(path, arena)
  } match {
    case Failure(exception) =>
      log.warn(s"Could not load library due to: $exception")
      None

    case Success(value) =>
      Some(value)
  }

  def getLibrary: Try[String]

  def buildHandle(
      fcnName: String,
      descriptor: FunctionDescriptor,
  ): Option[MethodHandle] =
    library
      .flatMap(_.find(fcnName).toScala)
      .map(linker.downcallHandle(_, descriptor))

}
