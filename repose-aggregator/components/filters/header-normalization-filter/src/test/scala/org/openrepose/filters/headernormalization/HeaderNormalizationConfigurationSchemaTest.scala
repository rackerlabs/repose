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
package org.openrepose.filters.headernormalization

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigValidator
import org.openrepose.core.spring.{CoreSpringProvider, ReposeSpringProperties}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HeaderNormalizationConfigurationSchemaTest extends FunSpec with Matchers {
  val validator = ConfigValidator("/META-INF/schema/config/header-normalization-configuration.xsd")

  describe("schema validation") {
    it("should successfully validate the sample config") {
      validator.validateConfigFile("/META-INF/schema/examples/header-normalization.cfg.xml")
    }

    Seq("request-header-filters", "response-header-filters").foreach { filterType =>
      it(s"should successfully validate when the $filterType is used") {
        val config =
          s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
             |    <$filterType>
             |        <target>
             |        </target>
             |    </$filterType>
             |</header-normalization>""".stripMargin
        validator.validateConfigString(config)
      }

      Seq("whitelist", "blacklist").foreach { listType =>
        it(s"should successfully validate when a $filterType target defines a $listType") {
          val config =
            s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
               |    <$filterType>
               |        <target>
               |            <$listType>
               |                <header id="X-Some-Header"/>
               |            </$listType>
               |        </target>
               |    </$filterType>
               |</header-normalization>""".stripMargin
          validator.validateConfigString(config)
        }
      }

      it(s"should fail to validate when a target defines both a $filterType whitelist and blacklist") {
        val config =
          s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
             |    <$filterType>
             |        <target>
             |            <whitelist>
             |                <header id="X-Some-Header"/>
             |            </whitelist>
             |            <blacklist>
             |                <header id="X-Some-Header"/>
             |            </blacklist>
             |        </target>
             |    </$filterType>
             |</header-normalization>""".stripMargin
        val exception = intercept[SAXParseException] {
          validator.validateConfigString(config)
        }
        exception.getLocalizedMessage should include("Invalid content")
      }
    }

    it("should successfully validate when the new request-header-filters is used with the new response-header-filters") {
      val config =
        """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
          |    <request-header-filters>
          |        <target>
          |        </target>
          |    </request-header-filters>
          |    <response-header-filters>
          |        <target>
          |        </target>
          |    </response-header-filters>
          |</header-normalization>""".stripMargin
      validator.validateConfigString(config)
    }
  }

  val coreSpringProvider = CoreSpringProvider.getInstance()
  coreSpringProvider.initializeCoreContext("/etc/repose", false)
  val reposeVersion = coreSpringProvider.getCoreContext.getEnvironment.getProperty(
    ReposeSpringProperties.stripSpringValueStupidity(ReposeSpringProperties.CORE.REPOSE_VERSION))

  if (reposeVersion != "9") {
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

      it("should fail to validate when the deprecated header-filters is used with the new request-header-filters") {
        val config =
          """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
            |    <header-filters>
            |        <target>
            |        </target>
            |    </header-filters>
            |    <request-header-filters>
            |        <target>
            |        </target>
            |    </request-header-filters>
            |</header-normalization>""".stripMargin
        val exception = intercept[SAXParseException] {
          validator.validateConfigString(config)
        }
        exception.getLocalizedMessage should include("Cannot define the deprecated header-filters and request-header-filters.")
      }

      it("should successfully validate when the deprecated header-filters is used with the new response-header-filters") {
        val config =
          """<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
            |    <header-filters>
            |        <target>
            |        </target>
            |    </header-filters>
            |    <response-header-filters>
            |        <target>
            |        </target>
            |    </response-header-filters>
            |</header-normalization>""".stripMargin
        validator.validateConfigString(config)
      }


      Seq("header-filters", "request-header-filters", "response-header-filters").foreach { filterType =>
        Seq("whitelist", "blacklist").foreach { listType =>
          it(s"should successfully validate when a $filterType target defines two $listType") {
            val config =
              s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
                 |    <$filterType>
                 |        <target>
                 |            <$listType>
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |            <$listType>
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |        </target>
                 |    </$filterType>
                 |</header-normalization>""".stripMargin
            validator.validateConfigString(config)
          }

          it(s"should successfully validate when a $filterType target defines a $listType without an id") {
            val config =
              s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
                 |    <$filterType>
                 |        <target>
                 |            <$listType>
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |        </target>
                 |    </$filterType>
                 |</header-normalization>""".stripMargin
            validator.validateConfigString(config)
          }
        }
      }
    }
  } else {
    describe("new schema validation") {
      it("should fail to validate when the deprecated header-filters") {
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

      Seq("request-header-filters", "response-header-filters").foreach { filterType =>
        Seq("whitelist", "blacklist").foreach { listType =>
          it(s"should fail to validate when a $filterType target defines two $listType") {
            val config =
              s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
                 |    <$filterType>
                 |        <target>
                 |            <$listType>
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |            <$listType>
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |        </target>
                 |    </$filterType>
                 |</header-normalization>""".stripMargin
            val exception = intercept[SAXParseException] {
                validator.validateConfigString(config)
              }
            exception.getLocalizedMessage should include("Invalid content")
          }

          it(s"should fail to validate when a $filterType target defines $listType with an id") {
            val config =
              s"""<header-normalization xmlns='http://docs.openrepose.org/repose/header-normalization/v1.0'>
                 |    <$filterType>
                 |        <target>
                 |            <$listType id="No-Longer-Exists">
                 |                <header id="X-Some-Header"/>
                 |            </$listType>
                 |        </target>
                 |    </$filterType>
                 |</header-normalization>""".stripMargin
            val
            exception = intercept[SAXParseException] {
                validator.validateConfigString(config)
              }
            exception.getLocalizedMessage should include("not allowed")
          }
        }
      }
    }
  }
}