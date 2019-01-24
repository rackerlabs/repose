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
package features.filters.headertranslation

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class HeaderTranslationSchemaAssertionTest extends ReposeValveTest {
    def static params = [:]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        params = properties.defaultTemplateParams
    }

    def cleanup() {
        repose?.stop()
    }

    @Unroll
    def "starting with a bad config should fail (#config)."() {
        given: "a bad config"
        reposeLogSearch.cleanLog()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/badconfig/$config", params)

        when: "repose is started"
        repose.start()

        then: "filter fails to initialize"
        reposeLogSearch.searchByString(errMsg).size() == 1

        where:
        config    | errMsg
        "quality" | "Value '-1.0' is not facet-valid with respect to minInclusive '0.0E1' for type 'doubleBetweenZeroAndOne'"
        "unique"  | "Assertion failed for schema type 'header-translationType'. Original names must be unique. Evaluation is case insensitive."
    }
}
