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
package framework

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.linkedin.util.clock.SystemClock

/**
 * Responsible for applying and updating configuration files for an instance of Repose
 */
class ReposeConfigurationProvider {

    private static final List<String> FILE_EXTENSIONS_SKIP_TEMPLATING = [".jks"]

    File reposeConfigDir
    File configTemplatesDir
    File commonTemplatesDir
    def clock = new SystemClock()

    ReposeConfigurationProvider(TestProperties properties) {
        this(properties.configDirectory, properties.configTemplates)
    }

    ReposeConfigurationProvider(String reposeConfigDir, String configTemplatesDir) {
        this.reposeConfigDir = new File(reposeConfigDir)
        this.configTemplatesDir = new File(configTemplatesDir)
        this.commonTemplatesDir = new File(configTemplatesDir + "/common")
    }

    /**
     * Copies files from the designated source folder to Repose's config
     *   folder, and substitutes templates parameters as specified. This
     *   method acts recursively, copying and retaining the whole folder
     *   hierarchy under sourceFolder. Template parameters are substituted
     *   at runtime.
     * @param sourceFolder The folder containing the config files to apply
     * @param params A map for names to values to be substituted.
     *   eg: "Hello ${name}!" with params=["name":"world"] becomes
     *   "Hello world!"
     * @param sleepTimeInSeconds Sleep for this many seconds after the
     *   config files have been applied. Useful for waiting for Repose
     *   to pick up the new changes.
     */
    void applyConfigs(String sourceFolder, Map params = [:], sleepTimeInSeconds = null) {

        def source = new File(configTemplatesDir.absolutePath + "/" + sourceFolder)

        if (!source.exists()) {
            throw new IllegalArgumentException("\"${source.toString()}\" not found")
        }
        if (!source.isDirectory()) {
            throw new IllegalArgumentException("\"${source.toString()}\" is not a directory")
        }

        for (file in FileUtils.listFiles(source, null, true)) {
            // Note: this is necessary to get relative paths under JDK 6.
            // If using JDK 7, use java.nio.file.Path.relativize instead.
            def relativePath = source.toURI().relativize(file.toURI()).path
            def destinationFilename = FilenameUtils.concat(reposeConfigDir.absolutePath, relativePath)
            def destinationFile = new File(destinationFilename)

            if (FILE_EXTENSIONS_SKIP_TEMPLATING.any { file.name.toLowerCase().endsWith(it) }) {
                // no template substitution
                FileUtils.copyFile(file, destinationFile)
            } else {
                // substitute template parameters in the file contents
                String contents = FileUtils.readFileToString(file)
                def processedContents = StrSubstitutor.replace(contents, params, "\${", "}")
                FileUtils.writeStringToFile(destinationFile, processedContents)
            }
        }

        if (sleepTimeInSeconds &&
                sleepTimeInSeconds instanceof Number &&
                sleepTimeInSeconds > 0) {

            sleep(sleepTimeInSeconds * 1000)
        }
    }

    void cleanConfigDirectory() {
        if (reposeConfigDir.exists()) {
            FileUtils.cleanDirectory(reposeConfigDir)
        } else {
            reposeConfigDir.mkdirs()
        }
    }

    File getSystemModel() {
        new File(reposeConfigDir, "system-model.cfg.xml")
    }

}
