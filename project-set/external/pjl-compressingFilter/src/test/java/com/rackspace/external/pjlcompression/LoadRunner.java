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

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockServletContext;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.servlet.ServletTestModule;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Can be used to generate load on {@link CompressingFilter}.
 *
 * @author Sean Owen
 */
public final class LoadRunner {

	private LoadRunner() {
		// do nothing
	}

	public static void main(String... args) {

		WebMockObjectFactory factory = new WebMockObjectFactory();
		MockServletContext context = factory.getMockServletContext();
		context.setInitParameter("debug", "true");
		context.setInitParameter("statsEnabled", "true");
		ServletTestModule module = new ServletTestModule(factory);
		module.addFilter(new CompressingFilter(), true);
		module.setDoChain(true);

		Random r = new Random(0xDEADBEEFL);
		final String[] data = new String[200];
		for (int i = 0; i < data.length; i++) {
			byte[] bytes = new byte[50];
			r.nextBytes(bytes);
			data[i] = new String(bytes);
		}

		module.setServlet(new HttpServlet() {
			@Override			
			public void doGet(HttpServletRequest request,
			                  HttpServletResponse response) throws IOException {
				PrintWriter writer = response.getWriter();
				for (String string : data) {
					writer.print(string);
				}
			}
		});
		MockHttpServletRequest request = factory.getMockRequest();
		request.addHeader("Accept-Encoding", "gzip");

    long start = System.currentTimeMillis();
    int iterations = 1000;
		for (int i = 0; i < iterations; i++) {
			module.doGet();
		}
    long end = System.currentTimeMillis();
    long time = end - start;
    System.out.println("Completed in " + time + "ms (" + (double) time / iterations + " per request)");

	}


}
