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
package features.core.logging

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Core
import spock.lang.IgnoreIf
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

import static java.nio.file.attribute.PosixFilePermission.*

@Category(Core)
class LogFileAccessTest extends ReposeValveTest {

    // todo: devise a way to ensure we have a valid username other than the name of the user running the JVM process
    static final String LOG_FILE_OWNER = System.getProperty('user.name')
    static final String LOG_FILE_PERMS = 'rw-rw-rw-'

    @Shared
    Path logFilePath

    def setupSpec() {
        logFilePath = Paths.get(properties.logFile)

        getLogFilePath()
        def params = properties.defaultTemplateParams
        params += [
            logFileOwner: LOG_FILE_OWNER,
            logFilePerms: LOG_FILE_PERMS
        ]

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/core/logging', params)

        // Starting Repose should ensure that the logFilePath is created
        repose.start()
    }

    def "log file owner should be set"() {
        expect:
        Files.getOwner(logFilePath).getName() == LOG_FILE_OWNER
    }

    @IgnoreIf({ System.getProperty("os.name").contains("windows") })
    def "log file permissions should be set"() {
        given:
        Set<PosixFilePermission> logFilePerms = Files.getPosixFilePermissions(logFilePath)

        expect:
        logFilePerms.containsAll(
            OWNER_READ,
            OWNER_WRITE,
            OTHERS_READ,
            OTHERS_WRITE,
            GROUP_READ,
            GROUP_WRITE
        )

        and:
        logFilePerms.disjoint([
            OWNER_EXECUTE,
            OTHERS_EXECUTE,
            GROUP_EXECUTE
        ])
    }
}
