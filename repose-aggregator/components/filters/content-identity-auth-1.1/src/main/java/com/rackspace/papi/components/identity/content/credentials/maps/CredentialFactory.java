package com.rackspace.papi.components.identity.content.credentials.maps;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.content.credentials.wrappers.MossoCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.NastCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.PasswordCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.UserCredentialsWrapper;
import com.rackspacecloud.docs.auth.api.v1.*;

import java.util.Map;

public final class CredentialFactory {

    private CredentialFactory() {
    }

    public static AuthCredentials getCredentials(CredentialType type, Map<String, Object> credentials) {
        switch (type) {
            case MOSSO:
                return new MossoCredentialsWrapper(credentials);
            case NAST:
                return new NastCredentialsWrapper(credentials);
            case PASSWORD:
                return new PasswordCredentialsWrapper(credentials);
            case USER:
                return new UserCredentialsWrapper(credentials);
        }

        return null;
    }

    public static AuthCredentials getCredentials(Credentials credentials) {
        AuthCredentials authCredentials = null;

        if (credentials != null) {
            if (credentials instanceof MossoCredentials) {

                MossoCredentialsWrapper credentialsWrapper = new MossoCredentialsWrapper();
                credentialsWrapper.setMossoCredentials((MossoCredentials) credentials);
                authCredentials = credentialsWrapper;
            } else if (credentials instanceof NastCredentials) {

                NastCredentialsWrapper credentialsWrapper = new NastCredentialsWrapper();
                credentialsWrapper.setNastCredentials((NastCredentials) credentials);
                authCredentials = credentialsWrapper;
            } else if (credentials instanceof PasswordCredentials) {

                PasswordCredentialsWrapper credentialsWrapper = new PasswordCredentialsWrapper();
                credentialsWrapper.setPasswordCredentials((PasswordCredentials) credentials);
                authCredentials = credentialsWrapper;
            } else if (credentials instanceof UserCredentials) {

                UserCredentialsWrapper credentialsWrapper = new UserCredentialsWrapper();
                credentialsWrapper.setUserCredentials((UserCredentials) credentials);
                authCredentials = credentialsWrapper;
            }
        }

        return authCredentials;
    }
}
