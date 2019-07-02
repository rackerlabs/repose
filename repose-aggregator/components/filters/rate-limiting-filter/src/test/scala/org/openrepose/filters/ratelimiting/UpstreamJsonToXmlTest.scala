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
package org.openrepose.filters.ratelimiting

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

import scala.xml.{Elem, XML}

@RunWith(classOf[JUnitRunner])
class UpstreamJsonToXmlTest extends FunSpec with Matchers {
  val upstreamJson =
    """{
      |    "limits": {
      |        "rate": [],
      |        "absolute": {
      |            "maxServerMeta": 40,
      |            "maxPersonality": 5,
      |            "totalPrivateNetworksUsed": 0,
      |            "maxImageMeta": 40,
      |            "maxPersonalitySize": 1000,
      |            "maxSecurityGroupRules": -1,
      |            "maxTotalKeypairs": 100,
      |            "totalCoresUsed": 0,
      |            "totalRAMUsed": 0,
      |            "totalInstancesUsed": 0,
      |            "maxSecurityGroups": -1,
      |            "totalFloatingIpsUsed": 0,
      |            "maxTotalCores": -1,
      |            "totalSecurityGroupsUsed": 0,
      |            "maxTotalPrivateNetworks": 3,
      |            "maxTotalFloatingIps": -1,
      |            "maxTotalInstances": 100,
      |            "maxTotalRAMSize": 131072
      |        }
      |    }
      |}
    """.stripMargin

  it("Converts json upstream to the proper XML format") {
    val is = new ByteArrayInputStream(upstreamJson.getBytes(StandardCharsets.UTF_8))
    val converted = UpstreamJsonToXml.convert(is)
    val parsedXml: Elem = XML.loadString(converted)

    for {
      limits <- parsedXml \ "absolute" \ "limit"
      namedLimit <- limits if (limits \ "@name").text == "maxServerMeta"
      value <- namedLimit \ "@value"
    } yield {
      value.text.toInt shouldEqual 40
    }
  }
}
