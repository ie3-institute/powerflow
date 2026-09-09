/*
 * © 2026. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow.libraries

import org.slf4j.{Logger, LoggerFactory}

/** Object containing method for loading native libraries.
  */
object LibraryLoader {

  val log: Logger = LoggerFactory.getLogger(classOf[LibraryLoader.type])

  /** Method for loading a native library.
    * @param libPath
    *   An option for a path provided via [[java.lang.System.getProperty()]].
    * @param fallback
    *   A fallback path.
    * @param native
    *   A constructor to build the native wrapper.
    * @tparam T
    *   Type of loaded library.
    * @return
    *   Either an option for the loaded native library or None.
    */
  def load[T](
      libPath: Option[String],
      fallback: String,
      native: String => T,
  ): Option[T] =
    libPath match {
      case Some(path) if path != null && path.nonEmpty =>
        try {
          Some(native(path))
        } catch {
          case e: Throwable =>
            log.warn(
              s"Could not load native library '$path'. Using fallback implementation"
            )

            load(fallback, native)
        }

      case _ =>
        load(fallback, native)
    }

  /** Method for loading a native library.
    * @param path
    *   The path of the library.
    * @param native
    *   A constructor to build the native wrapper.
    * @tparam T
    *   Type of loaded library.
    * @return
    *   Either an option for the loaded native library or None.
    */

  def load[T](path: String, native: String => T): Option[T] = try {
    Some(native(path))
  } catch {
    case e: Throwable =>
      log.warn(
        s"Could not load native library '$path'. Using default implementation",
        e,
      )
      None
  }
}
