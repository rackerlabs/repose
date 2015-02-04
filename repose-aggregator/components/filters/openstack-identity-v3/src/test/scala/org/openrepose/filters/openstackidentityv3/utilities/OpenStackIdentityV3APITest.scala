package org.openrepose.filters.openstackidentityv3.utilities

import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

import org.apache.http.message.BasicHeader
import org.hamcrest.Matchers.{equalTo, is, lessThanOrEqualTo, theInstance}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityService, OpenstackIdentityV3Config, ServiceEndpoint}
import org.openrepose.filters.openstackidentityv3.objects.{AuthenticateResponse, Group}
import org.openrepose.services.datastore.Datastore
import org.openrepose.services.serviceclient.akka.AkkaServiceClient
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers, PrivateMethodTester}

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3APITest extends FunSpec with BeforeAndAfter with Matchers with PrivateMethodTester with MockitoSugar {

  var identityV3API: OpenStackIdentityV3API = _
  var identityConfig: OpenstackIdentityV3Config = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockDatastore: Datastore = _

  before {
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
    val getAdminToken = PrivateMethod[Try[String]]('getAdminToken)

    it("should build a JSON auth token request without a domain ID") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_UNAUTHORIZED)
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API invokePrivate getAdminToken(true)

      verify(mockAkkaServiceClient).post(
        anyString,
        anyString,
        anyMap.asInstanceOf[java.util.Map[String, String]],
        contains("{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"user\",\"password\":\"password\"}}}}}"),
        any[MediaType]
      )
    }

    it("should build a JSON auth token request with a string domain ID") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      identityConfig.getOpenstackIdentityService.setProjectId("projectId")

      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures
      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_UNAUTHORIZED)

      identityV3API invokePrivate getAdminToken(true)

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

      identityV3API invokePrivate getAdminToken(true) shouldBe a[Failure[_]]
      identityV3API.invokePrivate(getAdminToken(true)).failed.get shouldBe a[InvalidAdminCredentialsException]
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate getAdminToken(true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getAdminToken(true)).get should startWith("test-admin-token")
    }

    it("should return an admin token as a string when the admin API call succeeds") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API invokePrivate getAdminToken(true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getAdminToken(true)).get should startWith("test-admin-token")
    }

    it("should return a new admin token (non-cached) if checkCache is set to false") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockDatastore.get(anyString)).thenReturn("test-cached-token", Nil: _*)
      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API invokePrivate getAdminToken(false) shouldBe a[Success[_]]
      identityV3API.invokePrivate(getAdminToken(false)).get should startWith("test-admin-token")
    }

    it("should cache an admin token when the admin API call succeeds") {
      val mockServiceClientResponse = mock[ServiceClientResponse]

      when(mockServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_CREATED)
      when(mockServiceClientResponse.getHeaders).thenReturn(Array(new BasicHeader(OpenStackIdentityV3Headers.X_SUBJECT_TOKEN, "test-admin-token")), Nil: _*)
      when(mockServiceClientResponse.getData).thenReturn(new ByteArrayInputStream("{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}".getBytes))
      when(mockAkkaServiceClient.post(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]], anyString, any(classOf[MediaType]))).
        thenReturn(mockServiceClientResponse, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      identityV3API invokePrivate getAdminToken(true)

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

      identityV3API invokePrivate getAdminToken(true)

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

      identityV3API invokePrivate validateSubjectToken("test-subject-token", true) shouldBe a[Failure[_]]
      an[InvalidSubjectTokenException] should be thrownBy identityV3API.invokePrivate(validateSubjectToken("test-subject-token", true)).get
    }

    it("should return a Success for a cached admin token") {
      when(mockDatastore.get(anyString)).thenReturn(AuthenticateResponse(null, null, null, null, null, null, null, null), Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(validateSubjectToken("test-subject-token", true)).get shouldBe an[AuthenticateResponse]
    }

    it("should return a token object when x-subject-token validation succeeds") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate validateSubjectToken("test-subject-token", true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(validateSubjectToken("test-subject-token", true)).get shouldBe an[AuthenticateResponse]
    }

    it("should correctly map the default region to the authentication response") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\", \"RAX-AUTH:defaultRegion\":\"ORD\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", true)
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

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", true)
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

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", true)
      response.get.rax_impersonator.get.id.get shouldBe "567"
      response.get.rax_impersonator.get.name.get shouldBe "impersonator.joe"
    }

    it("should correctly not populate an impersonation object if its not available") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"token\":{\"expires_at\":\"2013-02-27T18:30:59.999999Z\",\"issued_at\":\"2013-02-27T16:30:59.999999Z\",\"methods\":[\"password\"],\"user\":{\"domain\":{\"id\":\"1789d1\",\"links\":{\"self\":\"http://identity:35357/v3/domains/1789d1\"},\"name\":\"example.com\"},\"id\":\"0ca8f6\",\"links\":{\"self\":\"http://identity:35357/v3/users/0ca8f6\"},\"name\":\"Joe\"}}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      val response: Try[AuthenticateResponse] = identityV3API validateToken("test-subject-token", true)
      response.get.rax_impersonator shouldBe None
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

      identityV3API invokePrivate validateSubjectToken("test-subject-token", true)

      verify(mockDatastore).put(argThat(equalTo("IDENTITY:V3:TOKEN:test-subject-token")), any[Serializable], intThat(lessThanOrEqualTo((expirationTime.getMillis - currentTime.getMillis).toInt)), any[TimeUnit])
    }
  }

  describe("fetchGroups") {
    val fetchGroups = PrivateMethod[Try[List[_]]]('getGroups)

    it("should return a Failure when x-subject-token validation fails") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_NOT_FOUND)
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate fetchGroups("test-user-id", true) shouldBe a[Failure[_]]
    }

    it("should return a Success for cached groups") {
      when(mockDatastore.get(anyString)).thenReturn(List(Group("", "", "")).toBuffer.asInstanceOf[Serializable], Nil: _*)

      identityV3API invokePrivate fetchGroups("test-user-id", false) shouldBe a[Success[_]]
      identityV3API.invokePrivate(fetchGroups("test-user-id", false)).get shouldBe a[List[_]]
    }

    it("should return a list of groups when groups call succeeds") {
      val mockGetServiceClientResponse = mock[ServiceClientResponse]

      when(mockGetServiceClientResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)
      when(mockGetServiceClientResponse.getData).thenReturn(new ByteArrayInputStream(
        "{\"groups\":[{\"description\":\"Developersclearedforworkonallgeneralprojects\",\"domain_id\":\"--domain-id--\",\"id\":\"--group-id--\",\"links\":{\"self\":\"http://identity:35357/v3/groups/--group-id--\"},\"name\":\"Developers\"},{\"description\":\"Developersclearedforworkonsecretprojects\",\"domain_id\":\"--domain-id--\",\"id\":\"--group-id--\",\"links\":{\"self\":\"http://identity:35357/v3/groups/--group-id--\"},\"name\":\"SecureDevelopers\"}],\"links\":{\"self\":\"http://identity:35357/v3/users/--user-id--/groups\",\"previous\":null,\"next\":null}}"
          .getBytes))
      when(mockAkkaServiceClient.get(anyString, anyString, anyMap.asInstanceOf[java.util.Map[String, String]])).thenReturn(mockGetServiceClientResponse)
      when(mockDatastore.get(argThat(equalTo("IDENTITY:V3:ADMIN_TOKEN")))).thenReturn("test-admin-token", Nil: _*)

      identityV3API invokePrivate fetchGroups("test-user-id", true) shouldBe a[Success[_]]
      identityV3API.invokePrivate(fetchGroups("test-user-id", true)).get shouldBe a[List[_]]
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
