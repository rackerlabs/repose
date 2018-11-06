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
       * 4. Remove the flush output filter
       *
       * 5. Extract common XML types (e.g., keystore configuration).
       *
       * 6. Remove the Container Configuration's `cluster-config` element's deprecated `via` attribute.
       *    a. This needs done in the XSD.
       *    b. There will also be some tests that should be removed also.
       *
       * 7. Allow the Container config to provide empty [in|ex]cluded-[protocols|ciphers].
       *
       * 8. Remove the uri-extraction-regex attribute from the Keystone filters' configuration.
       *
       * 9. For Keystone Authorization, when the default tenant ID matches a request tenant, use the configured request tenant quality rather than using the higher of the request tenant and default tenant qualities.
       *     Before doing so, verify that this behavior is not useful.
       *
       * 10. Remove the population of X-Auth-Token-Key from Keystone v2 Filter.
       *
       * 11. The following classes should all be obsoleted when the `ReposeRoutingServlet` is put to use:
       *     a. PowerFilterRouter
       *     b. PowerFilterRouterImpl
       *     c. PowerFilterRouterFactory
       *     d. DispatchPathBuilder
       *     e. RoutingService
       *     f. RoundRobinRoutingService
       *     g. DestinationLocation
       *     h. DestinationLocationBuilder
       *     i. EndpointUriBuilder
       *     j. EndpointUrlBuilder
       *     k. RequestHeaderService
       *     l. RequestHeaderServiceImpl
       *     m. ResponseHeaderService
       *     n. ResponseHeaderServiceImpl
       *     o. Port
       */
    }

    it("is not 10 (timebomb)") {
      val reposeVersion = coreSpringProvider.getCoreContext.getEnvironment.getProperty(
        ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.REPOSE_VERSION))

      reposeVersion should not startWith "10"

      /*
       * Before moving to version 10, the following updates should be made:
       *
       * 1. Update the Header Normalization filters deprecated items:
       *    a. Remove the deprecated `header-filters` element.
       *    b. Update the `target` element's min to be One (1).
       *    c. Remove the deprecated top-level `whitelist` & `blacklist` elements.
       *    d. Remove the `HttpHeaderList` (`whitelist` & `blacklist`) element's id attribute.
       *    e. Remove the old tests and conditional test
       *
       * 2. Remove authorization functionality from the Keystone v2 authentication filter.
       *    a. Push the shared authorization schema stuff into the authorization filter schema.
       *    b. Push the shared authorization code into the authorization filter itself.
       *    c. Remove the configuration deprecation warnings.
       *    d. Remove the versioned documentation deprecation warnings.
       *    e. Rename the keystone-v2 filter to keystone-v2-authentication.
       */
    }
  }
}
