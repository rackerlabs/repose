package com.rackspace.papi.service.datastore.impl.redundant.data;

public class MessageQueueItem {

    private final Subscriber subscriber;
    private final Message message;

    public MessageQueueItem(Subscriber subscriber, Message message) {
        this.subscriber = subscriber;
        this.message = message;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public Message getMessage() {
        return message;
    }
}
