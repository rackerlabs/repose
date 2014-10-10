package org.openrepose.core.services.deploy;

/**
 * @author fran
 */
public class DeploymentDirectoryNotFoundException extends RuntimeException {
    public DeploymentDirectoryNotFoundException(String message) {
        super(message);
    }
}
