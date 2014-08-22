package org.openrepose.servo

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, StandardOpenOption, Paths, Files}

import scala.io.Source

trait TestUtils {
  def resourceContent(resource: String) = {
    Source.fromInputStream(this.getClass.getResourceAsStream(resource)).mkString
  }

  def writeSystemModel(configRoot: String, content: String): Unit = {
    val tempFile = new File(configRoot, "system-model.cfg.xml")
    tempFile.deleteOnExit() //Mark it to be clobbered later
    writeFileContent(tempFile, content)
  }

  def writeContainerConfig(configRoot:String, content:String):Unit = {
    val tempFile = new File(configRoot, "container.cfg.xml")
    tempFile.deleteOnExit()
    writeFileContent(tempFile, content)
  }

  def writeFileContent(file: File, content: String): Unit = {
    Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
  }


  /**
   * Apparently I should never use DeleteOnExit: http://bobcongdon.net/blog/2005/07/file-deleteonexit-is-evil/
   * However, since test cases are used and then terminated relatively quickly, this is probably okay.
   * @param prefix
   * @param suffix
   * @return
   */
  def tempFile(prefix: String, suffix: String, parent: Option[File] = None): File = {
    val file = parent match {
      case Some(path) => File.createTempFile(prefix, suffix, path)
      case None => File.createTempFile(prefix, suffix)
    }
    file.deleteOnExit()
    file
  }

  def tempDir(prefix: String): Path = {
    val dir = Files.createTempDirectory(prefix)
    dir.toFile.deleteOnExit() //Note, this doesn't clean up files in it, if they're not also delete-able
    dir
  }

}
