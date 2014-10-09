package org.openrepose.filters.uriidentity;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.filters.uriidentity.config.IdentificationMapping;
import org.openrepose.filters.uriidentity.config.UriIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UriIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriIdentityHandler> {

    public static final Double DEFAULT_QUALITY = 0.5;
    private static final String DEFAULT_GROUP = "User_Standard";
    private List<Pattern> patterns = new ArrayList<Pattern>();
    private UriIdentityConfig config;
    private Double quality;
    private String group;

    public UriIdentityHandlerFactory() {
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

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(UriIdentityConfig configurationObject) {

            config = configurationObject;
            patterns.clear();


            for (IdentificationMapping identificationMapping : config.getIdentificationMappings().getMapping()) {
                patterns.add(Pattern.compile(identificationMapping.getIdentificationRegex()));
            }

            quality = determineQuality();
            group = StringUtilities.getNonBlankValue(group, DEFAULT_GROUP);

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected UriIdentityHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new UriIdentityHandler(patterns, group, quality);
    }

    private Double determineQuality() {
        Double q = DEFAULT_QUALITY;
        Double configQuality;

        if (config != null) {
            configQuality = config.getQuality();
            if (configQuality != null) {
                q = configQuality;
            }
        }

        return q;
    }
}