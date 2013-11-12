package com.rackspace.papi.service.serviceclient.akka;


import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class ReusableServiceClientResponse<E> extends ServiceClientResponse {

    private byte [] dataArray;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ReusableServiceClientResponse.class);

    public ReusableServiceClientResponse(int code, InputStream data) {
       super(code,data);

        try{
            dataArray = IOUtils.toByteArray(data);
        } catch(IOException e){
           LOG.error("Not able read inputstream to byte array: "+e.getMessage());
        }
    }

    @Override
    public InputStream getData() {
        return new ByteArrayInputStream(dataArray);
    }



}
