package org.openrepose.commons.utils.servlet.http

import java.io.ByteArrayOutputStream
import javax.servlet.ServletOutputStream

class ByteArrayServletOutputStream extends ServletOutputStream {
  val baos = new ByteArrayOutputStream()

  override def write(b: Int): Unit = baos.write(b)

  override def toString: String = baos.toString
}
