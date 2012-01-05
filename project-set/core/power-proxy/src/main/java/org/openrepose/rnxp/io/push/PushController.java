package org.openrepose.rnxp.io.push;

public interface PushController {

    void requestNext();
    
    void stopMessageFlow();
}