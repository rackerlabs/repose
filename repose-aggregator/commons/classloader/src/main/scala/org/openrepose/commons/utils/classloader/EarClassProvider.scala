package org.openrepose.commons.utils.classloader

import java.io.{IOException, File, FileInputStream, FileOutputStream}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, SimpleFileVisitor, Files, Path}
import java.util.UUID
import java.util.zip.{ZipFile, ZipInputStream}

import org.slf4j.LoggerFactory

class EarClassProvider(earFile: File, unpackRoot: File) {
  val log = LoggerFactory.getLogger(classOf[EarClassProvider])

  val outputDir = new File(unpackRoot, UUID.randomUUID().toString)

  def unpack(): Unit = {
    try {
      if (!outputDir.exists()) {
        outputDir.mkdir()
      }

      //Make sure it's actually a zip file, so we can fail with some kind of exception
      new ZipFile(earFile)

      val zis = new ZipInputStream(new FileInputStream(earFile))

      val buffer = new Array[Byte](1024)
      Stream.continually(zis.getNextEntry).
        takeWhile(_ != null).foreach(entry =>
        //Unpack the entry
        if (entry.isDirectory) {
          new File(outputDir, entry.getName).mkdir()
        } else {
          val outFile = new File(outputDir, entry.getName)
          val ofs = new FileOutputStream(outFile)
          Stream.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(count => ofs.write(buffer, 0, count))
          ofs.close()
        }
        )
      zis.close()
    } catch {
      case e: Exception =>
        log.warn("Error during ear extraction! Partial extraction at {}", outputDir.getAbsolutePath)
        throw e
    }
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


}
