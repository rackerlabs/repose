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
package org.openrepose.filters.samlpolicy

import org.junit.runner.RunWith
import org.mockito.Mockito.{never, verify, when}
import org.mockito.{Matchers => MM}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class SamlPolicyProviderTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {
  var akkaServiceClient: AkkaServiceClient = _
  var akkaServiceClientFactory: AkkaServiceClientFactory = _
  var samlPolicyProvider: SamlPolicyProvider = _

  override def beforeEach(): Unit = {
    akkaServiceClient = mock[AkkaServiceClient]
    akkaServiceClientFactory = mock[AkkaServiceClientFactory]

    samlPolicyProvider = new SamlPolicyProvider(akkaServiceClientFactory)

    when(akkaServiceClientFactory.newAkkaServiceClient())
      .thenReturn(akkaServiceClient)
    when(akkaServiceClientFactory.newAkkaServiceClient(MM.anyString()))
      .thenReturn(akkaServiceClient)
  }

  describe("using") {
    it("should build a new service client when the token connection pool ID changes") {
      samlPolicyProvider.using(Some("foo"), Some("baz"))
      samlPolicyProvider.using(Some("bar"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes from None") {
      samlPolicyProvider.using(None, Some("baz"))
      samlPolicyProvider.using(Some("bar"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes to None") {
      samlPolicyProvider.using(Some("foo"), Some("baz"))
      samlPolicyProvider.using(None, Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient()
    }

    it("should not build a new service client if the token connection pool ID does not change") {
      samlPolicyProvider.using(Some("foo"), Some("baz"))
      samlPolicyProvider.using(Some("foo"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient, never).destroy()
    }

    it("should not build a new service client if the token connection pool ID does not change from/to null") {
      samlPolicyProvider.using(None, Some("baz"))
      samlPolicyProvider.using(None, Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient, never).destroy()
    }

    it("should build a new service client when the policy connection pool ID changes") {
      samlPolicyProvider.using(Some("baz"), Some("foo"))
      samlPolicyProvider.using(Some("baz"), Some("bar"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes from None") {
      samlPolicyProvider.using(Some("baz"), None)
      samlPolicyProvider.using(Some("baz"), Some("bar"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes to None") {
      samlPolicyProvider.using(Some("baz"), Some("foo"))
      samlPolicyProvider.using(Some("baz"), None)

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient()
    }

    it("should not build a new service client if the policy connection pool ID does not change") {
      samlPolicyProvider.using(Some("baz"), Some("foo"))
      samlPolicyProvider.using(Some("baz"), Some("foo"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient, never).destroy()
    }

    it("should not build a new service client if the policy connection pool ID does not change from/to null") {
      samlPolicyProvider.using(Some("baz"), None)
      samlPolicyProvider.using(Some("baz"), None)

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient, never).destroy()
    }
  }

  describe("getToken") {
    pending
  }

  describe("getPolicy") {
    pending
  }
}
