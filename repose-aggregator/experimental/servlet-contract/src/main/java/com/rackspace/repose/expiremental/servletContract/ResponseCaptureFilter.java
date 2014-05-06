package com.rackspace.repose.experimental.servletContract;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This test is to verify that repose supports the contract on the ServletResponse.getOutputStream()
 * and ServletResponse.getWriter() methods.
 *
 * If I pass a ResponseWrapper along the filter chain and override the getOutputStream() & getWriter() methods, I should
 * be able to access the results written to those methods through the ResponseWrapper.getContent() method, contained
 * in this file.  This isn't the case.  The call to getContent() is empty, even though data had been written to
 * the response's outputstream and is viewable by the http client which made the request.
 *
 * This project creates an ear file which provides the 'filter-test' filter which can be included in the filter chain.
 *
 * If the call to getContent() is empty, this filter throws and exception and the response from the origin service
 * is received by the client.
 *
 * If the call to getContent() provides the response, this filter appends additional content to the response.
 *
 * PS - ServletResponse.getContentType() returns null as well, although the content type can be accessed through the
 * call to ServletResponse.getHeaders()
 */
public class ResponseCaptureFilter implements Filter {


    @Override
    public void init( FilterConfig filterConfig ) throws ServletException {

       System.out.println( "Start " + this.getClass().getName() );
    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
          throws IOException, ServletException {

        // create response wrapper to capture the output stream from  further down the filter chain
        ResponseWrapper respWrap = new ResponseWrapper( (HttpServletResponse) servletResponse );

        filterChain.doFilter( servletRequest, respWrap );

        HttpServletRequest req = (HttpServletRequest)servletRequest;

        // Print out info from request & response wrapper
        System.out.println( "URI: " + req.getRequestURI() );
        System.out.println( "Status: " + respWrap.getStatus() );
        System.out.println( "resp Header 'Content-Type: " + respWrap.getHeader( "Content-Type" ) );

        String content = respWrap.getContent();

        System.out.println( "Content Body: '" + content + "'" );

        // verify that the content is not empty.  This fails in repose but works in tomcat
        if( content.isEmpty() ) {

            throw new RuntimeException( "Content is empty" );
        }

        // writer content to the actual servletResponse & append additional content
        servletResponse.getWriter().write( content + "<extra> Added by TestFilter, should also see the rest of the content </extra>" );
        servletResponse.getWriter().flush();
    }

    @Override
    public void destroy() { }

    private class FilterServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream stream;

        public FilterServletOutputStream( ByteArrayOutputStream streamP ) {
            stream = streamP;
        }

        @Override
        public void write(int b) throws IOException  {
            stream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException  {
            stream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException  {
            stream.write(b,off,len);
        }
    }

    private class ResponseWrapper extends HttpServletResponseWrapper {

        private ByteArrayOutputStream stream = new ByteArrayOutputStream();
        private PrintWriter writer = new PrintWriter( stream );
        private ServletOutputStream soStream = new FilterServletOutputStream( stream );

        public String getContent() {
            try {
                stream.flush();
                stream.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
            return stream.toString();
        }

        public ResponseWrapper( HttpServletResponse resp ) {
            super( resp );
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {

            return soStream;
        }

        @Override
        public PrintWriter getWriter() {

            return writer;
        }
    }
}
