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
package org.openrepose.core.services.deploy

import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import org.openrepose.core.services.event.EventService

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ArtifactDirectoryWatcherTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    File artifactDir

    EventService mockEventService = Mockito.mock(EventService.class)
    ArtifactDirectoryWatcher artifactDirectoryWatcher = new ArtifactDirectoryWatcher(mockEventService)

    @Before
    void setup() {
        artifactDir = tempFolder.newFolder()
        FileUtils.cleanDirectory(artifactDir)
        artifactDirectoryWatcher.updateArtifactDirectoryLocation(artifactDir)
    }

    @Test
    void "checkArtifacts should return true if an artifact is changed"() {
        File newEar = new File(artifactDir, "empty.ear")
        FileUtils.writeStringToFile(newEar, "fake data")
        assertTrue(artifactDirectoryWatcher.checkArtifacts())
    }

    @Test
    void "checkArtifacts should return false if no artifact is changed"() {
        assertFalse(artifactDirectoryWatcher.checkArtifacts())
    }
}
