package org.openrepose.valveLauncher

import java.util.Properties

object Main extends App {

  //Load up the main.properties
  val props = new Properties()
  props.load(this.getClass.getResourceAsStream("/main.properties"))

  val version = props.getProperty("version", "UNKNONWN")

  /**
   * Yeah this looks ugly in here, but it comes out glorious on the console.
   * For reference: http://patorjk.com/software/taag/#p=display&h=1&v=1&f=ANSI%20Shadow&t=SERVO
   */
  val fancyString =
    """
      |███████╗███████╗██████╗ ██╗   ██╗ ██████╗
      |██╔════╝██╔════╝██╔══██╗██║   ██║██╔═══██╗  I'm in your base,
      |███████╗█████╗  ██████╔╝██║   ██║██║   ██║  launchin your valves.
      |╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██║   ██║  Version $version
      |███████║███████╗██║  ██║ ╚████╔╝ ╚██████╔╝
      |╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝   ╚═════╝
      |
    """.stripMargin.replace("$version", version)

  println(fancyString)


}
