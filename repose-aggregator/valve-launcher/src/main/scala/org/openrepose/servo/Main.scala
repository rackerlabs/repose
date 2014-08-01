package org.openrepose.servo

/**
 * Just a thin wrapper around the actual Servo. This is the application entry point
 * Servo.scala does all the actual work
 */
object Main extends App {
  val exitCode = Servo.execute(args, System.in, System.out, System.err)
  //Should only get here if it's done (like that method blocks all this forever)
  sys.exit(exitCode)
}
