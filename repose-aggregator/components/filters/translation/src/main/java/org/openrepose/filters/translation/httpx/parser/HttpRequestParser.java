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
package org.openrepose.filters.translation.httpx.parser;

import org.openrepose.core.httpx.MessageDetail;
import org.openrepose.core.httpx.MessageEnvelope;
import org.openrepose.core.httpx.RequestHeadDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;
import org.openrepose.filters.translation.httpx.marshaller.MarshallerFactory;
import org.openrepose.filters.translation.httpx.node.ComplexNode;
import org.openrepose.filters.translation.httpx.node.RequestNode;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.List;

/**
 * @author fran
 */
public class HttpRequestParser extends ObjectFactoryUser implements Parser<HttpServletRequest, RequestHeadDetail> {

    @Override
    public InputStream parse(HttpServletRequest request, List<MessageDetail> requestFidelity, List<RequestHeadDetail> headFidelity, List<String> headersFidelity, boolean jsonProcessing) {
        MessageEnvelope messageEnvelope = getObjectFactory().createMessageEnvelope();

        ComplexNode requestNode = new RequestNode(request, messageEnvelope, requestFidelity, headFidelity, headersFidelity, jsonProcessing);

        requestNode.build();

        return MarshallerFactory.newInstance().marshall(messageEnvelope);
    }
}