/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.logging;

import com.rackspace.papi.commons.util.reflection.ReflectionTools;
import org.slf4j.Logger;

/**
 * The ExceptionLogger is a simple wrapper for the java.util.logging.Logger designed to
 * add a number of 'nice' features and syntax enhancements to the logging metaphor.
 * 
 * 
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class ExceptionLogger {
    private final Logger loggerRef;

    public ExceptionLogger(Logger loggerRef) {
        this.loggerRef = loggerRef;
    }

    public <T extends Exception> T newException(String message, Class<T> exceptionClass) {
        return newException(message, null, exceptionClass);
    }

    public <T extends Exception> T newException(String message, Throwable cause, Class<T> exceptionClass) {
        Throwable newExceptionInstance;

        if (cause == null) {
            newExceptionInstance = ReflectionTools.construct(exceptionClass, message);
        } else {
            newExceptionInstance = ReflectionTools.construct(exceptionClass, message, cause);
        }

        loggerRef.error(message, cause) ;

        return (T) newExceptionInstance;
    }
}
