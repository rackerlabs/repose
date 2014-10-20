package org.openrepose.core.spring

import java.io.{FileFilter, File}
import java.net.URLClassLoader

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.filefilter.WildcardFileFilter

trait TestFilterBundlerHelper {
  lazy val testFilterBundleRoot = new File(ConfigFactory.load("test.properties").getString("coreTestFilterBundleLocation"))

  lazy val testFilterBundleClassLoader: ClassLoader = {
    val fileFilter: FileFilter = new WildcardFileFilter("core-test-filter-*.jar")
    val files: Array[File] = testFilterBundleRoot.listFiles(fileFilter)
    new URLClassLoader(Array(files(0).toURI.toURL), getClass.getClassLoader)
  }

}
