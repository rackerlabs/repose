package org.openrepose.lols

import javax.script.ScriptEngineManager

import org.python.core.Options

object MainTest extends App {

  import scala.collection.JavaConversions._

  val manager = new ScriptEngineManager()

  manager.getEngineFactories.toList.foreach { it =>
    println(it.getEngineName)
    println(s"\t${it.getNames.mkString(",")}")
  }

  //THIS IS OMFG NECESSARY
  Options.importSite = false

  val engine = manager.getEngineByName("python")
  println(manager.getEngineFactories.toList.mkString(", "))

  println(s" engine is ${engine}")
}
