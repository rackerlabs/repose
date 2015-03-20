/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.core.spring

import java.io.{FileFilter, File}
import java.net.URLClassLoader

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.filefilter.WildcardFileFilter

trait TestFilterBundlerHelper {
  val testProps = ConfigFactory.load("test.properties")

  lazy val testFilterBundleRoot = new File(testProps.getString("earFilesLocation"))

  lazy val testFilterBundleFile = new File(testFilterBundleRoot, "core-test-filter-bundle-" + testProps.getString("earFilesVersion") + ".ear")

}
