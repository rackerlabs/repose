package com.rackspace.papi.jmx;

import java.util.HashMap;
import java.util.Map;

public class FilterInformation {
    private final String id;
    private final String name;
    private final String regex;
    private final String configuration;
    private boolean isConfiguarationLoaded;
    private Map successConfigurationLoadinginformation;
    private Map failedConfigurationLoadingInformation;

    public FilterInformation(String id, String name, String regex, String configuration, Boolean isConfiguarationLoaded) {
        this.id = id;
        this.name = name;
        this.regex = regex;
        this.configuration = configuration;
        this.isConfiguarationLoaded = isConfiguarationLoaded;
        successConfigurationLoadinginformation = new HashMap<String, String[]>();
        failedConfigurationLoadingInformation = new HashMap<String, String[]>();


    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }

    public String getConfiguration() {
        return configuration;
    }

    public Boolean getIsConfiguarationLoaded() {
        return isConfiguarationLoaded;
    }

    public void setConfiguarationLoaded(boolean isConfiguarationLoaded) {
        this.isConfiguarationLoaded = isConfiguarationLoaded;
    }

    public Map<String, String[]> getSuccessConfigurationLoadinginformation() {
        return successConfigurationLoadinginformation;
    }

    public void setSuccessConfigurationLoadinginformation(Map successConfigurationLoadinginformation) {
        this.successConfigurationLoadinginformation = successConfigurationLoadinginformation;
    }

    public Map<String, String[]> getFailedConfigurationLoadingInformation() {
        return failedConfigurationLoadingInformation;
    }

    public void setFailedConfigurationLoadingInformation(Map failedConfigurationLoadingInformation) {
        this.failedConfigurationLoadingInformation = failedConfigurationLoadingInformation;
    }

}
