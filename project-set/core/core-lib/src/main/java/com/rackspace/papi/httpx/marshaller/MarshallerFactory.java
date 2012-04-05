package com.rackspace.papi.httpx.marshaller;

/**
 * @author fran
 */
public class MarshallerFactory {
    
    private MarshallerFactory(){
    }
    
    public static Marshaller newInstance() {
        return new MessageEnvelopeMarshaller();
    }
}
