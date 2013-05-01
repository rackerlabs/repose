/*
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

package com.rackspace.external.pjlcompression;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A simple facade in front of logging services -- this class is used by other classes in this package to log
 * informational messages. It simply logs these message to the servlet log.
 *
 * @author Sean Owen
 */
final class CompressingFilterLoggerImpl implements CompressingFilterLogger {

	private static final String MESSAGE_PREFIX = " [CompressingFilter/" + CompressingFilter.VERSION + "] ";

	private final ServletContext servletContext;
	private final boolean debug;
	private final CompressingFilterLogger delegate;

	CompressingFilterLoggerImpl(ServletContext ctx,
	                            boolean debug,
	                            String delegateLoggerName,
	                            boolean isJavaUtilLogger) throws ServletException {
		assert ctx != null;
		servletContext = ctx;
		this.debug = debug;

		if (delegateLoggerName == null) {
			delegate = null;
		} else if (isJavaUtilLogger) {
			delegate = new JavaUtilLoggingImpl(delegateLoggerName);
		} else {
			try {
				// Load by reflection to avoid a hard dependence on Jakarta Commons Logging
				Class<?> delegateClass =
					Class.forName("com.planetj.servlet.filter.compression.JakartaCommonsLoggingImpl");
				Constructor<?> constructor =
					delegateClass.getConstructor(String.class);
				delegate = (CompressingFilterLogger) constructor.newInstance(delegateLoggerName);
			} catch (ClassNotFoundException cnfe) {
				throw new ServletException(cnfe);
			} catch (NoSuchMethodException nsme) {
				throw new ServletException(nsme);
			} catch (InvocationTargetException ite) {
				throw new ServletException(ite);
			} catch (IllegalAccessException iae) {
				throw new ServletException(iae);
			} catch (InstantiationException ie) {
				throw new ServletException(ie);
			}
		}
	}

	public boolean isDebug() {
		return debug;
	}

	public void log(String message) {
		servletContext.log(MESSAGE_PREFIX + message);
		if (delegate != null) {
			delegate.log(message);
		}
	}

	public void log(String message, Throwable t) {
		servletContext.log(MESSAGE_PREFIX + message, t);
		if (delegate != null) {
			delegate.log(message, t);
		}
	}

	public void logDebug(String message) {
		if (debug) {
      servletContext.log(MESSAGE_PREFIX + message);
			if (delegate != null) {
				delegate.logDebug(message);
			}
		}
	}

	public void logDebug(String message, Throwable t) {
		if (debug) {
      servletContext.log(MESSAGE_PREFIX + message, t);
			if (delegate != null) {
				delegate.logDebug(message, t);
			}
		}
	}

	@Override
	public String toString() {
		return "CompressingFilterLoggerImpl";
	}

}
