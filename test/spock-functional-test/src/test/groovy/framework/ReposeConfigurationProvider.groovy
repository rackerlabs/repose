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

    ReposeConfigurationProvider(String reposeConfigDir, String samplesDir) {
        this.reposeConfigDir = new File(reposeConfigDir)
        this.samplesDir = new File(samplesDir)
        this.commonSamplesDir = new File(samplesDir + "/common")
    }

    /**
     * Prepare the configs using the configuration locations specified
     * @param configLocations directories where Repose configuration files live
     */
    void applyConfigs(String[] configLocations) {

        FileUtils.deleteDirectory(reposeConfigDir)

        // set the common config files, like system model and container
        FileUtils.copyDirectory(commonSamplesDir, reposeConfigDir)

        // Apply all configurations found in each of the config locations to the REPOSE
        // config directory
        configLocations.each { configs ->
            FileUtils.copyDirectory(new File(samplesDir.absolutePath + "/" + configs), reposeConfigDir)
        }
    }

    void updateConfigs(String[] configLocations) {
        configLocations.each { configs ->
            FileUtils.copyDirectory(new File(samplesDir.absolutePath + "/" + configs), reposeConfigDir)
        }
        println("updating configs")

        // TODO: add some conditional check here to only sleep until the configs have been reloaded
        // For now, we know that configs get checked every 15 seconds, so sleep a bit longer than this... suckaroos
        try {
            waitForCondition(clock, '25s', '1s', {
                false
            })
        } catch (TimeoutException e) {
            // do nothing... eventually make this conditional wait actually check some condition
        }

        println("updated configs")
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
     */
    void applyConfigsRuntime(String sourceFolder, params=[:]) {

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
    }

    public void cleanConfigDirectory() {
        if (reposeConfigDir.exists()) {
            FileUtils.cleanDirectory(reposeConfigDir)
        } else {
            reposeConfigDir.mkdirs()
        }

    }

}
