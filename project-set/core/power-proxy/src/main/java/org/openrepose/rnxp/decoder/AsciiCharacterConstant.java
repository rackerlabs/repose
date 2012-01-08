package org.openrepose.rnxp.decoder;

/**
 *
 * @author zinic
 */
public enum AsciiCharacterConstant {
    CARRIAGE_RETURN('\r'),
    COLON(':'),
    LINE_FEED('\n'),
    SPACE(' ');
    
    private final char characterValue;

    private AsciiCharacterConstant(char characterValue) {
        this.characterValue = characterValue;
    }
    
    public boolean matches(char character) {
        return characterValue == character;
    }
}
