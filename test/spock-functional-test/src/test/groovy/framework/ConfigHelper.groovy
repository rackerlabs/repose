package framework

import org.apache.commons.io.FileUtils

class ConfigHelper {

    def File reposeConfigDirectory
    def File configSamplesDirectory

    ConfigHelper(String reposeConfigDirectory, String configSamplesDirectory) {
        this.reposeConfigDirectory = new File(reposeConfigDirectory)
        this.configSamplesDirectory = new File(configSamplesDirectory)
    }

    /**
     * Prepare the configs using the configuration locations specified
     * @param configLocations directories where Repose configuration files live
     */
    void prepConfiguration(String[] configLocations) {

        FileUtils.deleteDirectory(reposeConfigDirectory)

        // Add all default common configs
        configSamplesDirectory.eachFile {
            if (it.isFile()) {
                FileUtils.copyFileToDirectory(it, reposeConfigDirectory)
            }
        }

        configLocations.each { configs ->
            FileUtils.copyDirectory(new File(configSamplesDirectory.absolutePath + "/" + configs), reposeConfigDirectory)
        }

    }
}
