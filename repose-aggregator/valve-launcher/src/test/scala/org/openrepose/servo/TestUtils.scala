package org.openrepose.servo

import scala.io.Source

trait TestUtils {
  def resourceContent(resource:String) = {
    Source.fromInputStream(this.getClass.getResourceAsStream(resource)).mkString
  }
}
