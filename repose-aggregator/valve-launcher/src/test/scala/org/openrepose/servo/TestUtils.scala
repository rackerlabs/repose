package org.openrepose.servo

import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Paths, Files}

import scala.io.Source

trait TestUtils {
  def resourceContent(resource: String) = {
    Source.fromInputStream(this.getClass.getResourceAsStream(resource)).mkString
  }

  def writeSystemModel(configRoot: String, content: String): Unit = {
    Files.write(Paths.get(configRoot + "/system-model.cfg.xml"), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
  }

}
