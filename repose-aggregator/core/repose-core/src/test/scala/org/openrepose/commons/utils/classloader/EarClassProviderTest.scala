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
package org.openrepose.commons.utils.classloader

import java.io.{File, FileOutputStream, FileWriter, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import com.typesafe.config.ConfigFactory
import javax.servlet.Filter
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.apache.logging.log4j.{Level, LogManager}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.springframework.context.annotation.AnnotationConfigApplicationContext

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class EarClassProviderTest extends FunSpec with Matchers {

  val logContext = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val appender = logContext.getConfiguration.getAppender("List0").asInstanceOf[ListAppender]
  val testProps = ConfigFactory.load("test.properties")
  val version = testProps.getString("earFilesVersion")
  val earFile = getEarFile("core-test-filter-bundle")
  val random = new Random()

  def getEarFile(name: String): File = {
    new File(testProps.getString("earFilesLocation"), s"$name-$version.ear")
  }

  def unpackedArtifact(f: EarClassProvider => Unit) = {
    withTempDir { root =>
      val p = new EarClassProvider(earFile, root)
      f(p)
    }
  }

  def withTempDir(f: (File) => Unit) = {
    def tempDir(): File = {
      val f = Files.createTempDirectory("earUnpackRoot").toFile
      f.deleteOnExit()
      f
    }
    val t = tempDir()
    try {
      f(t)
    } finally {
      deleteRecursive(t.toPath)
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


  it("unpacks an ear to a directory successfully") {

    withTempDir { root =>
      val p = new EarClassProvider(earFile, root)

      p.getClassLoader()

      root.listFiles.toList shouldNot be(empty)

      val files = p.outputDir.listFiles.toList

      val libDir = new File(p.outputDir, "lib")
      libDir.listFiles.toList should contain(new File(libDir, s"core-test-filter-${version}.jar"))

      val webInfDir = new File(p.outputDir, "WEB-INF")
      webInfDir.listFiles.toList should contain(new File(webInfDir, "web-fragment.xml"))

    }
  }

  it("logs a warning if extraction failed") {
    import scala.collection.JavaConversions._

    appender.clear()

    val tempFile = File.createTempFile("junk", ".ear")
    tempFile.deleteOnExit()
    withTempDir { root =>
      val range = 0 to 2048
      val fos = new FileOutputStream(tempFile)
      range.foreach { i =>
        fos.write(Random.nextInt())
      }
      fos.close()

      val p = new EarClassProvider(tempFile, root)
      intercept[EarProcessingException] {
        p.getClassLoader()
      }

      //Verify that a warning was logged
      appender.getEvents.size() shouldBe 1

      val event = appender.getEvents.toList.head
      event.getLevel shouldBe Level.WARN


    }

  }

  it("throws an EarProcessingException if unable to unpack the ear to the specified directory") {
    //create garbage file
    val tempFile = File.createTempFile("junk", ".ear")
    tempFile.deleteOnExit()

    withTempDir { root =>
      val range = 0 to 2048
      val fos = new FileOutputStream(tempFile)
      range.foreach { i =>
        fos.write(Random.nextInt())
      }
      fos.close()

      val p = new EarClassProvider(tempFile, root)
      intercept[EarProcessingException] {
        p.getClassLoader()
      }
    }
  }

  it("throws an EarProcessingException if the Ear file doesn't exist") {
    withTempDir { root =>
      val notAFile = new File("this file doesn't exist.txt")

      val p = new EarClassProvider(notAFile, root)
      intercept[EarProcessingException] {
        p.getClassLoader()
      }
    }
  }

  it("if the unpack root doesn't exist we get an EarProcessingException") {
    val root = new File("/derp/derp/derp")

    val p = new EarClassProvider(earFile, root)

    intercept[EarProcessingException] {
      p.getClassLoader()
    }
  }

  it("provides a class that is not in the current classloader") {
    withTempDir { root =>

      val p = new EarClassProvider(earFile, root)
      val earClass = "org.openrepose.filters.core.test.TestFilter"

      intercept[ClassNotFoundException] {
        Class.forName(earClass)
      }

      val tehClass = p.getClassLoader().loadClass(earClass)

      tehClass shouldNot be(null)
      tehClass.getName shouldBe earClass

      intercept[ClassNotFoundException] {
        Class.forName(earClass)
      }

    }
  }

  it("throws a ClassNotFoundException when you ask for a class that isn't in the ear (or in the parent Classloader)") {
    unpackedArtifact { provider =>
      intercept[ClassNotFoundException] {
        provider.getClassLoader().loadClass("derp.derpclass.derp.derp.derp")
      }
    }
  }

  it("multiple ear files don't share classes") {
    withTempDir { outputDir1 =>
      withTempDir { outputDir2 =>
        val p1 = new EarClassProvider(earFile, outputDir1)

        //Second ear file
        val ear2 = getEarFile("second-filter-bundle")

        val p2 = new EarClassProvider(ear2, outputDir2)

        val ear1Class = "org.openrepose.filters.core.test.TestFilter"
        val ear2Class = "org.openrepose.filters.second.SecondFilter"

        intercept[ClassNotFoundException] {
          Class.forName(ear1Class)
        }
        intercept[ClassNotFoundException] {
          Class.forName(ear2Class)
        }

        val class1 = p1.getClassLoader().loadClass(ear1Class)
        class1.getName shouldBe ear1Class

        intercept[ClassNotFoundException] {
          p2.getClassLoader().loadClass(ear1Class)
        }

        val class2 = p2.getClassLoader().loadClass(ear2Class)
        class2.getName shouldBe ear2Class

        intercept[ClassNotFoundException] {
          p1.getClassLoader().loadClass(ear2Class)
        }
      }
    }
  }

  it("can get the web-fragment.xml") {
    withTempDir { root =>
      val p1 = new EarClassProvider(earFile, root)

      val resourcePath = "WEB-INF/web-fragment.xml"

      val webFragment = p1.getClassLoader().getResource(resourcePath)
      webFragment should not be (null)
    }
  }

  it("provides an EarDescriptor when an ear is properly formed") {
    withTempDir { root =>
      val p1 = new EarClassProvider(earFile, root)

      val descriptor = p1.getEarDescriptor()

      descriptor.getApplicationName shouldBe "core-test-filter-bundle"

      descriptor.getRegisteredFilters.keySet() should contain("test-filter")
    }
  }

  it("throws an EarProcessingException when the application name cannot be found") {
    withTempDir { root =>
      val p1 = new EarClassProvider(getEarFile("busted-application-name-ear"), root)

      intercept[EarProcessingException] {
        p1.getEarDescriptor()
      }
    }
  }
  it("throws an EarProcessingException when the web-fragment contains no filter/class mappings") {
    withTempDir { root =>
      val p1 = new EarClassProvider(getEarFile("busted-web-fragment-ear"), root)

      intercept[EarProcessingException] {
        p1.getEarDescriptor()
      }
    }
  }

  describe("in the context of spring") {
    it("when given to a AppContext beans are provided") {
      withTempDir { root =>
        val p1 = new EarClassProvider(earFile, root)

        val context = new AnnotationConfigApplicationContext()
        context.setClassLoader(p1.getClassLoader())

        context.scan("org.openrepose.filters.core.test")
        context.refresh()

        val beanClass: Class[Filter] = p1.getClassLoader().loadClass("org.openrepose.filters.core.test.TestFilter").asInstanceOf[Class[Filter]]

        beanClass shouldNot be(null)

        val bean = context.getBean[Filter](beanClass)

        bean shouldNot be(null)
      }
    }
  }

  it ("writes the artifact file if the artifact file exists but is not valid") {
    val packedArtifact = s"core-test-filter-$version.jar"
    val unpackedArtifactRelativePath = s"lib/$packedArtifact"

    withTempDir { root =>
      val artifactFile = new File(root, unpackedArtifactRelativePath)
      artifactFile.getParentFile.mkdirs()
      artifactFile.createNewFile()
      new FileWriter(artifactFile).append(random.nextString(1024)).close()

      val invalidArtifactCrc = FileUtils.checksumCRC32(artifactFile)

      val earClassProvider = new EarClassProvider(earFile, root)
      earClassProvider.getClassLoader()

      artifactFile should exist
      FileUtils.checksumCRC32(artifactFile) should not equal invalidArtifactCrc
    }
  }

  it("does not write an artifact file if the artifact file exists and is valid") {
    val packedArtifact = s"core-test-filter-$version.jar"
    val unpackedArtifactRelativePath = s"lib/$packedArtifact"

    withTempDir { root =>
      val artifactFile = new File(root, unpackedArtifactRelativePath)

      val earClassProvider = new EarClassProvider(earFile, root)
      earClassProvider.getClassLoader()

      val artifactModificationTime = artifactFile.lastModified()

      Thread.sleep(100)

      val earClassProvider2 = new EarClassProvider(earFile, root)
      earClassProvider2.getClassLoader()

      val artifactModificationTime2 = artifactFile.lastModified()

      artifactFile should exist
      artifactModificationTime shouldEqual artifactModificationTime2
    }
  }
}
