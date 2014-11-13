package org.openrepose.valve

import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.load()
  val valve = new Valve()
  sys.ShutdownHookThread {
    valve.shutdown()
  }

  val exitCode = valve.execute(args, System.in, System.out, System.err, config)
  
  sys.exit(exitCode)
}
