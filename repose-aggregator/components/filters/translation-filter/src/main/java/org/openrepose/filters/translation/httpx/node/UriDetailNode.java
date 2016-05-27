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
package org.openrepose.filters.translation.httpx.node;

import org.openrepose.core.httpx.ComplexParameter;
import org.openrepose.core.httpx.RequestHead;
import org.openrepose.core.httpx.URIDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import java.util.Map;

/**
 * @author fran
 */
public class UriDetailNode extends ObjectFactoryUser implements Node {
    private final Map<String, String[]> parameterMap;
    private final RequestHead requestHead;

    public UriDetailNode(Map<String, String[]> parameterMap, RequestHead requestHead) {
        this.parameterMap = parameterMap;
        this.requestHead = requestHead;
    }

    @Override
    public void build() {
        URIDetail uriDetail = getObjectFactory().createURIDetail();
        uriDetail.setFragment("where do we get this?");

        if (parameterMap != null) {

            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                ComplexParameter parameter = getObjectFactory().createComplexParameter();
                parameter.setName(entry.getKey());

                for (String value : entry.getValue()) {
                    parameter.getValue().add(value);
                }

                uriDetail.getQueryParameter().add(parameter);
            }
        }

        requestHead.setUriDetail(uriDetail);
    }
}
