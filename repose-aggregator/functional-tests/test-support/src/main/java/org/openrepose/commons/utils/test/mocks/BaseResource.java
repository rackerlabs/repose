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
package org.openrepose.commons.utils.test.mocks;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * This resource should be the parent class of every mock resource defined in
 * this library. Extending this class by extending a child of this class is in
 * line with this requirement (you may have nested inheritance models).
 */
public class BaseResource {

    private final DataProvider provider;

    public BaseResource() throws DatatypeConfigurationException {
       this(new DataProviderImpl());
    }
    
    public BaseResource(DataProvider provider) throws DatatypeConfigurationException {
        this.provider = provider;
    }
    
    protected <P extends DataProvider> P getProvider() {
       return (P) provider;
    }
}
