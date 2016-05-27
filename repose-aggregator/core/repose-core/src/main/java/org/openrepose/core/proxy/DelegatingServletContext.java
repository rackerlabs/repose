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
package org.openrepose.core.proxy;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

/**
 * Just a ServletContext that delegates to another. Pretty much only used to hide all the delegating methods from a
 * big interface.
 */
public abstract class DelegatingServletContext implements ServletContext {
    protected final ServletContext context;

    public DelegatingServletContext(ServletContext context) {
        this.context = context;
    }

    @Override
    public String getContextPath() {
        return context.getContextPath();
    }

    @Override
    public ServletContext getContext(String uripath) {
        return context.getContext((uripath));
    }

    @Override
    public int getMajorVersion() {
        return context.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return context.getMinorVersion();
    }

    @Override
    public int getEffectiveMajorVersion() {
        return context.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return context.getEffectiveMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return context.getMimeType(file);
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return context.getResourcePaths(path);
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return context.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return context.getResourceAsStream(path);
    }

    @Override
    public String getVirtualServerName() {
        return context.getVirtualServerName();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return context.getRequestDispatcher(path);
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return context.getNamedDispatcher(name);
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return context.getServlet(name);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return context.getServlets();
    }

    @Override
    public Enumeration<String> getServletNames() {
        return context.getServletNames();
    }

    @Override
    public void log(String msg) {
        context.log(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        context.log(exception, msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        context.log(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        return context.getRealPath(path);
    }

    @Override
    public String getServerInfo() {
        return context.getServerInfo();
    }

    @Override
    public String getInitParameter(String name) {
        return context.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return context.getInitParameterNames();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return context.setInitParameter(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object object) {
        context.setAttribute(name, object);
    }

    @Override
    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }

    @Override
    public String getServletContextName() {
        return context.getServletContextName();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return context.addServlet(servletName, className);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return context.addServlet(servletName, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return context.addServlet(servletName, servletClass);
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return context.createServlet(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return context.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return context.getServletRegistrations();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return context.addFilter(filterName, className);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return context.addFilter(filterName, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return context.addFilter(filterName, filterClass);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return context.createFilter(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return context.getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return context.getFilterRegistrations();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return context.getSessionCookieConfig();
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        context.setSessionTrackingModes(sessionTrackingModes);
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return context.getDefaultSessionTrackingModes();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return context.getEffectiveSessionTrackingModes();
    }

    @Override
    public void addListener(String className) {
        context.addListener(className);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        context.addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        context.addListener(listenerClass);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return context.createListener(clazz);
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return context.getJspConfigDescriptor();
    }

    @Override
    public ClassLoader getClassLoader() {
        return context.getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
        context.declareRoles(roleNames);
    }

}
