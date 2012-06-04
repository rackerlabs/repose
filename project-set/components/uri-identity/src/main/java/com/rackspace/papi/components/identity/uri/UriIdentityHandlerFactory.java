package com.rackspace.papi.components.identity.uri;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.components.identity.uri.config.IdentificationMapping;
import com.rackspace.papi.components.identity.uri.config.UriIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class UriIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriIdentityHandler> {

    public static final String DEFAULT_QUALITY = "0.5";
    private static final String DEFAULT_GROUP = "User_Standard";
    private final KeyedRegexExtractor<Object> keyedRegexExtractor;
    private UriIdentityConfig config;
    private String quality, group;

    public UriIdentityHandlerFactory() {
        keyedRegexExtractor = new KeyedRegexExtractor<Object>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {

            {
                put(UriIdentityConfig.class, new UriIdentityConfigurationListener());
            }
        };
    }

    private class UriIdentityConfigurationListener implements UpdateListener<UriIdentityConfig> {

        @Override
        public void configurationUpdated(UriIdentityConfig configurationObject) {

            keyedRegexExtractor.clear();
            config = configurationObject;
            for (IdentificationMapping identificationMapping : config.getIdentificationMappings().getMapping()) {
                keyedRegexExtractor.addPattern(identificationMapping.getIdentificationRegex(), null);
            }

            quality = determineQuality();
            group = StringUtilities.getNonBlankValue(group, DEFAULT_GROUP);
        }
    }

    @Override
    protected UriIdentityHandler buildHandler() {
        return new UriIdentityHandler(keyedRegexExtractor, StringUtilities.getNonBlankValue(group, DEFAULT_GROUP), determineQuality());
    }

    private String determineQuality() {
        String q = DEFAULT_QUALITY;

        if (config != null) {
            q = StringUtilities.getNonBlankValue(config.getQuality(), DEFAULT_QUALITY);
        }

        return ";q=" + q;
    }
}
