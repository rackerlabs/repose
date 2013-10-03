package com.rackspace.papi.service.reporting.metrics.impl;

import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.TimerByCategory;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implements a collection of Timers which share the same JMX type & scope.  These Timers are usually related in some
 * fashion.
 * <p>
 * By calling the time(), stop(), and update() methods, a Timer object is automatically registered and can be updated
 * by later calls.
 * <p>
 * This is created by the {@link com.rackspace.papi.service.reporting.metrics.impl.MetricsServiceImpl} factory class.
 * <p>
 * This class is thread-safe.
 *
 */
public class TimerByCategoryImpl implements TimerByCategory {

    private Map<String, Timer> map = new HashMap<String, Timer>();
    private MetricsService metricsService;
    private Class klass;
    private String scope;
    private TimeUnit duration;
    private TimeUnit rate;

    TimerByCategoryImpl(MetricsService metricsServiceP, Class klassP, String scopeP, TimeUnit durationP,
                        TimeUnit rateP) {
        this.metricsService = metricsServiceP;
        this.klass = klassP;
        this.scope = scopeP;
        this.duration = durationP;
        this.rate = rateP;
    }

    public void update(String key, long duration, TimeUnit unit) {
        verifyGet(key).update(duration, unit);
    }

    public TimerContext time(String key) {
        return verifyGet(key).time();
    }

    private Timer verifyGet(String key) {
        //assert metricsService != null;

        if ( !map.containsKey( key ) )

            synchronized ( this ) {

                if ( !map.containsKey( key ) ) {

                    map.put( key, metricsService.newTimer(klass, key, scope, duration, rate) );
                }
            }

        return map.get( key );
    }
}
