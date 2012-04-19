package org.openrepose.rnxp.http;

import java.util.Arrays;

/**
 *
 * @author zinic
 */
public class HttpMessageComponentOrder {

    private static final HttpMessageComponentOrder REQUEST_COMP_ORDER = new HttpMessageComponentOrder(new HttpMessageComponent[]{
        HttpMessageComponent.MESSAGE_START,
        HttpMessageComponent.REQUEST_METHOD,
        HttpMessageComponent.REQUEST_URI,
        HttpMessageComponent.HTTP_VERSION,
        HttpMessageComponent.HEADER,
        HttpMessageComponent.CONTENT_START,
        HttpMessageComponent.CONTENT,
        HttpMessageComponent.MESSAGE_END_NO_CONTENT,
        HttpMessageComponent.MESSAGE_END_WITH_CONTENT
    });

    private static final HttpMessageComponentOrder RESPONSE_COMP_ORDER = new HttpMessageComponentOrder(new HttpMessageComponent[]{
        HttpMessageComponent.MESSAGE_START,
        HttpMessageComponent.RESPONSE_STATUS_CODE,
        HttpMessageComponent.HTTP_VERSION,
        HttpMessageComponent.HEADER,
        HttpMessageComponent.CONTENT_START,
        HttpMessageComponent.CONTENT,
        HttpMessageComponent.MESSAGE_END_NO_CONTENT,
        HttpMessageComponent.MESSAGE_END_WITH_CONTENT
    });
    
    public static HttpMessageComponentOrder requestOrderInstance() {
        return REQUEST_COMP_ORDER;
    }
    
    public static HttpMessageComponentOrder responseOrderInstance() {
        return RESPONSE_COMP_ORDER;
    }
    
    private final HttpMessageComponent[] order;

    private HttpMessageComponentOrder(HttpMessageComponent[] order) {
        this.order = Arrays.copyOf(order, order.length);
    }

    public boolean isBefore(HttpMessageComponent first, HttpMessageComponent second) {
        return indexOf(first) < indexOf(second);
    }
    
    public boolean isAfter(HttpMessageComponent first, HttpMessageComponent second) {
        return indexOf(first) > indexOf(second);
    }

    public boolean isEqual(HttpMessageComponent first, HttpMessageComponent second) {
        return indexOf(first) == indexOf(second);
    }

    public boolean isAfterOrEqual(HttpMessageComponent first, HttpMessageComponent second) {
        final int firstIndex = indexOf(first);
        final int secondIndex = indexOf(second);
        
        return firstIndex >= secondIndex;
    }
    
    public int indexOf(HttpMessageComponent component) {
        for (int i = 0; i < order.length; i++) {
            if (component == order[i]) {
                return i;
            }
        }
        
        return -1;
    }
    
    public HttpMessageComponent nextComponent(HttpMessageComponent component) {
        for (int i = 0; i < order.length; i++) {
            if (component == order[i]) {
                return ++i < order.length ? order[i] : null;
            }
        }
        
        return null;
    }
}
