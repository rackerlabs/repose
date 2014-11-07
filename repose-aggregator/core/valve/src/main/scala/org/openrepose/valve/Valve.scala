package org.openrepose.valve

import java.io.{File, PrintStream, InputStream}


class Valve {

  case class ValveConfig(configDirectory: File = new File("/etc/repose"),
                         insecure: Boolean = false,
                         showVersion: Boolean = false,
                         showUsage: Boolean = false
                          )

  def execute(args: Array[String], in: InputStream, out: PrintStream, err: PrintStream): Int = {
    ???
  }

  def shutdown() = {
    //TODO: I might not need to call a shutdown?
    ???
  }
}
