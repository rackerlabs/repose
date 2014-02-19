package com.rackspace.papi.service.healthcheck;

public class InputNullException extends Exception{

    public InputNullException(){
        super("Cannot pass null as an input");
    }

    public InputNullException(String identifier){
        super("Cannot pass a null value for " + identifier);
    }
}
