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
package org.openrepose.filters.headernormalization.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HeaderNormalizationConfigurationSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/header-normalization-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/header-normalization.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  val filterTypes = Seq("request", "response")
  val listTypes = Seq("whitelist", "blacklist")

  describe("schema validation") {
    filterTypes.foreach { filterType =>
      listTypes.foreach { listType =>
        it(s"should successfully validate when a $filterType target defines a $listType") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <target>
               |        <$filterType>
               |            <$listType>
               |                <header id="X-Some-Header"/>
               |            </$listType>
               |        </$filterType>
               |    </target>
               |</header-normalization>""".stripMargin
          validator.validateConfigString(config)
        }

        it(s"should fail to validate when a $filterType target defines two $listType's") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <target>
               |        <$filterType>
               |            <$listType>
               |                <header id="X-Some-Header"/>
               |            </$listType>
               |            <$listType>
               |                <header id="X-Other-Header"/>
               |            </$listType>
               |        </$filterType>
               |    </target>
               |</header-normalization>""".stripMargin
          val exception = intercept[SAXParseException] {
            validator.validateConfigString(config)
          }
          exception.getLocalizedMessage should include("Invalid content")
        }
      }

      it(s"should fail to validate when a target defines both a $filterType whitelist and blacklist") {
        val config =
          s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
             |    <target>
             |        <$filterType>
             |            <whitelist>
             |                <header id="X-Some-Header"/>
             |            </whitelist>
             |            <blacklist>
             |                <header id="X-Some-Header"/>
             |            </blacklist>
             |      </$filterType>
             |    </target>
             |</header-normalization>""".stripMargin
        val exception = intercept[SAXParseException] {
          validator.validateConfigString(config)
        }
        exception.getLocalizedMessage should include("Invalid content")
      }
    }
  }

  private val coreSpringProvider = CoreSpringProvider.getInstance()
  coreSpringProvider.initializeCoreContext("/etc/repose", false)
  private val reposeVersion = coreSpringProvider.getCoreContext.getEnvironment.getProperty(
    ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.REPOSE_VERSION))

  if (reposeVersion.startsWith("8.") || reposeVersion.startsWith("9.")) {
    describe("deprecated schema validation") {
      it("should successfully validate when the deprecated header-filters is used") {
        val config =
          """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
            |    <header-filters>
            |        <target>
            |        </target>
            |    </header-filters>
            |</header-normalization>""".stripMargin
        validator.validateConfigString(config)
      }

      it("should fail to validate when the deprecated header-filters is used with targets at the same level") {
        val config =
          """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
            |    <header-filters>
            |        <target>
            |        </target>
            |    </header-filters>
            |    <target>
            |    </target>
            |</header-normalization>""".stripMargin
        val exception = intercept[SAXParseException] {
          validator.validateConfigString(config)
        }
        exception.getLocalizedMessage should include("Cannot define the deprecated header-filters at this level along side a target")
      }

      listTypes.foreach { listType =>
        it(s"should successfully validate when a deprecated header-filters' target defines two $listType") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <header-filters>
               |        <target>
               |            <$listType>
               |                <header id="X-Some-Header"/>
               |            </$listType>
               |            <$listType>
               |                <header id="X-Other-Header"/>
               |            </$listType>
               |        </target>
               |    </header-filters>
               |</header-normalization>""".stripMargin
          validator.validateConfigString(config)
        }

        it(s"should successfully validate when a deprecated header-filters' target defines $listType with an id attribute") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <header-filters>
               |        <target>
               |            <$listType id="SomeList">
               |                <header id="X-Some-Header"/>
               |            </$listType>
               |        </target>
               |    </header-filters>
               |</header-normalization>""".stripMargin
          validator.validateConfigString(config)
        }

        filterTypes.foreach { filterType =>
          it(s"should fail to validate when the deprecated higher-level $listType with the new $filterType") {
            val config =
              s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
                 |    <header-filters>
                 |        <target>
                 |            <$listType id="SomeList">
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |            <$filterType>
                 |                <$listType>
                 |                    <header id="X-Other-Header"/>
                 |                </$listType>
                 |            </$filterType>
                 |        </target>
                 |    </header-filters>
                 |</header-normalization>""".stripMargin
            val exception = intercept[SAXParseException] {
              validator.validateConfigString(config)
            }
            exception.getLocalizedMessage should include(s"Cannot define the deprecated $listType at this level along side the new $filterType")
          }
        }
      }

      filterTypes.foreach { filterType =>
        it(s"should successfully validate when the $filterType is used") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <target>
               |        <$filterType>
               |        </$filterType>
               |    </target>
               |</header-normalization>""".stripMargin
          validator.validateConfigString(config)
        }
      }

      it(s"should fail to validate when a deprecated header-filters' target defines both a whitelist and blacklist") {
        val config =
          s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
             |    <header-filters>
             |        <target>
             |            <whitelist>
             |                <header id="X-Some-Header"/>
             |            </whitelist>
             |            <blacklist>
             |                <header id="X-Some-Header"/>
             |            </blacklist>
             |        </target>
             |    </header-filters>
             |</header-normalization>""".stripMargin
        val exception = intercept[SAXParseException] {
          validator.validateConfigString(config)
        }
        exception.getLocalizedMessage should include("Invalid content")
      }

      it("should successfully validate when the new request and response is used") {
        val config =
          """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
            |    <target>
            |        <request>
            |        </request>
            |        <response>
            |        </response>
            |    </target>
            |</header-normalization>""".stripMargin
        validator.validateConfigString(config)
      }
    }
  } else {
    describe("new schema validation") {
      it("should fail to validate when the deprecated header-filters is defined") {
        val config =
          """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
            |    <header-filters>
            |        <target>
            |        </target>
            |    </header-filters>
            |</header-normalization>""".stripMargin
        val exception = intercept[SAXParseException] {
          validator.validateConfigString(config)
        }
        exception.getLocalizedMessage should include("Invalid content")
      }

      filterTypes.foreach { filterType =>
        listTypes.foreach { listType =>
          it(s"should fail to validate when a $filterType target defines $listType with an id") {
            val config =
              s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
                 |    <target>
                 |        <$filterType>
                 |            <$listType id="No-Longer-Exists">
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |        </$filterType>
                 |    </target>
                 |</header-normalization>""".stripMargin
            val
            exception = intercept[SAXParseException] {
              validator.validateConfigString(config)
            }
            exception.getLocalizedMessage should include("not allowed")
          }
        }

        it(s"should fail to validate when a $filterType target doesn't define a whitelist or blacklist") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <target>
               |        <$filterType>
               |        </$filterType>
               |    </target>
               |</header-normalization>""".stripMargin
          val
          exception = intercept[SAXParseException] {
            validator.validateConfigString(config)
          }
          exception.getLocalizedMessage should include("is not complete")
        }
      }
    }
  }
}
