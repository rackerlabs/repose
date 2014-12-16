package org.openrepose.commons.utils.classloader

import java.io.{IOException, File, FileInputStream, FileOutputStream}
import java.net.{URL, URLClassLoader}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, SimpleFileVisitor, Files, Path}
import java.util.UUID
import java.util.zip.{ZipFile, ZipInputStream}
import scala.collection.mutable

import org.slf4j.LoggerFactory

class EarClassProvider(earFile: File, unpackRoot: File) {
  val log = LoggerFactory.getLogger(classOf[EarClassProvider])

  val outputDir = new File(unpackRoot, UUID.randomUUID().toString)

  private def unpack(): Unit = {
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
   * Calls unpack, and gets you a new classloader for all the items in this ear file
   */
  lazy val getClassLoader: ClassLoader = {
    unpack()

    val mutableList = mutable.MutableList[Path]()

    Files.walkFileTree(outputDir.toPath(), new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (file.toString.endsWith(".jar")) {
          mutableList += file
        }
        super.visitFile(file, attrs)
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        mutableList += dir
        super.postVisitDirectory(dir, exc)
      }
    })

    val fileUrls: Array[URL] = mutableList.map { path =>
      path.toUri.toURL
    }.toArray

    URLClassLoader.newInstance(fileUrls)
  }
}
