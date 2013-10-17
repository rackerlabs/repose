package com.rackspace.papi.commons.util.logging.apache.format.converters;

import com.rackspace.papi.commons.util.StringUtilities;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public enum TypeConversionFormatFactory {

    DATE(DateTimeFormatConverter.class);

    TypeConversionFormatFactory(Class<? extends FormatConverter> converter) {
        try {
            ConverterMap.addConverter(name(), converter.newInstance());
        } catch (InstantiationException ex) {
            LoggerFactory.getLogger(TypeConversionFormatFactory.class).error("Unable to instantiate converter: " + converter.getName(), ex);
        } catch (IllegalAccessException ex) {
            LoggerFactory.getLogger(TypeConversionFormatFactory.class).error("Unable to instantiate converter: " + converter.getName(), ex);
        }
    }

    public static FormatConverter getConverter(String type) {
        return ConverterMap.getConverter(type);
    }
}

final class ConverterMap {

    private static final Map<String, FormatConverter> CONVERSION_MAP = new HashMap<String, FormatConverter>();
    
    private ConverterMap(){
    }

    public static void addConverter(String name, FormatConverter converter) {
        CONVERSION_MAP.put(name, converter);
    }

    public static FormatConverter getConverter(String name) {
        if (StringUtilities.isBlank(name)) {
            return null;
        }

        return CONVERSION_MAP.get(name);
    }
}
