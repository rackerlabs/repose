package org.openrepose.core.spring

import java.io.{FileFilter, File}
import java.net.URLClassLoader

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.filefilter.WildcardFileFilter

trait TestFilterBundlerHelper {
  val testProps = ConfigFactory.load("test.properties")

  lazy val testFilterBundleRoot = new File(testProps.getString("coreTestFilterBundleLocation"))

  lazy val testFilterBundleFile = new File(testFilterBundleRoot, "core-test-filter-bundle-" + testProps.getString("coreTestFilterBundleVersion") + ".ear")

  lazy val testFilterBundleClassLoader: ClassLoader = {
    val fileFilter: FileFilter = new WildcardFileFilter("core-test-filter-*.jar")
    val files: Array[File] = testFilterBundleRoot.listFiles(fileFilter)
    new URLClassLoader(Array(files(0).toURI.toURL), getClass.getClassLoader)
  }

}
