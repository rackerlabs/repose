package com.rackspace.papi.service.deploy;

/**
 * @author fran
 */
public class DeploymentDirectoryNotFoundException extends RuntimeException {
    public DeploymentDirectoryNotFoundException(String message) {
        super(message);
    }
}
