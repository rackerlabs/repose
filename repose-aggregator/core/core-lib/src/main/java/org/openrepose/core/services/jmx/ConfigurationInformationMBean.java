/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.core.services.jmx;

import java.util.List;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface ConfigurationInformationMBean {
    String OBJECT_NAME = "org.openrepose.core.services.jmx:type=ConfigurationInformation";

    //TODO: I don't think this is useful any more...
    Map<String, List<CompositeData>> getPerNodeFilterInformation() throws OpenDataException;

    boolean isNodeReady(String clusterId, String nodeId);

}
