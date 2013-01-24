package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.StreamToJaxbTransform;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import org.openrepose.rackspace.auth.content_identity.config.ContentIdentityAuthConfig;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ContentIdentityAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ContentIdentityAuthHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandlerFactory.class);
    private ContentIdentityAuthConfig config;
    private JacksonJaxbTransform jsonTranformer;
    private Transform<InputStream, JAXBElement<Credentials>> xmlTransformer;

    public ContentIdentityAuthHandlerFactory() {
        jsonTranformer = new JacksonJaxbTransform();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
            xmlTransformer = new StreamToJaxbTransform(jaxbContext);
        } catch (JAXBException e) {
            LOG.error("Error when creating JABXContext for auth credentials. Reason: " + e.getMessage(), e);
        }
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(ContentIdentityAuthConfig.class, new ContentIdentityAuthConfigurationListener());
            }
        };
    }

    private class ContentIdentityAuthConfigurationListener implements UpdateListener<ContentIdentityAuthConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContentIdentityAuthConfig configurationObject) {
            config = configurationObject;
            LOG.debug("Configuration updated (quality = '" + config.getQuality() + "' group = '" + config.getGroup() + "')");
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected ContentIdentityAuthHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new ContentIdentityAuthHandler(config, jsonTranformer, xmlTransformer);
    }
}
