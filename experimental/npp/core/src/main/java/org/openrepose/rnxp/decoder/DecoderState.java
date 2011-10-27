package org.openrepose.rnxp.decoder;

/**
 *
 * @author zinic
 */
public enum DecoderState {

    // Request line states
    READ_SC_PARSE_METHOD,
    READ_MC_PARSE_METHOD,
    READ_URI,
    READ_VERSION,
    
    // Header line states
    READ_HEADER_KEY,
    READ_HEADER_VALUE,
    
    // Body states
    START_CONTENT,
    READ_CONTENT,
    
    // Control States
    STREAM_REMAINING,
    READ_END
}
