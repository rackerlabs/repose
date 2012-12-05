package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.http.proxy.common.AbstractResponseProcessor;
import com.sun.jersey.api.client.ClientResponse;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

class JerseyResponseProcessor extends AbstractResponseProcessor {

    private static final int MAX_RESPONSE_BUFFER_SIZE = 2048;
    private static final int READ_BUFFER_SIZE = 1024;
    private final ClientResponse clientResponse;

    public JerseyResponseProcessor(ClientResponse clientResponse, HttpServletResponse response) {
        super(response, clientResponse.getStatus());
        this.clientResponse = clientResponse;
    }

    @Override
    protected void setResponseHeaders() throws IOException {
        MultivaluedMap<String, String> headers = clientResponse.getHeaders();
        
        final Set<Entry<String,List<String>>> entries = headers.entrySet();
        
        for(Entry<String,List<String>> entry: entries){
           for(String value: entry.getValue()){
              addHeader(entry.getKey(), value);
           }
        }
    }

    @Override
    protected void setResponseBody() throws IOException {
        int len = clientResponse.getLength();

        if (getResponse() instanceof MutableHttpServletResponse) {
            if (len > 0 && len < MAX_RESPONSE_BUFFER_SIZE) {
                // Tell Jersey to buffer entity and close input
                clientResponse.bufferEntity();
            }
            MutableHttpServletResponse mutableResponse = (MutableHttpServletResponse) getResponse();
            mutableResponse.setInputStream(new JerseyInputStream(clientResponse));
        } else {
            final InputStream source = clientResponse.getEntityInputStream();

            if (source != null) {

                final BufferedInputStream httpIn = new BufferedInputStream(source);
                final OutputStream clientOut = getResponse().getOutputStream();

                //Using a buffered stream so this isn't nearly as expensive as it looks
                byte bytes[] = new byte[READ_BUFFER_SIZE];
                int readData = httpIn.read(bytes);

                while (readData != -1) {
                    clientOut.write(bytes, 0, readData);
                    readData = httpIn.read(bytes);
                }

                httpIn.close();
                clientOut.flush();
                clientOut.close();
            }
            clientResponse.close();
        }
    }
}
