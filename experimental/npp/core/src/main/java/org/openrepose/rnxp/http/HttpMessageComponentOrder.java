package org.openrepose.rnxp.http;

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
        HttpMessageComponent.ENTITY_HEADER,
        HttpMessageComponent.CONTENT_START,
        HttpMessageComponent.CONTENT,
    });
    
    public static HttpMessageComponentOrder requestOrderInstance() {
        return REQUEST_COMP_ORDER;
    }
    
    private final HttpMessageComponent[] order;

    public HttpMessageComponentOrder(HttpMessageComponent[] order) {
        this.order = order;
    }

    public boolean isBefore(HttpMessageComponent first, HttpMessageComponent second) {
        return indexOf(first) < indexOf(second);
    }

    public boolean isEqualOrAfter(HttpMessageComponent first, HttpMessageComponent second) {
        return indexOf(first) >= indexOf(second);
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
