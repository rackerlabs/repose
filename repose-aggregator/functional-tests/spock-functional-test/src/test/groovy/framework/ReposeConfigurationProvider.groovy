package framework

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.text.StrSubstitutor
import org.linkedin.util.clock.SystemClock

import java.util.concurrent.TimeoutException

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

/**
 * Responsible for applying and updating configuration files for an instance of Repose
 */
class ReposeConfigurationProvider {

    def File reposeConfigDir
    def File samplesDir
    def File commonSamplesDir
    def clock = new SystemClock()

    ReposeConfigurationProvider(TestProperties properties) {
        this(properties.configDirectory, properties.configSamples)
    }
    ReposeConfigurationProvider(String reposeConfigDir, String samplesDir) {
        this.reposeConfigDir = new File(reposeConfigDir)
        this.samplesDir = new File(samplesDir)
        this.commonSamplesDir = new File(samplesDir + "/common")
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
    void applyConfigs(String sourceFolder, Map params=[:], sleepTimeInSeconds=null) {

        def source = new File(samplesDir.absolutePath + "/" + sourceFolder)

        if (!source.exists()) { throw new IllegalArgumentException("\"${source.toString()}\" not found")}
        if (!source.isDirectory()) { throw new IllegalArgumentException("\"${source.toString()}\" is not a directory") }

        for (file in FileUtils.listFiles(source, null, true)) {

            String contents = FileUtils.readFileToString(file)
            def processedContents = StrSubstitutor.replace(contents, params, "\${", "}")

            // Note: this is necessary to get relative paths under JDK 6.
            // If using JDK 7, use java.nio.file.Path.relativize instead.
            def relativePath = source.toURI().relativize(file.toURI()).path
            def destinationFilename = FilenameUtils.concat(reposeConfigDir.absolutePath, relativePath)
            FileUtils.writeStringToFile(new File(destinationFilename), processedContents)
        }

        if (sleepTimeInSeconds &&
                sleepTimeInSeconds instanceof Number &&
                sleepTimeInSeconds > 0) {

            sleep(sleepTimeInSeconds * 1000)
        }
    }

    public void cleanConfigDirectory() {
        if (reposeConfigDir.exists()) {
            FileUtils.cleanDirectory(reposeConfigDir)
        } else {
            reposeConfigDir.mkdirs()
        }

    }

}
