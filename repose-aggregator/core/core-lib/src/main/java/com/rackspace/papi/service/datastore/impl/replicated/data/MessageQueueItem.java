package com.rackspace.papi.service.datastore.impl.replicated.data;

public class MessageQueueItem {

    private final Subscriber subscriber;
    private final Message message;

    public MessageQueueItem(Subscriber subscriber, Message message) {
        if (subscriber == null || message == null) {
            throw new IllegalArgumentException("Subscriber and message must not be null");
        }
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
