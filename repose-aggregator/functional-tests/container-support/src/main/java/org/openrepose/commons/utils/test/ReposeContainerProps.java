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
package org.openrepose.commons.utils.test;

public class ReposeContainerProps {

    String startPort;
    String war;
    String configDir;
    String clusterId;
    String nodeId;
    String[] originServiceWars;

    public ReposeContainerProps(String startPort, String war, String configDir, String clusterId, String nodeId,
                                String... originServiceWars) {

        this.startPort = startPort;
        this.war = war;
        this.configDir = configDir;
        this.clusterId = clusterId;
        this.nodeId = nodeId;
        this.originServiceWars = originServiceWars;
    }

    public String getStartPort() {
        return startPort;
    }

    public String getWar() {
        return war;
    }

    public String getConfigDirectory() {
        return configDir;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String[] getOriginServiceWars() {
        return originServiceWars;
    }
}
