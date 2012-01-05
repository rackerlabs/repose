package org.openrepose.rnxp.servlet.http.detached;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 *
 * @author zinic
 */
public abstract class AbstractHttpServletRequest implements HttpServletRequest {

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAuthType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContextPath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Cookie[] getCookies() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getDateHeader(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getHeader(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getIntHeader(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getMethod() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPathInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPathTranslated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getQueryString() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRequestURI() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public StringBuffer getRequestURL() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRequestedSessionId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getServletPath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HttpSession getSession(boolean create) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HttpSession getSession() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getAttribute(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getContentLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalAddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getParameter(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration<String> getParameterNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getParameterValues(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getProtocol() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRealPath(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRemoteAddr() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getRemoteHost() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getRemotePort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getScheme() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getServerName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getServerPort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isAsyncStarted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isAsyncSupported() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSecure() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(String name, Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
