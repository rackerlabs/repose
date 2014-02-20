package com.rackspace.papi.service.healthcheck;

public class NotRegisteredException extends Exception {

    public NotRegisteredException(String UID) {
        super("ID with " + UID + " not registered with the Health Check Service");
    }

}
