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

import java.util.Base64
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.{Matchers => MM}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientFactory
import org.openrepose.filters.samlpolicy.config.{Cache, PolicyAcquisition, SamlPolicyConfig}
import org.openrepose.nodeservice.atomfeed.AtomFeedService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.io.Source

/**
  * Created by adrian on 12/14/16.
  */
@RunWith(classOf[JUnitRunner])
class SamlPolicyTranslationFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  val atomFeedService: AtomFeedService = mock[AtomFeedService]

  var filter: SamlPolicyTranslationFilter =_

  override def beforeEach(): Unit = {
    filter = new SamlPolicyTranslationFilter(mock[ConfigurationService], atomFeedService, mock[AkkaServiceClientFactory])
  }

  describe("doWork") {
    ignore("should call the chain") {
      val request = mock[HttpServletRequest]
      val response = mock[HttpServletResponse]
      val chain = mock[FilterChain]

      filter.doWork(request, response, chain)

      verify(chain).doFilter(request, response)
    }
  }

  describe("decodeSamlResponse") {
    it("should throw a SamlPolicyException(400) if the SAMLResponse parameter is not present") {
      val request = mock[HttpServletRequest]

      intercept[SamlPolicyException] {
        filter.decodeSamlResponse(request)
      }.statusCode shouldEqual SC_BAD_REQUEST
    }

    it("should throw a SamlPolicyException(400) if the SAMLResponse value is not Base64 encoded") {
      val request = mock[HttpServletRequest]

      when(request.getParameter("SAMLResponse"))
        .thenReturn("<samlp:Response/>")

      intercept[SamlPolicyException] {
        filter.decodeSamlResponse(request)
      }.statusCode shouldEqual SC_BAD_REQUEST
    }

    it("should return the decoded SAMLResponse") {
      val samlResponse = "<samlp:Response/>"
      val request = mock[HttpServletRequest]

      when(request.getParameter("SAMLResponse"))
        .thenReturn(Base64.getEncoder.encodeToString(samlResponse.getBytes))

      val decodedSaml = filter.decodeSamlResponse(request)

      Source.fromInputStream(decodedSaml).mkString shouldEqual samlResponse
    }
  }

  describe("readToDom") {
    pending
  }

  describe("determineVersion") {
    pending
  }

  describe("validateResponseAndGetIssuer") {
    pending
  }

  describe("getPolicy") {
    pending
  }

  describe("translateResponse") {
    pending
  }

  describe("signResponse") {
    pending
  }

  describe("convertDocumentToStream") {
    pending
  }

  describe("onNewAtomEntry") {
    pending
  }

  describe("onLifecycleEvent") {
    pending
  }

  describe("configurationUpdated") {
    var config = new SamlPolicyConfig

    def prepTest() = {
      config = new SamlPolicyConfig
      val acquisition = new PolicyAcquisition
      val cache = new Cache
      cache.setAtomFeedId("banana")
      acquisition.setCache(cache)
      config.setPolicyAcquisition(acquisition)
      reset(atomFeedService)
    }

    it("should attempt to subscribe to the configured atom feed") {
      prepTest()
      filter.configurationUpdated(config)

      verify(atomFeedService).registerListener(MM.eq("banana"), MM.same(filter))
    }

    it("shouldn't try to change subscriptions when the feed didn't change") {
      prepTest()
      when(atomFeedService.registerListener(MM.eq("banana"), MM.same(filter))).thenReturn("thingy")

      filter.configurationUpdated(config)
      filter.configurationUpdated(config)

      verify(atomFeedService, times(1)).registerListener(MM.eq("banana"), MM.same(filter))
      verify(atomFeedService, never()).unregisterListener(MM.any[String])
    }

    it("should change subscription when the config changes") {
      prepTest()
      when(atomFeedService.registerListener(MM.eq("banana"), MM.same(filter))).thenReturn("thingy")
      filter.configurationUpdated(config)

      val newConfig = new SamlPolicyConfig
      val newAcquisition = new PolicyAcquisition
      val newCache = new Cache
      newCache.setAtomFeedId("phone")
      newAcquisition.setCache(newCache)
      newConfig.setPolicyAcquisition(newAcquisition)

      filter.configurationUpdated(newConfig)

      verify(atomFeedService).unregisterListener("thingy")
      verify(atomFeedService).registerListener(MM.eq("phone"), MM.same(filter))
    }

    it("should unsubscribe when the feed id is removed") {
      prepTest()
      when(atomFeedService.registerListener(MM.eq("banana"), MM.same(filter))).thenReturn("thingy")
      filter.configurationUpdated(config)

      val newConfig = new SamlPolicyConfig
      val newAcquisition = new PolicyAcquisition
      val newCache = new Cache
      newAcquisition.setCache(newCache)
      newConfig.setPolicyAcquisition(newAcquisition)

      filter.configurationUpdated(newConfig)

      verify(atomFeedService).unregisterListener("thingy")
      verify(atomFeedService, times(1)).registerListener(MM.any[String], MM.same(filter))
    }

    it("should not subscribe when there is no id") {
      prepTest()
      config.getPolicyAcquisition.getCache.setAtomFeedId(null)

      filter.configurationUpdated(config)

      verifyZeroInteractions(atomFeedService)
    }

    it("should subscribe when the config changes to have an id") {
      prepTest()
      config.getPolicyAcquisition.getCache.setAtomFeedId(null)
      filter.configurationUpdated(config)
      verifyZeroInteractions(atomFeedService)

      val newConfig = new SamlPolicyConfig
      val newAcquisition = new PolicyAcquisition
      val newCache = new Cache
      newCache.setAtomFeedId("phone")
      newAcquisition.setCache(newCache)
      newConfig.setPolicyAcquisition(newAcquisition)

      filter.configurationUpdated(newConfig)

      verify(atomFeedService).registerListener(MM.eq("phone"), MM.same(filter))
    }
  }
}
