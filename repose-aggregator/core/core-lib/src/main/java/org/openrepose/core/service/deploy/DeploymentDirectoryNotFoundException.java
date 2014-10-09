package org.openrepose.core.service.deploy;

/**
 * @author fran
 */
public class DeploymentDirectoryNotFoundException extends RuntimeException {
    public DeploymentDirectoryNotFoundException(String message) {
        super(message);
    }
}
