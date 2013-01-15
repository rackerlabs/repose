package org.openrepose.rackspace.auth_2_0.identity.content.auth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.rackspace.auth2.content_identity.config.ContentIdentityAuthConfig;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.HashMap;
import java.util.Map;

public class ContentIdentityAuthHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ContentIdentityAuthHandler> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ContentIdentityAuthHandlerFactory.class);
    private ContentIdentityAuthConfig config;
    private JacksonJaxbTransform jsonTranformer;
    private Unmarshaller unmarshaller;

    public ContentIdentityAuthHandlerFactory() {
        jsonTranformer = new JacksonJaxbTransform();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(org.openstack.docs.identity.api.v2.ObjectFactory.class,
                    com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory.class);
            unmarshaller = jaxbContext.createUnmarshaller();
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
        return new ContentIdentityAuthHandler(config, jsonTranformer, unmarshaller);
    }
}
