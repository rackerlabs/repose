/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.valve

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import scala.io.Source

trait TestUtils {
  def resourceContent(resource: String) = {
    Source.fromInputStream(this.getClass.getResourceAsStream(resource)).mkString
  }

  def writeSystemModel(configRoot: String, content: String): File = {
    val tempFile = new File(configRoot, "system-model.cfg.xml")
    tempFile.deleteOnExit() //Mark it to be clobbered later
    writeFileContent(tempFile, content)
  }

  def writeContainerConfig(configRoot: String, content: String): File = {
    val tempFile = new File(configRoot, "container.cfg.xml")
    tempFile.deleteOnExit()
    writeFileContent(tempFile, content)
  }

  def writeFileContent(file: File, content: String): File = {
    Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
    file
  }

  /**
   * A scalaish adaptation of http://stackoverflow.com/questions/779519/delete-files-recursively-in-java/8685959#8685959
   * This is Java7 Dependent
   * As specified in the notes, it's a fail fast, rather than a try hardest.
   * That's okay, we shouldn't be using this outside of test directories
   * @param path
   * @return
   */
  def deleteRecursive(path: Path) = {
    Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Option(exc) match {
          case None =>
            Files.delete(dir)
            FileVisitResult.CONTINUE
          case Some(x) =>
            throw x
        }
      }
    })
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
