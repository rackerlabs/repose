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
package org.openrepose.commons.utils.jmx

import javax.management.ObjectName

import org.junit.runner.RunWith
import org.scalatest.{FunSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JmxObjectNameFactoryTest extends FunSpec with Matchers {

  private final val DefaultDomain = "defaultDomain"
  private final val DefaultName = "defaultName"

  describe("getName") {
    it("should set the ObjectName domain to the provided domain") {
      val objectName = JmxObjectNameFactory.getName(DefaultDomain, DefaultName)

      objectName.getDomain shouldEqual DefaultDomain
    }

    it("should set the ObjectName key properties to the name segments split and quoted") {
      val name = "one.two.three"
      val objectName = JmxObjectNameFactory.getName(DefaultDomain, name)

      objectName.getKeyProperty("001") shouldEqual ObjectName.quote("one")
      objectName.getKeyProperty("002") shouldEqual ObjectName.quote("two")
      objectName.getKeyProperty("003") shouldEqual ObjectName.quote("three")
    }

    it("should lexically sort the key property list") {
      val name = "one.two.three.four.five.six.seven.eight.nine.ten"
      val objectName = JmxObjectNameFactory.getName(DefaultDomain, name)

      val expectedKeyPropertyList = "001=" + ObjectName.quote("one") + "," +
        "002=" + ObjectName.quote("two") + "," +
        "003=" + ObjectName.quote("three") + "," +
        "004=" + ObjectName.quote("four") + "," +
        "005=" + ObjectName.quote("five") + "," +
        "006=" + ObjectName.quote("six") + "," +
        "007=" + ObjectName.quote("seven") + "," +
        "008=" + ObjectName.quote("eight") + "," +
        "009=" + ObjectName.quote("nine") + "," +
        "010=" + ObjectName.quote("ten")

      objectName.getKeyPropertyListString shouldEqual expectedKeyPropertyList
    }

    it("should quote a pattern domain") {
      val defaultDomainPattern = DefaultDomain + "?"
      val objectName = JmxObjectNameFactory.getName(defaultDomainPattern, DefaultName)

      objectName.getDomain shouldEqual ObjectName.quote(defaultDomainPattern)
    }
  }
}
