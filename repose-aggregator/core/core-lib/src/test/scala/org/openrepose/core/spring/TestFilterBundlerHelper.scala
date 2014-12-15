package org.openrepose.core.spring

import java.io.{FileFilter, File}
import java.net.URLClassLoader

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.filefilter.WildcardFileFilter

trait TestFilterBundlerHelper {
  val testProps = ConfigFactory.load("test.properties")

  lazy val testFilterBundleRoot = new File(testProps.getString("coreTestFilterBundleLocation"))

  lazy val testFilterBundleFile = new File(testFilterBundleRoot, "core-test-filter-bundle-" + testProps.getString("coreTestFilterBundleVersion") + ".ear")

}
