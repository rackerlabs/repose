package features.servo

import framework.TestProperties
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions

class ServoJvmTest extends Specification {

    def testProps = new TestProperties()


    def "Servo will start up and run one node with in a 32MB heap space JVM"() {
        given:
        def fakeReposeContent = """
#!/bin/bash
while(true); do
  sleep 1
done
"""
        //Write out the fake repose to a temp dir
        def tempDir = Files.createTempDirectory("servoJvmTest")
        def fakeRepose = new File(tempDir.toFile(), "fakeRepose.sh")
        fakeRepose.deleteOnExit()
        Files.write(fakeRepose.toPath(), fakeReposeContent.getBytes(), StandardOpenOption.CREATE)
        Files.setPosixFilePermissions(fakeRepose.toPath(), PosixFilePermissions.fromString("r-xr-xr--"))

        //define an override config file to use our fake command rather than a real one
        def configOverrideFile = new File(tempDir.toFile(), "configOverride")
        configOverrideFile.deleteOnExit()
        def configOverride = """
baseCommand = [ ${fakeRepose.getAbsolutePath()} ]
"""
        Files.write(configOverrideFile.toPath(), configOverride.getBytes(), StandardOpenOption.CREATE)
        def configOverridePath = configOverrideFile.getAbsolutePath()

        //Set up a fake system model for this (one local node)
        def systemModelContent = this.getClass().getResource("/servo/system-model.cfg.xml").text
        def systemModelFile = new File(tempDir.toFile(), "system-model.cfg.xml")
        systemModelFile.deleteOnExit()
        Files.write(systemModelFile.toPath(), systemModelContent.getBytes(), StandardOpenOption.CREATE)

        //Set up a fake container.cfg.xml for this
        def containerContent = this.getClass().getResource("/servo/container.cfg.xml").text
        def containerFile = new File(tempDir.toFile(), "container.cfg.xml")
        containerFile.deleteOnExit()
        Files.write(containerFile.toPath(), containerContent.getBytes(), StandardOpenOption.CREATE)

        //Set up a fake log4j.properties for this
        def log4jContent = this.getClass().getResource("/servo/log4j.properties").text
        def log4jFile = new File(tempDir.toFile(), "log4j.properties")
        log4jFile.deleteOnExit()
        Files.write(log4jFile.toPath(), log4jContent.getBytes(), StandardOpenOption.CREATE)

        //Default nasty Log4j settings should be okay

        //Make sure it can start up and run for a few seconds, maybe 10.
        def servoPath = testProps.servoJar
        def command = "java -jar ${servoPath} --XX_CONFIGURATION_OVERRIDE_FILE_XX ${configOverridePath} --config-file ${tempDir.toFile().getAbsolutePath()}"

        //have defined JVM options for how low to tune Servo
        def servoJvmOptions = "-Xms16m -Xmx16m"
        def environment = ["JAVA_OPTS='${servoJvmOptions}'"]
        when:

        def out = new StringBuffer()
        def err = new StringBuffer()
        def proc = command.execute(environment, tempDir.toFile())
        proc.consumeProcessOutput(out, err)

        //Is three seconds long enough?
        Thread.sleep(3000)

        proc.destroy() //Kill it now
        proc.waitFor()

        println("command: ${command}")

        def exitValue = proc.exitValue()
        println("exit value is ${exitValue}")

        println("stdout: ${out}")
        println("\nstderr: ${err}")

        //TODO:Clean up temp dir recursively because servo creates files that aren't deleted on shutdown!


        then:
        proc.exitValue() == 143 //Because I terminated it

    }
}
