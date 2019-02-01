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
package features.core.deployment

import com.google.common.hash.Hashing
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Core

import static com.google.common.io.Files.hash

@Category(Core)
class ArtifactDeploymentTest extends ReposeValveTest {

    static final String ARTIFACT_DIR_NAME = 'artifacts'
    static final String DEPLOYMENT_DIR_NAME = 'deployments'
    static final FilenameFilter EAR_FILENAME_FILTER = new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith('.ear')
        }
    }

    static File artifactDir
    static File deploymentDir

    def setupSpec() {
        def params = properties.defaultTemplateParams
        params += [
            deploymentDirName: DEPLOYMENT_DIR_NAME
        ]

        artifactDir = new File(properties.reposeHome, ARTIFACT_DIR_NAME)
        deploymentDir = new File(properties.reposeHome, DEPLOYMENT_DIR_NAME)

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/core/deployment', params)
    }

    def setup() {
        repose.start()
    }

    def cleanup() {
        // Stop is idempotent, so it is safe to always call
        repose.stop()
    }

    def 'artifacts should be deployed to directories named the hash of the artifact itself'() {
        given:
        List<String> artifactHashes = artifactDir.listFiles(EAR_FILENAME_FILTER).collect {
            hash(it, Hashing.murmur3_128()).toString()
        }

        expect:
        deploymentDir.list() as Set == artifactHashes as Set
    }

    def 'auto-clean should delete only the files and directories created by Repose'() {
        given:
        File testFile = new File(deploymentDir, "testFile")
        File testDir = new File(deploymentDir, "testDir")
        testFile.createNewFile()
        testDir.mkdir()

        when:
        repose.stop()

        then:
        deploymentDir.listFiles() as Set == [testFile, testDir] as Set

        cleanup:
        testFile.delete()
        testDir.delete()
    }
}
