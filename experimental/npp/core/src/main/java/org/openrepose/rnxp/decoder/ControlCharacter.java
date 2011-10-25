package org.openrepose.rnxp.decoder;

/**
 *
 * @author zinic
 */
public class ControlCharacter {

    private final AsciiCharacterConstant character;

    public ControlCharacter(AsciiCharacterConstant character) {
        this.character = character;
    }

    public AsciiCharacterConstant getCharacterConstant() {
        return character;
    }
}
