package org.openrepose.filters.translation.httpx

import java.io.{ByteArrayInputStream, InputStream}

import org.junit.runner.RunWith
import org.openrepose.repose.httpx.v1.{RequestInformation, QueryParameters, Headers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpxMarshallerTest extends FunSpec with Matchers{
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
      pending
    }
    it("should turn Headers into an input stream") {
      pending
    }
    it("should turn QueryParamters into an input stream") {
      pending
    }
    it("should use a pool of marshallers") {
      pending
    }
  }
}
