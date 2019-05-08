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
package org.openrepose.core.services.httplogging;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * A container for all of the state associated with an HTTP interaction that
 * may be used by the {@link HttpLoggingService} to render messages.
 * <p>
 * This class is intentionally mutable to keep interactions simple.
 * Additionally, standard setters are used rather than "fluent" setters to
 * indicate mutability and keep the behavior of methods clear.
 * <p>
 * This class should only be constructed by the {@link HttpLoggingService}.
 * By only allowing the {@link HttpLoggingService} to construct this class,
 * stronger assertions can be made about the tracking and integrity of any
 * instances. For that reason, constructor access is limited to the package.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HttpLoggingContext {
    private HttpServletRequest inboundRequest;
    private HttpServletRequest outboundRequest;
    private HttpServletResponse outboundResponse;

    private final Map<String, Object> extensions = new HashMap<>();
}
