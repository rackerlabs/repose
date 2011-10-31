package org.openrepose.rnxp.decoder;

/**
 *
 * @author zinic
 */
public enum DecoderState {

    // Shared head states
    READ_VERSION,
    
    // Status line states
    READ_STATUS_CODE,
    READ_REASON_PHRASE,
    
    // Request line states
    READ_SC_PARSE_METHOD,
    READ_MC_PARSE_METHOD,
    READ_URI,
    
    // Header line states
    READ_HEADER_KEY,
    READ_HEADER_VALUE,
    
    // Body states
    START_CONTENT,
    READ_CONTENT,
    READ_CHUNK_LENGTH,
    READ_CONTENT_CHUNKED,
    READ_CHUNK_FOOTER,
    
    // Control
    STOP
}
