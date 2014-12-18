package org.openrepose.commons.utils.classloader

class EarProcessingException(message:String, cause: Throwable = null) extends Exception(message, cause) {
  def this(message:String) {
    this(message, null)
  }
}
