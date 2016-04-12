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
package features.core.manifest

import framework.ReposeValveTest
import org.apache.commons.io.FileUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder

import java.util.concurrent.TimeoutException
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

/**
 * Created by jennyvo on 4/12/16.
 */
class VersionAddedManifestTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
    }

    static def params

    def "Verify Version added to manifest"() {
        setup: "starts repose with installation configs"
        def params = properties.getDefaultTemplateParams()
        def nextPort = PortFinder.Singleton.getNextOpenPort()

        //note: Order matters here. The common directory overwrites some of the configs from the core directory.
        //      This means that the core configs we provide may not get tested, but due to the structure of our tests,
        //      this is currently "hard" to fix.
        repose.configurationProvider.applyConfigs("../../../../installation/configs/core", params)
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("../../../../installation/configs/extensions", params)
        repose.configurationProvider.applyConfigs("../../../../installation/configs/filters", params)
        String systemModelTemp = "${repose.configurationProvider.reposeConfigDir}/system-model.cfg.xml.${nextPort}"
        String systemModelSource = "${repose.configurationProvider.reposeConfigDir}/system-model.cfg.xml"
        new File(systemModelTemp).withWriter {
            out ->
                new File(systemModelSource).eachLine {
                    line ->
                        out << line.replaceAll("http-port=\"8080\"", "http-port=\"${nextPort}\"")
                }
        }
        FileUtils.copyFile(new File(systemModelTemp), new File(systemModelSource))

        repose.start()
        // get jar and war
        def reposeJar = repose.reposeJar
        def reposeWar = properties.reposeRootWar

        when: "start with installation configs"
        //todo: use a dynamic port (will require tinkering with [a copy of] the installation system-model).
        repose.waitForNon500FromUrl("http://localhost:${nextPort}")

        then:
        notThrown(TimeoutException)
        verifyVersionFromWar(reposeWar, properties.reposeVersion)
        verifyVersion(reposeJar, properties.reposeVersion)

        cleanup:
        repose.stop([throwExceptionOnKill: false])
    }

    private boolean verifyVersionFromWar(String reposeWar, String version) {
        def execcommand = "unzip -c ${reposeWar} META-INF/MANIFEST.MF"
        def stream = """${execcommand}""".execute()
        boolean verify = false
        Manifest reposemf = new Manifest(stream.inputStream);
        Attributes attributes = reposemf.getMainAttributes();
        String versionNumber = ""
        if (attributes != null) {
            Iterator it = attributes.keySet().iterator();
            while (it.hasNext()) {
                Attributes.Name key = (Attributes.Name) it.next();
                String keyword = key.toString();
                if (keyword.equals("Implementation-Version") || keyword.equals("Specification-Version")) {
                    versionNumber = (String) attributes.get(key);
                    break;
                }
            }
            if (version == versionNumber) {
                verify = true
            } else {
                println "version doesn't match..."
            }
        } else {
            println "Manifest attributes not found..."
        }
        stream.destroy()
        return verify
    }

    private boolean verifyVersion(String reposeJar, String version) {
        File file = new File(reposeJar)
        JarFile jarFile = new JarFile(file)
        Manifest reposemf = jarFile.getManifest()
        Attributes attributes = reposemf.getMainAttributes();
        boolean verify = false
        String versionNumber = ""
        if (attributes != null) {
            Iterator it = attributes.keySet().iterator();
            while (it.hasNext()) {
                Attributes.Name key = (Attributes.Name) it.next();
                String keyword = key.toString();
                if (keyword.equals("Implementation-Version") || keyword.equals("Specification-Version")) {
                    versionNumber = (String) attributes.get(key);
                    break;
                }
            }
            if (version == versionNumber) {
                verify = true
            } else {
                println "version doesn't match..."
            }
        } else {
            println "Manifest attributes not found..."
        }
        jarFile.close();
        return verify
    }

    // if need to check a list of attributes
    private boolean verifyManifest(JarFile jarFile, Map listAttr) {
        Manifest reposemf = jarFile.getManifest()
        Attributes attributes = reposemf.getMainAttributes();
        boolean check = false
        if (attributes != null) {
            listAttr.each { e ->
                def checkName = e.key
                Iterator it = attributes.keySet().iterator();
                while (it.hasNext()) {
                    Attributes.Name key = (Attributes.Name) it.next();
                    String keyword = key.toString();
                    if (keyword.equals(checkName)) {
                        if (checkName == (String) attributes.get(key)) {
                            check = true
                        } else {
                            check = false
                        }
                        break;
                    }
                }
            }
            return check;
        } else {
            println "Manifest attributes not found..."
            return false
        }
        jarFile.close();
    }
}
