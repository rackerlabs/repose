package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author zinic
 */
public class SwitchableHttpServletResponse implements HttpServletResponse {

    private HttpServletResponse responseDelegate;

    public synchronized void setResponseDelegate(HttpServletResponse responseDelegate) {
        this.responseDelegate = responseDelegate;
    }

    public synchronized HttpServletResponse getResponseDelegate() {
        return responseDelegate;
    }

    @Override
    public void setLocale(Locale loc) {
        getResponseDelegate().setLocale(loc);
    }

    @Override
    public void setContentType(String type) {
        getResponseDelegate().setContentType(type);
    }

    @Override
    public void setContentLength(int len) {
        getResponseDelegate().setContentLength(len);
    }

    @Override
    public void setCharacterEncoding(String charset) {
        getResponseDelegate().setCharacterEncoding(charset);
    }

    @Override
    public void setBufferSize(int size) {
        getResponseDelegate().setBufferSize(size);
    }

    @Override
    public void resetBuffer() {
        getResponseDelegate().resetBuffer();
    }

    @Override
    public void reset() {
        getResponseDelegate().reset();
    }

    @Override
    public boolean isCommitted() {
        return getResponseDelegate().isCommitted();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return getResponseDelegate().getWriter();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return getResponseDelegate().getOutputStream();
    }

    @Override
    public Locale getLocale() {
        return getResponseDelegate().getLocale();
    }

    @Override
    public String getContentType() {
        return getResponseDelegate().getContentType();
    }

    @Override
    public String getCharacterEncoding() {
        return getResponseDelegate().getCharacterEncoding();
    }

    @Override
    public int getBufferSize() {
        return getResponseDelegate().getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        getResponseDelegate().flushBuffer();
    }

    @Override
    public void setStatus(int sc, String sm) {
        getResponseDelegate().setStatus(sc, sm);
    }

    @Override
    public void setStatus(int sc) {
        getResponseDelegate().setStatus(sc);
    }

    @Override
    public void setIntHeader(String name, int value) {
        getResponseDelegate().setIntHeader(name, value);
    }

    @Override
    public void setHeader(String name, String value) {
        getResponseDelegate().setHeader(name, value);
    }

    @Override
    public void setDateHeader(String name, long date) {
        getResponseDelegate().setDateHeader(name, date);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        getResponseDelegate().sendRedirect(location);
    }

    @Override
    public void sendError(int sc) throws IOException {
        getResponseDelegate().sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        getResponseDelegate().sendError(sc, msg);
    }

    @Override
    public int getStatus() {
        return getResponseDelegate().getStatus();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return getResponseDelegate().getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return getResponseDelegate().getHeaderNames();
    }

    @Override
    public String getHeader(String name) {
        return getResponseDelegate().getHeader(name);
    }

    @Override
    public String encodeUrl(String url) {
        return getResponseDelegate().encodeUrl(url);
    }

    @Override
    public String encodeURL(String url) {
        return getResponseDelegate().encodeURL(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return getResponseDelegate().encodeRedirectUrl(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return getResponseDelegate().encodeRedirectURL(url);
    }

    @Override
    public boolean containsHeader(String name) {
        return getResponseDelegate().containsHeader(name);
    }

    @Override
    public void addIntHeader(String name, int value) {
        getResponseDelegate().addIntHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        getResponseDelegate().addHeader(name, value);
    }

    @Override
    public void addDateHeader(String name, long date) {
        getResponseDelegate().addDateHeader(name, date);
    }

    @Override
    public void addCookie(Cookie cookie) {
        getResponseDelegate().addCookie(cookie);
    }
}
