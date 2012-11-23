package com.rackspace.papi.commons.util.logging.jersey;

import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.ReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.io.*;
import java.util.List;
import java.util.Map;


public class LoggingFilter extends ClientFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

    private static final String NOTIFICATION_PREFIX = "* ";
    
    private static final String REQUEST_PREFIX = "> ";
    
    private static final String RESPONSE_PREFIX = "< ";
    
    private final class Adapter extends AbstractClientRequestAdapter {
        private final StringBuilder b;

        Adapter(ClientRequestAdapter cra, StringBuilder b) {
            super(cra);
            this.b = b;
        }

        public OutputStream adapt(ClientRequest request, OutputStream out) throws IOException {
            return new LoggingOutputStream(getAdapter().adapt(request, out), b);
        }
        
    }

    private final class LoggingOutputStream extends OutputStream {
        private final OutputStream out;
        
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        private final StringBuilder b;

        LoggingOutputStream(OutputStream out, StringBuilder b) {
            this.out = out;
            this.b = b;
        }
        
        @Override
        public void write(byte[] b)  throws IOException {
            baos.write(b);
            out.write(b);
        }
    
        @Override
        public void write(byte[] b, int off, int len)  throws IOException {
            baos.write(b, off, len);
            out.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
            out.write(b);
        }

        @Override
        public void close() throws IOException {
            printEntity(b, baos.toByteArray());
            log(b);
            out.close();
        }
    }

    private final PrintStream loggingStream;

    private final Logger logger;

    private long id = 0;

    /**
     * Create a logging filter logging the request and response to
     * a default JDK logger, named as the fully qualified class name of this
     * class.
     */
    public LoggingFilter() {
        this(LOGGER);
    }

    /**
     * Create a logging filter logging the request and response to
     * a JDK logger.
     * 
     * @param logger the logger to log requests and responses.
     */
    public LoggingFilter(Logger logger) {
        this.loggingStream = null;
        this.logger = logger;
    }

    /**
     * Create a logging filter logging the request and response to
     * print stream.
     *
     * @param loggingStream the print stream to log requests and responses.
     */
    public LoggingFilter(PrintStream loggingStream) {
        this.loggingStream = loggingStream;
        this.logger = null;
    }

    private void log(StringBuilder b) {
        if (logger != null) {
            logger.info(b.toString());
        } else {
            loggingStream.print(b);
        }
    }

    private StringBuilder prefixId(StringBuilder b, long localId) {
        b.append(Long.toString(localId)).append(" ");
        return b;
    }

    @Override
    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        long localId = ++this.id;

        logRequest(localId, request);

        ClientResponse response = getNext().handle(request);

        logResponse(localId, response);

        return response;
    }

    private void logRequest(long localId, ClientRequest request) {
        StringBuilder b = new StringBuilder();
        
        printRequestLine(b, localId, request);
        printRequestHeaders(b, localId, request.getHeaders());

        if (request.getEntity() != null) {
            request.setAdapter(new Adapter(request.getAdapter(), b));
        } else {
            log(b);
        }
    }

    private void printRequestLine(StringBuilder b, long localId, ClientRequest request) {
        prefixId(b, localId).append(NOTIFICATION_PREFIX).append("Client out-bound request").append("\n");
        prefixId(b, localId).append(REQUEST_PREFIX).append(request.getMethod()).append(" ").
                append(request.getURI().toASCIIString()).append("\n");
    }

    private void printRequestHeaders(StringBuilder b, long localId, MultivaluedMap<String, Object> headers) {
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            List<Object> val = e.getValue();
            String header = e.getKey();

            if(val.size() == 1) {
                prefixId(b, localId).append(REQUEST_PREFIX).append(header).append(": ").append(ClientRequest.getHeaderValue(val.get(0))).append("\n");
            } else {
                StringBuilder sb = new StringBuilder();
                boolean add = false;
                for(Object o : val) {
                    if(add) sb.append(',');
                    add = true;
                    sb.append(ClientRequest.getHeaderValue(o));
                }
                prefixId(b, localId).append(REQUEST_PREFIX).append(header).append(": ").append(sb.toString()).append("\n");
            }
        }
    }

    private void logResponse(long localId, ClientResponse response) {
        StringBuilder b = new StringBuilder();

        printResponseLine(b, localId, response);
        printResponseHeaders(b, localId, response.getHeaders());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = response.getEntityInputStream();
        try {
            ReaderWriter.writeTo(in, out);

            byte[] requestEntity = out.toByteArray();
            printEntity(b, requestEntity);
            response.setEntityInputStream(new ByteArrayInputStream(requestEntity));
        } catch (IOException ex) {
            throw new ClientHandlerException(ex);
        }
        log(b);
    }

    private void printResponseLine(StringBuilder b, long localId, ClientResponse response) {
        prefixId(b, localId).append(NOTIFICATION_PREFIX).
                append("Client in-bound response").append("\n");
        prefixId(b, localId).append(RESPONSE_PREFIX).
                append(Integer.toString(response.getStatus())).
                append("\n");
    }
    
    private void printResponseHeaders(StringBuilder b, long localId, MultivaluedMap<String, String> headers) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            String header = e.getKey();
            for (String value : e.getValue()) {
                prefixId(b, localId).append(RESPONSE_PREFIX).append(header).append(": ").
                        append(value).append("\n");
            }
        }
        prefixId(b, localId).append(RESPONSE_PREFIX).append("\n");
    }

    private void printEntity(StringBuilder b, byte[] entity) throws IOException {
        if (entity.length == 0)
            return;
        b.append(new String(entity)).append("\n");
    }   
}