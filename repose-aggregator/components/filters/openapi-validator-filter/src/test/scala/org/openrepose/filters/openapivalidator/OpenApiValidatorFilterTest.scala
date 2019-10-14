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
package org.openrepose.filters.openapivalidator

import java.io.{ByteArrayInputStream, File, InputStream}
import java.nio.charset.StandardCharsets

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.OpenApiInteractionValidator.ApiLoadException
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.report.{ValidationReport, ValidationReportScala}
import javax.servlet.http.HttpServletResponse
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.{spy, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.openapivalidator.HttpServletValidatorRequest.RequestConversionException
import org.openrepose.filters.openapivalidator.OpenApiValidatorFilterTest._
import org.openrepose.filters.openapivalidator.config.OpenApiValidatorConfig
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

import scala.io.Source
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class OpenApiValidatorFilterTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  final val ConfigRoot: String = "unused"

  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _
  var validator: OpenApiInteractionValidator = _
  var configurationService: ConfigurationService = _
  var openApiValidatorFilter: OpenApiValidatorFilter = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    servletRequest = new MockHttpServletRequest()
    servletResponse = new MockHttpServletResponse()
    filterChain = new MockFilterChain()

    validator = mock[OpenApiInteractionValidator]
    configurationService = mock[ConfigurationService]

    when(validator.validateRequest(any[Request]))
      .thenReturn(ValidationReportScala.empty)

    openApiValidatorFilter = new OpenApiValidatorFilter(ConfigRoot, configurationService)
    openApiValidatorFilter.validator = validator
  }

  describe("doWork") {
    it("should pass on a request with no validation issues") {
      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest should not be null
    }

    it("should wrap the request input stream if mark is not supported") {
      when(validator.validateRequest(any[Request]))
        .thenAnswer((request: Request) => {
          // Simulate validating the body
          request.getBody
          ValidationReportScala.empty
        })

      val testInputStream = spy(new ServletInputStreamWrapper(new ByteArrayInputStream("test data".getBytes(StandardCharsets.ISO_8859_1))))
      servletRequest = spy(new MockHttpServletRequest())

      when(testInputStream.markSupported).thenReturn(false)
      when(servletRequest.getInputStream).thenReturn(testInputStream)

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest should not be null
      filterChain.getRequest.getInputStream should not be testInputStream
    }

    it("should not wrap the request input stream if mark is supported") {
      when(validator.validateRequest(any[Request]))
        .thenAnswer((request: Request) => {
          // Simulate validating the body
          request.getBody
          ValidationReportScala.empty
        })

      val testInputStream = spy(new ServletInputStreamWrapper(new ByteArrayInputStream("test data".getBytes(StandardCharsets.ISO_8859_1))))
      servletRequest = spy(new MockHttpServletRequest())

      when(testInputStream.markSupported).thenReturn(true)
      when(servletRequest.getInputStream).thenReturn(testInputStream)

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest should not be null
      filterChain.getRequest.getInputStream shouldBe testInputStream
    }

    it("should allow downstream components to call getInputStream to read the request body") {
      when(validator.validateRequest(any[Request]))
        .thenAnswer((request: Request) => {
          // Simulate validating the body
          request.getBody
          ValidationReportScala.empty
        })

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest should not be null
      noException should be thrownBy filterChain.getRequest.getInputStream
    }

    it("should not allow downstream components to call getReader to read the request body") {
      when(validator.validateRequest(any[Request]))
        .thenAnswer((request: Request) => {
          // Simulate validating the body
          request.getBody
          ValidationReportScala.empty
        })

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest should not be null
      an[IllegalStateException] should be thrownBy filterChain.getRequest.getReader
    }

    it("should not destructively read the request body") {
      when(validator.validateRequest(any[Request]))
        .thenAnswer((request: Request) => {
          // Simulate validating the body
          request.getBody
          ValidationReportScala.empty
        })

      val body = "test data"
      servletRequest.setContent(body.getBytes(StandardCharsets.ISO_8859_1))

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest should not be null
      inputStreamToString(filterChain.getRequest.getInputStream, StandardCharsets.ISO_8859_1.name) shouldBe body
    }

    OpenApiValidatorFilter.ValidationIssues.foreach { case (issueKey, statusCode) =>
      it(s"should respond with a $statusCode when a $issueKey message is reported") {
        val message = ValidationReportScala.Message.create(issueKey, "test message")
        when(validator.validateRequest(any[Request]))
          .thenReturn(ValidationReportScala.singleton(message))

        openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

        verify(validator).validateRequest(any[Request])
        filterChain.getRequest shouldBe null
        servletResponse.getStatus shouldEqual statusCode
        servletResponse.getErrorMessage shouldBe message.getMessage
      }
    }

    OpenApiValidatorFilter.ValidationIssues.toSeq.take(4).combinations(2).flatMap(_.permutations).foreach { issuePairPermutation =>
      val (firstIssueKey, firstIssueStatusCode) = issuePairPermutation(0)
      val (secondIssueKey, secondIssueStatusCode) = issuePairPermutation(1)
      val issueKeys = OpenApiValidatorFilter.ValidationIssues.keys.toSeq

      it(s"should report the highest priority validation issue between $firstIssueKey and $secondIssueKey") {
        val firstMessage = ValidationReportScala.Message.create(firstIssueKey, "first issue")
        val secondMessage = ValidationReportScala.Message.create(secondIssueKey, "second issue")
        val (expectedStatus, expectedMessage) =
          if (issueKeys.indexOf(firstIssueKey) < issueKeys.indexOf(secondIssueKey)) {
            (firstIssueStatusCode, firstMessage.getMessage)
          } else {
            (secondIssueStatusCode, secondMessage.getMessage)
          }

        when(validator.validateRequest(any[Request]))
          .thenReturn(ValidationReportScala.from(
            firstMessage,
            secondMessage
          ))

        openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

        verify(validator).validateRequest(any[Request])
        filterChain.getRequest shouldBe null
        servletResponse.getStatus shouldEqual expectedStatus
        servletResponse.getErrorMessage shouldBe expectedMessage
      }
    }

    it("should respond with a 400 on a request with an unknown validation issue") {
      val message = ValidationReportScala.Message.create("unknown.key", "test message")
      when(validator.validateRequest(any[Request]))
        .thenReturn(ValidationReportScala.singleton(message))

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest shouldBe null
      servletResponse.getStatus shouldEqual HttpServletResponse.SC_BAD_REQUEST
      servletResponse.getErrorMessage shouldBe message.getMessage
    }

    it("should respond with a 500 if a servlet request cannot be converted to a validation request") {
      when(validator.validateRequest(any[Request]))
        .thenThrow(RequestConversionException("Conversion failed", null))

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest shouldBe null
      servletResponse.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("should respond with a 500 if the validation library throws an exception") {
      val exceptionMessage = "some test message"
      when(validator.validateRequest(any[Request]))
        .thenThrow(new RuntimeException(exceptionMessage))

      openApiValidatorFilter.doWork(servletRequest, servletResponse, filterChain)

      verify(validator).validateRequest(any[Request])
      filterChain.getRequest shouldBe null
      servletResponse.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      servletResponse.getErrorMessage shouldBe exceptionMessage
    }
  }

  describe("doConfigurationUpdated") {
    it("should fail to load a non-existent OpenAPI document") {
      val href = "file:/invalid/path.yaml"
      val config = new OpenApiValidatorConfig()
      config.setHref(href)

      an[ApiLoadException] should be thrownBy openApiValidatorFilter.doConfigurationUpdated(config)
    }

    it("should fail to load an invalid OpenAPI document") {
      val href = getClass.getResource("/openapi/invalid.yaml").toExternalForm
      val config = new OpenApiValidatorConfig()
      config.setHref(href)

      an[ApiLoadException] should be thrownBy openApiValidatorFilter.doConfigurationUpdated(config)
    }

    it("should load a document using a relative path") {
      val hrefPath = new File(getClass.getResource("/openapi/v3/petstore.yaml").getPath)
      val config = new OpenApiValidatorConfig()
      config.setHref(hrefPath.getName)

      openApiValidatorFilter = new OpenApiValidatorFilter(hrefPath.getParent, configurationService)

      noException should be thrownBy openApiValidatorFilter.doConfigurationUpdated(config)
    }

    Set("v2/petstore.json", "v2/petstore.yaml", "v3/petstore.yaml").foreach { document =>
      it(s"should successfully parse the $document OpenAPI document") {
        val href = getClass.getResource(s"/openapi/$document").toString
        val config = new OpenApiValidatorConfig()
        config.setHref(href)

        noException should be thrownBy openApiValidatorFilter.doConfigurationUpdated(config)
      }
    }
  }
}

object OpenApiValidatorFilterTest {

  def inputStreamToString(inputStream: InputStream, encoding: String): String = {
    Source.fromInputStream(inputStream, encoding)
      .getLines
      .mkString(System.lineSeparator)
  }

  // TODO: Remove this conversion once Scala supports Java lambda functions and `Answer` is a functional interface.
  implicit def getValidateRequestAnswer(f: Request => ValidationReport): Answer[ValidationReport] = {
    new Answer[ValidationReport] {
      override def answer(invocation: InvocationOnMock): ValidationReport = {
        val request = invocation.getArguments()(0).asInstanceOf[Request]
        f(request)
      }
    }
  }
}
