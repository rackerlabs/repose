/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.reporting.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openrepose.core.spring.ReposeJmxNamingStrategy;

import javax.inject.Inject;
import javax.inject.Named;

@Aspect
@Named
public class MetricRegistryAspect {

    private final static String REPOSE_PACKAGE = "org.openrepose";

    private final ReposeJmxNamingStrategy reposeJmxNamingStrategy;

    @Inject
    public MetricRegistryAspect(ReposeJmxNamingStrategy reposeJmxNamingStrategy) {
        this.reposeJmxNamingStrategy = reposeJmxNamingStrategy;
    }

    @Around("execution(* com.codahale.metrics.MetricRegistry.*(java.lang.String,..)) && args(name,..)")
    private Object prefixMetricName(ProceedingJoinPoint pjp, String name) throws Throwable {
        Object[] methodArguments = pjp.getArgs();
        if (name.startsWith(REPOSE_PACKAGE)) {
            methodArguments[0] = String.join("", reposeJmxNamingStrategy.getJmxPrefix(), name);
        }
        return pjp.proceed(methodArguments);
    }
}
