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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link CompressingFilterLogger} implementation based on Jakarta Commons Logging.
 *
 * @author Sean Owen
 */
public final class JakartaCommonsLoggingImpl implements CompressingFilterLogger {

	private final Log logger;

	/**
	 * This constructor is public so that it may be instantiated by reflection.
	 *
	 * @param loggerName Jakarta Log name
	 */
	public JakartaCommonsLoggingImpl(String loggerName) {
		logger = LogFactory.getLog(loggerName);
	}

	public void log(String message) {
		logger.info(message);
	}

	public void log(String message, Throwable t) {
		logger.info(message, t);
	}

	public void logDebug(String message) {
		logger.debug(message);
	}

	public void logDebug(String message, Throwable t) {
		logger.debug(message, t);
	}

  public boolean isDebug() {
    return logger.isDebugEnabled();
  }

	@Override
	public String toString() {
		return "JakartaCommonsLoggingImpl[" + logger + ']';
	}

}
