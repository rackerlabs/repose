/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.commons.util.io.charset;
import java.nio.charset.Charset;
import org.slf4j.Logger;

/**
 *
 * @author malconis
 */
public final class CharacterSetSupport {
    
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CharacterSetSupport.class);
    
    public static void checkCharSet(String charset){
        
        if(!Charset.isSupported(charset)){
            LOG.error("HTTP Logger will only work if your system supports UTF-8. Please remove this filter from the Power-Proxy config and restart.");
            System.exit(1);
        }
    }
    
}
