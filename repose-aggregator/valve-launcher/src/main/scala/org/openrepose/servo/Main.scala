package org.openrepose.servo

import com.typesafe.config.ConfigFactory

/**
 * Just a thin wrapper around the actual Servo. This is the application entry point
 * Servo.scala does all the actual work
 */
object Main extends App {

  val config = ConfigFactory.load()
  val exitCode = Servo.execute(args, System.in, System.out, System.err, config)
  //Should only get here if it's done (like that method blocks all this forever)
  sys.exit(exitCode)
}
