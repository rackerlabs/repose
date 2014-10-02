package com.rackspace.papi.service.logging;

import java.util.Properties;

/**
 * @author fran
 */
public class LoggingServiceImpl implements LoggingService {
    public LoggingServiceImpl(){}

    @Override
    public void updateLoggingConfiguration(Properties loggingConfigFile) {
        //org.apache.log4j.PropertyConfigurator.configure(loggingConfigFile);
        try {
            Class<?> clazz = Class.forName("org.apache.log4j.PropertyConfigurator");
            java.lang.reflect.Method method = clazz.getDeclaredMethod("configure", Properties.class);
            method.invoke(null, loggingConfigFile);
        } catch (ClassNotFoundException | NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
            org.slf4j.LoggerFactory.getLogger(LoggingServiceImpl.class).warn("Unable to configure the selected Log4J framework!", e);
            System.err.println("Unable to configure the selected Log4J framework!");
            e.printStackTrace();
        }
    }
}
