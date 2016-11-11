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
 *
 * ============================================================================
 *
 * Copyright 2004 and onwards Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openrepose.external.pjlcompression;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link CompressingFilterLogger} implementation based on java.util.logging.
 *
 * @author Sean Owen
 */
public final class JavaUtilLoggingImpl implements CompressingFilterLogger {

    private final Logger logger;

    /**
     * This constructor is public so that it may be instantiated by reflection.
     *
     * @param loggerName {@link Logger} name
     */
    public JavaUtilLoggingImpl(String loggerName) {
        logger = Logger.getLogger(loggerName);
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void log(String message, Throwable t) {
        logger.log(Level.INFO, message, t);
    }

    @Override
    public void logDebug(String message) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(message);
        }
    }

    @Override
    public void logDebug(String message, Throwable t) {
        logger.log(Level.FINE, message, t);
    }

    @Override
    public boolean isDebug() {
        return logger.isLoggable(Level.FINE);
    }

    @Override
    public String toString() {
        return "JavaUtilLoggingImpl[" + logger + ']';
    }

}
