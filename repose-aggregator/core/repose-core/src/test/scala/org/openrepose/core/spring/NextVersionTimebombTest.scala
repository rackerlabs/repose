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
package org.openrepose.core.spring

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

/**
  * User: adrian
  * Date: 2/9/16
  * Time: 9:43 AM
  */
@RunWith(classOf[JUnitRunner])
class NextVersionTimebombTest extends FunSpec with Matchers with TestFilterBundlerHelper {

  val coreSpringProvider = CoreSpringProvider.getInstance()
  coreSpringProvider.initializeCoreContext("/etc/repose", false)

  describe("Repose Version") {
    it("is not 9 (timebomb)") {
      val reposeVersion = coreSpringProvider.getCoreContext.getEnvironment.getProperty(
        ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.REPOSE_VERSION))

      reposeVersion should not startWith "9"

      /*
       * Before moving to version 9, the following updates should be made:
       *
       * 1. Remove these attributes from openstack-identity-v3.xsd:
       *    a. token-cache-timeout
       *    b. groups-cache-timeout
       *    c. cache-offset
       *
       * 2. Remove the functional tests for the above attributes:
       *    a. IdentityV3CacheOffSetOldTest
       *    b. IdentityV3NoCacheOffSetOldTest
       *
       * 3. Remove these attributes from container-configuration.xsd:
       *    a. http-port
       *    b. https-port
       *
       * 4. Remove these values from the chunkedEncodingType enumeration in http-connection-pool.xsd:
       *    a. 1
       *    b. 0
       */
    }
  }
}
