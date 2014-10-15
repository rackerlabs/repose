package org.openrepose.core.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * THis will be eagerly loaded as soon as the JVM fires up, which is exactly what we want.
 */
public class CoreSpringProvider {
    private static volatile CoreSpringProvider instance = new CoreSpringProvider();

    private CoreSpringProvider() {
    }

    public static CoreSpringProvider getInstance() {
        return instance;
    }

}
