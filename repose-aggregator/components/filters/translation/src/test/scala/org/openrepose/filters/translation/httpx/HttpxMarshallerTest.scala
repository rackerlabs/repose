package org.openrepose.filters.translation.httpx

import java.io.{ByteArrayInputStream, InputStream}

import org.junit.runner.RunWith
import org.openrepose.docs.repose.httpx.v1._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpxMarshallerTest extends FunSpec with Matchers {
  describe("The HttpxMarshaller when unmarshalling") {
    it("should turn an input stream into RequestInformation") {
      val httpxMarshaller = new HttpxMarshaller
      val xml: InputStream = getClass.getResourceAsStream("/request-information.xml")
      val requestInformation: RequestInformation = httpxMarshaller.unmarshallRequestInformation(xml)
      requestInformation shouldNot be(null)
      requestInformation.getUri shouldBe "/foo/bar/baz"
      requestInformation.getUrl shouldBe "http://foo"
      requestInformation.getInformational.getServerName shouldBe "serverName"
    }
    it("should turn an input stream into Headers") {
      val httpxMarshaller = new HttpxMarshaller
      val xml: InputStream = getClass.getResourceAsStream("/headers.xml")
      val headers: Headers = httpxMarshaller.unmarshallHeaders(xml)
      headers shouldNot be(null)
      headers.getRequest.getHeader.size shouldBe 5
      headers.getResponse.getHeader.size shouldBe 2
    }
    it("should turn an input stream into QueryParameters") {
      val httpxMarshaller = new HttpxMarshaller
      val xml: InputStream = getClass.getResourceAsStream("/query-parameters.xml")
      val queryParameters: QueryParameters = httpxMarshaller.unmarshallQueryParameters(xml)
      queryParameters shouldNot be(null)
      queryParameters.getParameter.size shouldBe 4
    }
    it("should throw an exception when it cant unmarshall the stream") {
      val httpxMarshaller = new HttpxMarshaller
      val ex = intercept[HttpxException] {
        httpxMarshaller.unmarshall(new ByteArrayInputStream(new Array[Byte](0)))
      }
      ex.getMessage shouldBe "Error unmarshalling xml input"
    }
  }
  describe("The HttpxMarshaller when marshalling") {
    it("should turn RequestInformation into an input stream") {
      val httpxMarshaller = new HttpxMarshaller
      val requestInfo = new RequestInformation
      requestInfo.setUri("foo")
      requestInfo.setUrl("bar")
      stringify(httpxMarshaller.marshall(requestInfo)) shouldBe
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<request-information xmlns=\"http://docs.openrepose.org/repose/httpx/v1.0\">" +
          "<uri>foo</uri>" +
          "<url>bar</url>" +
          "</request-information>"
    }
    it("should turn Headers into an input stream") {
      val httpxMarshaller = new HttpxMarshaller
      val headers = new Headers
      val headerList = new HeaderList
      val pair = new QualityNameValuePair()
      pair.setName("header1")
      pair.setValue("value1")
      pair.setQuality(.25)
      headerList.getHeader.add(pair)
      headers.setRequest(headerList)
      stringify(httpxMarshaller.marshall(headers)) shouldBe
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<headers xmlns=\"http://docs.openrepose.org/repose/httpx/v1.0\">" +
            "<request><header quality=\"0.25\" name=\"header1\" value=\"value1\"/></request>" +
          "</headers>"
    }
    it("should turn QueryParameters into an input stream") {
      val httpxMarshaller = new HttpxMarshaller
      val queryParameters = new QueryParameters
      val pair = new NameValuePair
      pair.setName("name1")
      pair.setValue("paramValue")
      queryParameters.getParameter.add(pair)
      stringify(httpxMarshaller.marshall(queryParameters)) shouldBe
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
          "<parameters xmlns=\"http://docs.openrepose.org/repose/httpx/v1.0\">" +
            "<parameter name=\"name1\" value=\"paramValue\"/>" +
          "</parameters>"
    }
    it("should throw an exception when it cant marshall the object") {
      val httpxMarshaller = new HttpxMarshaller
      val ex = intercept[Exception] {
        httpxMarshaller.marshall(new Object)
      }
      ex.getMessage shouldBe "Error marshalling HTTPX object"
    }
  }
  val stringify: InputStream => String = { marshalled =>
    val bytes = new Array[Byte](marshalled.available)
    marshalled.read(bytes, 0, marshalled.available)
    new String(bytes)
  }
}
