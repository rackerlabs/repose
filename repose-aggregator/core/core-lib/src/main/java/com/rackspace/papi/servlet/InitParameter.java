/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package com.rackspace.papi.servlet;

/**
 *
 * @author jhopper
 */
public enum InitParameter {
    POWER_API_CONFIG_DIR("powerapi-config-directory"),
    PORT("repose-bound-port"),
    CONNECTION_TIMEOUT("connection-timeout"),
    READ_TIMEOUT("read-timeout"),
    INSECURE("insecure"),
    MANAGEMENT_CONTEXT("com.rackspace.repose.management.context"),
    REPOSE_CLUSTER_ID("repose-cluster-id"),
    REPOSE_NODE_ID("repose-node-id");

    private final String initParameterName;

    private InitParameter(String webXmlName) {
        this.initParameterName = webXmlName;
    }

    public String getParameterName() {
        return initParameterName;
    }
}
