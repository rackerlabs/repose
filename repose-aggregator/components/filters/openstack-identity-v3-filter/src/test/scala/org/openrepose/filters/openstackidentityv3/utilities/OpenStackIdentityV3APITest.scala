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
package org.openrepose.filters.openstackidentityv3.utilities

import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.{Calendar, GregorianCalendar}
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import org.apache.http.Header
import org.apache.http.message.BasicHeader
import org.hamcrest.Matchers.{equalTo, is, lessThanOrEqualTo, theInstance}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.normal.ExtendedStatusCodes
import org.openrepose.commons.utils.http.{HttpDate, ServiceClientResponse}
import org.openrepose.core.services.datastore.Datastore
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityService, OpenstackIdentityV3Config, ServiceEndpoint}
import org.openrepose.filters.openstackidentityv3.objects.AuthenticateResponse
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.springframework.http.HttpHeaders

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3APITest extends FunSpec with BeforeAndAfterEach with Matchers with PrivateMethodTester with MockitoSugar {

  var identityV3API: OpenStackIdentityV3API = _
  var identityConfig: OpenstackIdentityV3Config = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockDatastore: Datastore = _

  override def beforeEach() = {
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockDatastore = mock[Datastore]
    identityConfig = new OpenstackIdentityV3Config()
    identityConfig.setOpenstackIdentityService(new OpenstackIdentityService())
    identityConfig.getOpenstackIdentityService.setUsername("user")
    identityConfig.getOpenstackIdentityService.setPassword("password")
    identityConfig.getOpenstackIdentityService.setUri("http://test-uri.com")
    identityConfig.setServiceEndpoint(new ServiceEndpoint())
    identityConfig.getServiceEndpoint.setUrl("http://www.notreallyawebsite.com")

    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)

    identityV3API = new OpenStackIdentityV3API(identityConfig, mockDatastore, mockAkkaServiceClient)
  }

  describe("getAdminToken") {

    it("builds a JSON auth token request with a domain ID"){
      //Modify the identity config to include the domain
      identityConfig.getOpenstackIdentityService.setDomainId("867530nieeein")
      identityV3API = new OpenStackIdentityV3API(identityConfig, mockDatastore, mockAkkaServiceClient)

      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_UNAUTHORIZED)
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, true)

      verify(mockAkkaServiceClient).post(
        anyString,
        anyString,
        anyMap.asInstanceOf[java.util.Map[String, String]],
        contains(
          """
            |{"auth":{"identity":{"methods":["password"],"password":{"user":{"domain":{"id":"867530nieeein"},"name":"user","password":"password"}}}}}
          """.stripMargin.trim
        ),
        any[MediaType]
      )
    }

    it("should build a JSON auth token request without a project ID") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_UNAUTHORIZED)
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, true)

      verify(mockAkkaServiceClient).post(
        anyString,
        anyString,
        anyMap.asInstanceOf[java.util.Map[String, String]],
        contains("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}"),
        any[MediaType]
      )
    }

    it("should build a JSON auth token request with a string project ID") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      identityConfig.getOpenstackIdentityService.setProjectId("projectId")

      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures
      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_UNAUTHORIZED)

      identityV3API.getAdminToken(None, true)

      verify(mockAkkaServiceClient).post(
        anyString,
        anyString,
        anyMap.asInstanceOf[java.util.Map[String, String]],
        contains("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}},\"scope\":{\"project\":{\"id\":\"projectId\"}}}}"),
        any[MediaType]
      )
    }

    it("should return a Failure when unable to retrieve admin token") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_UNAUTHORIZED)
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, true) shouldBe a[Failure[_]]
      identityV3API.getAdminToken(None, true).failed.get shouldBe a[InvalidAdminCredentialsException]
    }

    val statusCodes = List(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, ExtendedStatusCodes.SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      describe(s"should return an Exception when receiving $statusCode and") {
        it("not having headers while retrieving admin token") {
          val mockServiceClientResponse = mock[ServiceClientResponse]

          when(mockServiceClientResponse.getStatus).thenReturn(statusCode)
          when(mockServiceClientResponse.getHeaders).thenReturn(List.empty[Header].toArray)
          when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
            thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

          val value = identityV3API.getAdminToken(None, true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldNot be(null)
        }

        it("having headers while retrieving admin token") {
          val mockServiceClientResponse = mock[ServiceClientResponse]

          when(mockServiceClientResponse.getStatus).thenReturn(statusCode)
          val mockHeader = mock[Header]
          val retryCalendar = new GregorianCalendar()
          retryCalendar.add(Calendar.SECOND, 5)
          val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
          when(mockHeader.getName).thenReturn(HttpHeaders.RETRY_AFTER)
          when(mockHeader.getValue).thenReturn(retryString)
          when(mockServiceClientResponse.getHeaders).thenReturn(List(mockHeader).toArray)
          when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
            thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

          val value = identityV3API.getAdminToken(None, true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldBe retryString
        }
      }
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn("test-admin-token", Nil: _*)

      identityV3API.getAdminToken(None, true) shouldBe a[Success[_]]
      identityV3API.getAdminToken(None, true).get should startWith("test-admin-token")
    }

    it("should return an admin token as a string when the admin API call succeeds") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, true) shouldBe a[Success[_]]
      identityV3API.getAdminToken(None, true).get should startWith("test-admin-token")
    }

    it("should return a new admin token (non-cached) if checkCache is set to false") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockDatastore.get(anyString)).thenReturn("test-cached-token", Nil: _*)
      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, false) shouldBe a[Success[_]]
      identityV3API.getAdminToken(None, false).get should startWith("test-admin-token")
    }

    it("should cache an admin token when the admin API call succeeds") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, true)

      verify(mockDatastore).put(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")), argThat(equalTo("test-admin-token")), anyInt(), any[TimeUnit])
    }

    it("should cache an admin token with the right TTL") {
      val mockServiceClientResponse = mock[ServiceClientResponse]
      val currentTime = DateTime.now()
      val expirationTime = currentTime.plusMillis(100000)
      val returnJson = "{\"token\":{\"expires_at\":\"" + ISODateTimeFormat.dateTime().print(expirationTime) + "\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(returnJson.getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API.getAdminToken(None, true)

      verify(mockDatastore).put(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")), argThat(equalTo("test-admin-token")), intThat(lessThanOrEqualTo((expirationTime.getMillis - currentTime.getMillis).toInt)), argThat(is(theInstance(TimeUnit.MILLISECONDS))))
    }
  }

  describe("validateSubjectToken") {
    val validateSubjectToken = PrivateMethod[Try[_]]('validateToken)

    it("should return a Failure when x-subject-token validation fails") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_NOT_FOUND)
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true) shouldBe a[Failure[_]]
      an[InvalidSubjectTokenException] should be thrownBy identityV3API.invokePrivate(validateSubjectToken("test-subject-token", None, true)).get
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn(AuthenticateResponse(null, null, null, null, null, null, None, None), Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(validateSubjectToken("test-subject-token", None, true)).get shouldBe an[AuthenticateResponse]
    }

    it("should return a token object when x-subject-token validation succeeds") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(validateSubjectToken("test-subject-token", None, true)).get shouldBe an[AuthenticateResponse]
    }

    it("should correctly map the default region to the authentication response") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\", \"RAX-AUTH:defaultRegion\":\"ORD\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", None, true)
      response.get.user.rax_default_region shouldBe Some("ORD")
    }

    it("should correctly map none to the default region if there is not one provided") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", None, true)
      response.get.user.rax_default_region shouldBe None
    }

    it("should correctly create an impersonation object from the authentication response") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"RAX-AUTH:impersonator\":{ \"id\": \"567\", \"name\": \"impersonator.joe\"}, \"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", None, true)
      response.get.impersonatorId.get shouldBe "567"
      response.get.impersonatorName.get shouldBe "impersonator.joe"
    }

    it("should correctly not populate an impersonation object if its not available") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", None, true)
      response.get.impersonatorId shouldBe None
      response.get.impersonatorName shouldBe None
    }

    it("should cache a token object when x-subject-token validation succeeds with the correct TTL") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]
      val currentTime = DateTime.now()
      val expirationTime = currentTime.plusMillis(100000)
      val returnJson = "{\"token\":{\"expires_at\":\"" + ISODateTimeFormat.dateTime().print(expirationTime) + "\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(returnJson.getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true)

      verify(mockDatastore).put(argThat(equalTo("IDENTITY:V3:TOKEN:test-subject-token")), any[Serializable], intThat(lessThanOrEqualTo((expirationTime.getMillis - currentTime.getMillis).toInt)), any[TimeUnit])
    }

    val statusCodes = List(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, ExtendedStatusCodes.SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      describe(s"should return an Exception when receiving $statusCode and") {
        it("not having headers while retrieving admin token") {
          val mockServiceClientResponse = mock[ServiceClientResponse]

          when(mockServiceClientResponse.getStatus).thenReturn(statusCode)
          when(mockServiceClientResponse.getHeaders).thenReturn(List.empty[Header].toArray)
          when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
            thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

          val value = identityV3API.validateToken("test-subject-token", None, true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldNot be(null)
        }

        it("having headers while retrieving admin token") {
          val mockServiceClientResponse = mock[ServiceClientResponse]

          when(mockServiceClientResponse.getStatus).thenReturn(statusCode)
          val mockHeader = mock[Header]
          val retryCalendar = new GregorianCalendar()
          retryCalendar.add(Calendar.SECOND, 5)
          val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
          when(mockHeader.getName).thenReturn(HttpHeaders.RETRY_AFTER)
          when(mockHeader.getValue).thenReturn(retryString)
          when(mockServiceClientResponse.getHeaders).thenReturn(List(mockHeader).toArray)
          when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
            thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

          val value = identityV3API.validateToken("test-subject-token", None, true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldBe retryString
        }
      }
    }
  }

  describe("getGroups") {
    val getGroups = PrivateMethod[Try[List[_]]]('getGroups)

    it("should return a Failure when x-subject-token validation fails") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_NOT_FOUND)
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate getGroups("test-user-id", None, true) shouldBe a[Failure[_]]
    }

    val statusCodes = List(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, ExtendedStatusCodes.SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      describe(s"should return an Exception when receiving $statusCode and") {
        it("not having headers while retrieving admin token") {
          val mockServiceClientResponse = mock[ServiceClientResponse]

          when(mockServiceClientResponse.getStatus).thenReturn(statusCode)
          when(mockServiceClientResponse.getHeaders).thenReturn(List.empty[Header].toArray)
          when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
            thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

          val value = identityV3API.getGroups("test-user-id", None, true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldNot be(null)
        }

        it("having headers while retrieving admin token") {
          val mockServiceClientResponse = mock[ServiceClientResponse]

          when(mockServiceClientResponse.getStatus).thenReturn(statusCode)
          val mockHeader = mock[Header]
          val retryCalendar = new GregorianCalendar()
          retryCalendar.add(Calendar.SECOND, 5)
          val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
          when(mockHeader.getName).thenReturn(HttpHeaders.RETRY_AFTER)
          when(mockHeader.getValue).thenReturn(retryString)
          when(mockServiceClientResponse.getHeaders).thenReturn(List(mockHeader).toArray)
          when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
            thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

          val value = identityV3API.getGroups("test-user-id", None, true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldBe retryString
        }
      }
    }

    it("should return a Success for cached groups") {
      when(mockDatastore.get(anyString)).thenReturn(List("").asInstanceOf[Serializable], Nil: _*)

      identityV3API invokePrivate getGroups("test-user-id", None, false) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getGroups("test-user-id", None, false)).get shouldBe a[List[_]]
    }

    it("should return a list of groups when groups call succeeds") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"groups\":[{\"description\":\"Developersclearedforworkonallgeneralprojects\",\"domain_id\":\"--domain-id--\",\"id\":\"--group-id--\",\"links\":{\"self\":\"http://identity:35357/v3/groups/--group-id--\"},\"name\":\"Developers\"},{\"description\":\"Developersclearedforworkonsecretprojects\",\"domain_id\":\"--domain-id--\",\"id\":\"--group-id--\",\"links\":{\"self\":\"http://identity:35357/v3/groups/--group-id--\"},\"name\":\"SecureDevelopers\"}],\"links\":{\"self\":\"http://identity:35357/v3/users/--user-id--/groups\",\"previous\":null,\"next\":null}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate getGroups("test-user-id", None, true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getGroups("test-user-id", None, true)).get shouldBe a[List[_]]
    }
  }

  describe("offsetTtl") {
    val offsetTtl = PrivateMethod[Int]('offsetTtl)

    it("should return the configured ttl is offset is 0") {
      identityV3API invokePrivate offsetTtl(1000, 0) shouldBe 1000
    }

    it("should return 0 if the configured ttl is 0") {
      identityV3API invokePrivate offsetTtl(0, 1000) shouldBe 0
    }

    it("should return a random int between configured ttl +/- offset") {
      val firstCall = identityV3API invokePrivate offsetTtl(1000, 100)
      val secondCall = identityV3API invokePrivate offsetTtl(1000, 100)
      val thirdCall = identityV3API invokePrivate offsetTtl(1000, 100)

      firstCall shouldBe 1000 +- 100
      secondCall shouldBe 1000 +- 100
      thirdCall shouldBe 1000 +- 100
      firstCall should (not be secondCall or not be thirdCall)
    }
  }
}
