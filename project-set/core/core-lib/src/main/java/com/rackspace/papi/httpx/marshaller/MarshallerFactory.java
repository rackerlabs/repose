package com.rackspace.papi.httpx.marshaller;

/**
 * @author fran
 */
public final class MarshallerFactory {
    
    private MarshallerFactory(){
    }
    
    public static Marshaller newInstance() {
        return new MessageEnvelopeMarshaller();
    }
}
