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

import java.util.concurrent.TimeUnit
import java.util.{Calendar, GregorianCalendar}

import javax.servlet.http.HttpServletResponse._
import javax.ws.rs.core.HttpHeaders.RETRY_AFTER
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods._
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpEntity, HttpVersion}
import org.hamcrest.Matchers.{equalTo, is, lessThanOrEqualTo, theInstance}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.test.HttpUriRequestMatchers.hasMethod
import org.openrepose.commons.utils.http.HttpDate
import org.openrepose.commons.utils.http.normal.ExtendedStatusCodes
import org.openrepose.core.services.datastore.Datastore
import org.openrepose.core.services.httpclient.HttpClientServiceClient
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityService, OpenstackIdentityV3Config, ServiceEndpoint}
import org.openrepose.filters.openstackidentityv3.objects.ValidToken
import org.openrepose.filters.openstackidentityv3.utilities.Cache._
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.Function.tupled
import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3APITest extends FunSpec with BeforeAndAfterEach with Matchers with PrivateMethodTester with MockitoSugar {

  var identityV3API: OpenStackIdentityV3API = _
  var identityConfig: OpenstackIdentityV3Config = _
  var mockHttpClient: HttpClientServiceClient = _
  var mockDatastore: Datastore = _

  override def beforeEach() = {
    mockHttpClient = mock[HttpClientServiceClient]
    mockDatastore = mock[Datastore]

    identityConfig = new OpenstackIdentityV3Config()
    identityConfig.setOpenstackIdentityService(new OpenstackIdentityService())
    identityConfig.getOpenstackIdentityService.setUsername("user")
    identityConfig.getOpenstackIdentityService.setPassword("password")
    identityConfig.getOpenstackIdentityService.setUri("http://test-uri.com")
    identityConfig.setServiceEndpoint(new ServiceEndpoint())
    identityConfig.getServiceEndpoint.setUrl("http://www.notreallyawebsite.com")

    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)

    identityV3API = new OpenStackIdentityV3API(identityConfig, mockDatastore, mockHttpClient)
  }

  describe("getAdminToken") {

    it("builds a JSON auth token request with a domain ID") {
      //Modify the identity config to include the domain
      identityConfig.getOpenstackIdentityService.setDomainId("867530nieeein")
      identityV3API = new OpenStackIdentityV3API(identityConfig, mockDatastore, mockHttpClient)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
          .thenReturn(makeResponse(SC_UNAUTHORIZED))

      identityV3API.getAdminToken(None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(requestCaptor.capture(), any[HttpContext])
      requestCaptor.getValue.getMethod shouldBe HttpPost.METHOD_NAME
      EntityUtils.toString(requestCaptor.getValue.getEntity) should include(
        """
          |{"auth":{"identity":{"methods":["password"],"password":{"user":{"domain":{"id":"867530nieeein"},"name":"user","password":"password"}}}}}
        """.stripMargin.trim)
    }

    it("should build a JSON auth token request without a project ID") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(SC_UNAUTHORIZED))

      identityV3API.getAdminToken(None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(requestCaptor.capture(), any[HttpContext])
      requestCaptor.getValue.getMethod shouldBe HttpPost.METHOD_NAME
      EntityUtils.toString(requestCaptor.getValue.getEntity) should include(
        """{"auth":{"identity":{"methods":["password"],"password":{"user":{"name":"user","password":"password"}}}}}""")
    }

    it("should build a JSON auth token request with a string project ID") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(SC_UNAUTHORIZED))

      identityConfig.getOpenstackIdentityService.setProjectId("projectId")
      identityV3API.getAdminToken(None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpEntityEnclosingRequestBase])
      verify(mockHttpClient).execute(requestCaptor.capture(), any[HttpContext])
      requestCaptor.getValue.getMethod shouldBe HttpPost.METHOD_NAME
      EntityUtils.toString(requestCaptor.getValue.getEntity) should include(
        """{"auth":{"identity":{"methods":["password"],"password":{"user":{"name":"user","password":"password"}}},"scope":{"project":{"id":"projectId"}}}}""")
    }

    it("should return a Failure when unable to retrieve admin token") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(SC_UNAUTHORIZED))

      identityV3API.getAdminToken(None, checkCache = true) shouldBe a[Failure[_]]
      identityV3API.getAdminToken(None, checkCache = true).failed.get shouldBe a[InvalidAdminCredentialsException]
    }

    val statusCodes = List(SC_REQUEST_ENTITY_TOO_LARGE, ExtendedStatusCodes.SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      describe(s"should return an Exception when receiving $statusCode and") {
        it("not having headers while retrieving admin token") {
          when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
            .thenReturn(makeResponse(statusCode))

          val value = identityV3API.getAdminToken(None, checkCache = true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldNot be(null)
        }

        it("having headers while retrieving admin token") {
          val retryCalendar = new GregorianCalendar()
          retryCalendar.add(Calendar.SECOND, 5)
          val retryString = new HttpDate(retryCalendar.getTime).toRFC1123
          when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
            .thenReturn(makeResponse(statusCode, headers = Map(RETRY_AFTER -> Seq(retryString))))

          val value = identityV3API.getAdminToken(None, checkCache = true)
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

      identityV3API.getAdminToken(None, checkCache = true) shouldBe a[Success[_]]
      identityV3API.getAdminToken(None, checkCache = true).get should startWith("test-admin-token")
    }

    it("should return an admin token as a string when the admin API call succeeds") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_CREATED,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build(),
          Map(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> Seq("test-admin-token"))))

      identityV3API.getAdminToken(None, checkCache = true) shouldBe a[Success[_]]
      identityV3API.getAdminToken(None, checkCache = true).get should startWith("test-admin-token")
    }

    it("should return a new admin token (non-cached) if checkCache is set to false") {
      when(mockDatastore.get(anyString)).thenReturn("test-cached-token", Nil: _*)
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_CREATED,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build(),
          Map(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> Seq("test-admin-token"))))

      identityV3API.getAdminToken(None, checkCache = false) shouldBe a[Success[_]]
      identityV3API.getAdminToken(None, checkCache = false).get should startWith("test-admin-token")
    }

    it("should cache an admin token when the admin API call succeeds") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_CREATED,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build(),
          Map(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> Seq("test-admin-token"))))

      identityV3API.getAdminToken(None, checkCache = true)

      verify(mockDatastore).put(argThat(equalTo(AdminTokenKey)), argThat(equalTo("test-admin-token")), anyInt(), any[TimeUnit])
    }

    it("should cache an admin token with the right TTL") {
      val currentTime = DateTime.now()
      val expirationTime = currentTime.plusMillis(100000)
      val returnJson = s"""{"token":{"expires_at":"${ISODateTimeFormat.dateTime().print(expirationTime)}","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}"""

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_CREATED,
          EntityBuilder.create()
            .setText(returnJson)
            .build(),
          Map(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN -> Seq("test-admin-token"))))

      identityV3API.getAdminToken(None, checkCache = true)

      verify(mockDatastore).put(argThat(equalTo(AdminTokenKey)), argThat(equalTo("test-admin-token")), intThat(lessThanOrEqualTo((expirationTime.getMillis - currentTime.getMillis).toInt)), argThat(is(theInstance(TimeUnit.MILLISECONDS))))
    }
  }

  describe("validateSubjectToken") {
    val validateSubjectToken = PrivateMethod[Try[_]]('validateToken)

    it("should return a Failure when x-subject-token validation fails") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
          .thenReturn(makeResponse(SC_NOT_FOUND))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true) shouldBe a[Failure[_]]
      an[InvalidSubjectTokenException] should be thrownBy identityV3API.invokePrivate(validateSubjectToken("test-subject-token", None, true)).get
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn(ValidToken(null, null, null, null, null, null, null, null, null, None, None), Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(validateSubjectToken("test-subject-token", None, true)).get shouldBe an[ValidToken]
    }

    it("should return a token object when x-subject-token validation succeeds") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(validateSubjectToken("test-subject-token", None, true)).get shouldBe an[ValidToken]
    }

    it("should correctly map the default region to the authentication response") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe", "RAX-AUTH:defaultRegion":"ORD"}}}""")
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[ValidToken] = identityV3API validateToken("test-subject-token", None, true)
      response.get.defaultRegion shouldBe Some("ORD")
    }

    it("should correctly map none to the default region if there is not one provided") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[ValidToken] = identityV3API validateToken("test-subject-token", None, true)
      response.get.defaultRegion shouldBe None
    }

    it("should correctly create an impersonation object from the authentication response") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText("""{"token":{"RAX-AUTH:impersonator":{ "id": "567", "name": "impersonator.joe"}, "expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[ValidToken] = identityV3API validateToken("test-subject-token", None, true)
      response.get.impersonatorId.get shouldBe "567"
      response.get.impersonatorName.get shouldBe "impersonator.joe"
    }

    it("should correctly not populate an impersonation object if its not available") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText("""{"token":{"expires_at":"2013-02-27T18:30:59.999999Z","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}""")
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[ValidToken] = identityV3API validateToken("test-subject-token", None, true)
      response.get.impersonatorId shouldBe None
      response.get.impersonatorName shouldBe None
    }

    it("should cache a token object when x-subject-token validation succeeds with the correct TTL") {
      val currentTime = DateTime.now()
      val expirationTime = currentTime.plusMillis(100000)
      val returnJson = s"""{"token":{"expires_at":"${ISODateTimeFormat.dateTime().print(expirationTime)}","issued_at":"2013-02-27T16:30:59.999999Z","methods":["password"],"user":{"domain":{"id":"1789d1","links":{"self":"http://identity:35357/v3/domains/1789d1"},"name":"example.com"},"id":"0ca8f6","links":{"self":"http://identity:35357/v3/users/0ca8f6"},"name":"Joe"}}}"""

      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(returnJson)
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", None, true)

      verify(mockDatastore).put(argThat(equalTo("IDENTITY:V3:TOKEN:test-subject-token")), any[Serializable], intThat(lessThanOrEqualTo((expirationTime.getMillis - currentTime.getMillis).toInt)), any[TimeUnit])
    }

    val statusCodes = List(SC_REQUEST_ENTITY_TOO_LARGE, ExtendedStatusCodes.SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      describe(s"should return an Exception when receiving $statusCode and") {
        it("not having headers while retrieving admin token") {
          when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
            .thenReturn(makeResponse(statusCode))

          val value = identityV3API.validateToken("test-subject-token", None, checkCache = true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldNot be(null)
        }

        it("having headers while retrieving admin token") {
          val retryCalendar = new GregorianCalendar()
          retryCalendar.add(Calendar.SECOND, 5)
          val retryString = new HttpDate(retryCalendar.getTime).toRFC1123

          when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
            .thenReturn(makeResponse(statusCode, headers = Map(RETRY_AFTER -> Seq(retryString))))

          val value = identityV3API.validateToken("test-subject-token", None, checkCache = true)
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
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(SC_NOT_FOUND))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate getGroups("test-user-id", "test-token", None, true) shouldBe a[Failure[_]]
    }

    val statusCodes = List(SC_REQUEST_ENTITY_TOO_LARGE, ExtendedStatusCodes.SC_TOO_MANY_REQUESTS)
    statusCodes.foreach { statusCode =>
      describe(s"should return an Exception when receiving $statusCode and") {
        it("not having headers while retrieving admin token") {
          when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
            .thenReturn(makeResponse(statusCode))

          val value = identityV3API.getGroups("test-user-id", "test-token", None, checkCache = true)
          value shouldBe a[Failure[_]]
          val throwable = value.failed.get
          throwable.isInstanceOf[IdentityServiceOverLimitException]
          val ex = throwable.asInstanceOf[IdentityServiceOverLimitException]
          ex.getMessage shouldBe "Rate limited by OpenStack Identity service"
          ex.getStatusCode shouldBe statusCode
          ex.getRetryAfter shouldNot be(null)
        }

        it("having headers while retrieving admin token") {
          val retryCalendar = new GregorianCalendar()
          retryCalendar.add(Calendar.SECOND, 5)
          val retryString = new HttpDate(retryCalendar.getTime).toRFC1123

          when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
            .thenReturn(makeResponse(statusCode, headers = Map(RETRY_AFTER -> Seq(retryString))))

          val value = identityV3API.getGroups("test-user-id", "test-token", None, checkCache = true)
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

      identityV3API invokePrivate getGroups("test-user-id", "test-token", None, false) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getGroups("test-user-id", "test-token", None, false)).get shouldBe a[List[_]]
    }

    it("should return a list of groups when groups call succeeds") {
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText("""{"groups":[{"description":"Developersclearedforworkonallgeneralprojects","domain_id":"--domain-id--","id":"--group-id--","links":{"self":"http://identity:35357/v3/groups/--group-id--"},"name":"Developers"},{"description":"Developersclearedforworkonsecretprojects","domain_id":"--domain-id--","id":"--group-id--","links":{"self":"http://identity:35357/v3/groups/--group-id--"},"name":"SecureDevelopers"}],"links":{"self":"http://identity:35357/v3/users/--user-id--/groups","previous":null,"next":null}}""")
            .build()
        ))
      when(mockDatastore.get(argThat(equalTo(AdminTokenKey)))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate getGroups("test-user-id", "test-token", None, true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getGroups("test-user-id", "test-token", None, true)).get shouldBe a[List[_]]
    }
  }

  def makeResponse(statusCode: Int, entity: HttpEntity = null, headers: Map[String, Seq[String]] = Map.empty): CloseableHttpResponse = {
    val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, statusCode, null) with CloseableHttpResponse {
      override def close(): Unit = {}
    }
    Option(entity).foreach(response.setEntity)
    headers.foldLeft(Seq.empty[(String, String)]) { case (aggregate, (headerName, headerValues)) =>
      aggregate ++ headerValues.map(headerName -> _)
    }.foreach(tupled(response.addHeader))
    response
  }
}
