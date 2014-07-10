package com.rackspace.papi.service.event;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventDispatcher;
import com.rackspace.papi.service.event.common.EventListener;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.event.common.impl.EventDispatcherImpl;
import com.rackspace.papi.service.event.common.impl.EventListenerDescriptor;
import com.rackspace.papi.service.event.impl.SimpleEvent;
import com.rackspace.papi.service.threading.ThreadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class PowerProxyEventManager implements EventService {
    private static final Logger LOG = LoggerFactory.getLogger(PowerProxyEventManager.class);

    private final Map<ComparableClassWrapper<Enum>, Set<EventListenerDescriptor>> listenerMap;
    private final Queue<Event> eventQueue;
    private final Lock eventQueueLock;
    private final Condition queueNotEmpty;
    private final ThreadingService threadingService;
    private final PowerProxyEventKernel eventKernel;

    private DestroyableThreadWrapper eventKernelThread;

    @Autowired
    public PowerProxyEventManager(PowerProxyEventKernel eventKernel, ThreadingService threadingService) {
        this.threadingService = threadingService;
        this.eventKernel = eventKernel;

        listenerMap = new TreeMap<>();
        eventQueue = new LinkedList<>();

        eventQueueLock = new ReentrantLock();
        queueNotEmpty = eventQueueLock.newCondition();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        eventKernelThread = new DestroyableThreadWrapper(threadingService.newThread(eventKernel, "Event Kernel Thread"), eventKernel);
        eventKernelThread.start();
    }

    @PreDestroy
    public void destroy() {
        eventKernelThread.destroy();
    }

    @Override
    public void newEvent(Enum e, Object payload) {
        eventQueueLock.lock();

        try {
            eventQueue.add(new SimpleEvent(e, payload, this));

            if (eventQueue.size() == 1) {
                queueNotEmpty.signalAll();
            }
        } finally {
            eventQueueLock.unlock();
        }
    }

    @Override
    public synchronized EventDispatcher nextDispatcher() throws InterruptedException {
        final Event e = nextEvent();

        return new EventDispatcherImpl(e, Collections.unmodifiableSet(getOrCreateListenerSet(e.type().getClass())));
    }

    @Override
    public <T extends Enum> void listen(EventListener<T, ?> el, Class<T> events) {
        regsiterListener(el, events, EnumSet.allOf(events));
    }

    @Override
    public <T extends Enum> void listen(EventListener<T, ?> el, T... events) {
        if (events == null || events.length == 0) {
            throw new IllegalArgumentException("Must subscribe to at least one event type");
        }

        for (T event : events) {
            if (event != null) {
                regsiterListener(el, (Class<T>) event.getClass(), Arrays.asList(events));
                break;
            }
        }
    }

    @Override
    public <T extends Enum> void squelch(EventListener<T, ?> el, Class<T> events) {
        final Set<EventListenerDescriptor> listenerSet = listenerMap.get(new ComparableClassWrapper<Enum>(events));

        if (listenerSet != null) {
            final Iterator<EventListenerDescriptor> itr = listenerSet.iterator();

            while (itr.hasNext()) {
                final EventListenerDescriptor<T> elw = itr.next();

                if (elw.getListener() == el) {
                    itr.remove();
                    break;
                }
            }
        }
    }

    @Override
    public <T extends Enum> void squelch(EventListener<T, ?> el, T... events) {
        if (events == null || events.length == 0) {
            throw new IllegalArgumentException("Must unsubscribe from at least one event type");
        }

        final Set<EventListenerDescriptor> listenerSet = listenerMap.get(new ComparableClassWrapper<Enum>(events[0].getClass()));

        if (listenerSet != null) {
            final Iterator<EventListenerDescriptor> itr = listenerSet.iterator();

            while (itr.hasNext()) {
                final EventListenerDescriptor<T> elw = itr.next();

                if (elw.getListener() == el) {
                    if (elw.silence(Arrays.asList(events))) {
                        itr.remove();
                    }

                    break;
                }
            }
        }
    }

    private Event nextEvent() throws InterruptedException {
        eventQueueLock.lock();

        try {
            while (eventQueue.size() == 0) {
                queueNotEmpty.await();
            }

            return eventQueue.poll();
        } catch (InterruptedException ie) {
            LOG.trace("Power Proxy Event Manager Interrupted", ie);
            Thread.currentThread().interrupt();
            throw ie;
        } finally {
            eventQueueLock.unlock();
        }
    }

    private <T extends Enum> void regsiterListener(EventListener<T, ?> el, Class<T> enumClass, Collection<T> events) {
        boolean found = false;

        final Set<EventListenerDescriptor> descriptorSet = getOrCreateListenerSet(enumClass);

        for (EventListenerDescriptor<T> descriptor : descriptorSet) {
            if (descriptor.getListener() == el) {
                descriptor.listenFor(events);
                found = true;

                break;
            }
        }

        if (!found) {
            descriptorSet.add(new EventListenerDescriptor<T>(el, events));
        }
    }

    private <T extends Enum> Set<EventListenerDescriptor> getOrCreateListenerSet(Class<T> e) {
        final ComparableClassWrapper<Enum> classWrapper = new ComparableClassWrapper<Enum>(e);
        Set<EventListenerDescriptor> listenerSet = listenerMap.get(classWrapper);

        if (listenerSet == null) {
            listenerSet = new HashSet<EventListenerDescriptor>();
            listenerMap.put(classWrapper, listenerSet);
        }

        return listenerSet;
    }
}