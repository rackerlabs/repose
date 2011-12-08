/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.logging.util;
import java.nio.charset.Charset;
import org.slf4j.Logger;

/**
 *
 * @author malconis
 */
public final class CharacterSetSupport {
    
    private static final String CHAR_SET = "UTF-8";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CharacterSetSupport.class);
    
    public void checkCharSet(){
        
        if(!Charset.isSupported(CHAR_SET)){
            LOG.error("HTTP Logger only supports UTF-8 character encoding");
            System.exit(0);
        }
    }
    
}
