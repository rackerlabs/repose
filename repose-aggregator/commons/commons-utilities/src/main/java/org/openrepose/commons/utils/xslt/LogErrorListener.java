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
package org.openrepose.commons.utils.xslt;

import org.openrepose.commons.utils.logging.ExceptionLogger;
import org.slf4j.Logger;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public final class LogErrorListener implements ErrorListener {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LogErrorListener.class);
    private static final ExceptionLogger EXCEPTION_LOG = new ExceptionLogger(LOG);
    private static final String STOCK_ERROR_MSG = "Fatal error while processing XSLT, see previous LogErrorListener WARN for a hint. MSG : ";

    @Override
    public void warning(TransformerException te) {
        LOG.warn(te.getMessageAndLocation());
    }

    @Override
    public void error(TransformerException te) {
        throw EXCEPTION_LOG.newException(STOCK_ERROR_MSG + te.getMessageAndLocation(), te, RuntimeException.class);
    }

    @Override
    public void fatalError(TransformerException te) {
        throw EXCEPTION_LOG.newException(STOCK_ERROR_MSG + te.getMessageAndLocation(), te, RuntimeException.class);
    }
}
