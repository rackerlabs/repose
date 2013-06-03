package framework

import org.apache.commons.io.FileUtils

/**
 * Responsible for applying and updating configuration files for an instance of Repose
 */
class ReposeConfigurationProvider {

    def File reposeConfigDir
    def File samplesDir
    def File commonSamplesDir

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

        // TODO: add some conditional check here to only sleep until the configs have been reloaded
        // For now, we know that configs get checked every 15 seconds, so sleep a bit longer than this... suckaroos
        sleep(25000)
    }
}
