package com.rackspace.papi.filter;

import com.rackspace.papi.http.proxy.HttpRequestDispatcher;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author Dan Daley
 */
public class ServletContextWrapper implements ServletContext {

    private static final Logger LOG = LoggerFactory.getLogger(ServletContextWrapper.class);
    private final ServletContext context;
    private final String targetContext;
    private final String target;
    private ApplicationContext applicationContext = null;
    private RequestProxyService proxyService = null;

    public static ServletContext getContext(ServletContext context) {
        return new ServletContextWrapper(context);
    }

    public ServletContextWrapper(ServletContext context) {
        targetContext = "";
        this.context = context;
        this.target = null;
    }

    public String getTargetContext() {
        return targetContext;
    }

    public ServletContext getParentContext() {
        return context;
    }

    public String getTarget() {
        return target;
    }

    public ServletContextWrapper(ServletContext context, String contextName) {
        this.targetContext = contextName;
        this.context = context;

        URI uri = null;
        String targetHostPort = null;
        try {
            uri = new URI(targetContext);
            targetHostPort = uri.getHost() + ":" + uri.getPort();
        } catch (URISyntaxException ex) {
            LOG.error("Invalid target context: " + targetContext, ex);
        }

        this.target = targetHostPort;
    }

    @Override
    public String getContextPath() {
        return context.getContextPath();
    }

    private String cleanPath(String uri) {
        return uri == null ? "" : uri.split("\\?")[0];
    }

    @Override
    public ServletContext getContext(String uripath) {
        final String uri = cleanPath(uripath);
        if (uri.matches("^https?://.*")) {
            return new ServletContextWrapper(this, uri);
        } else {
            ServletContext newContext = context.getContext(uri);
            if (newContext == null) {
                return null;
            }

            return new ServletContextWrapper(newContext, uri);
        }

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

    private RequestDispatcher getDispatcher() {
        RequestDispatcher dispatcher = null;

        if (target != null) {
            return new HttpRequestDispatcher(getProxyService(), targetContext);
        }

        return dispatcher;
    }

    private synchronized ApplicationContext getApplicationContext() {
        if (applicationContext != null) {
            return applicationContext;
        }

        applicationContext = (ApplicationContext) getAttribute("PAPI_SpringApplicationContext");

        return applicationContext;
    }

    private synchronized RequestProxyService getProxyService() {
        if (proxyService != null) {
            return proxyService;
        }

        ApplicationContext appContext = getApplicationContext();
        proxyService = appContext != null ? appContext.getBean("requestProxyService", RequestProxyService.class) : null;

        return proxyService;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        RequestDispatcher dispatcher;

        if (targetContext.matches("^https?://.*")) {

            dispatcher = getDispatcher();
            if (dispatcher == null) {
                dispatcher = new HttpRequestDispatcher(getProxyService(), targetContext);
            }
        } else {
            String dispatchPath = path.startsWith("/") ? path : "/" + path;
            dispatcher = context.getRequestDispatcher(dispatchPath);
        }

        return dispatcher;
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
    public Dynamic addServlet(String servletName, String className) {
        return context.addServlet(servletName, className);
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        return context.addServlet(servletName, servlet);
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
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
