/*
 * Copyright 2006 and onwards Sean Owen
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

import junit.framework.TestCase;

import java.lang.reflect.Constructor;

/**
 * Tests {@link com.planetj.servlet.filter.compression.JakartaCommonsLoggingImpl} and
 * {@link com.planetj.servlet.filter.compression.JavaUtilLoggingImpl}.
 *
 * @author Sean Owen
 * @since 1.6.2
 */
public final class LogTest extends TestCase {

	public void testJakartaImpl() throws Exception {
		Class<?> delegateClass =
			Class.forName("com.rackspace.external.pjlcompression.JakartaCommonsLoggingImpl");
		Constructor<?> constructor = delegateClass.getConstructor(String.class);
		// Verify this works
		constructor.newInstance("foo");
	}

	public void testJavaUtiImpl() throws Exception {
		Class<?> delegateClass = Class.forName("com.rackspace.external.pjlcompression.JavaUtilLoggingImpl");
		Constructor<?> constructor = delegateClass.getConstructor(String.class);
		// Verify this works
		constructor.newInstance("foo");
	}

}
