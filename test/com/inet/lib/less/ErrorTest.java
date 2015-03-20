package com.inet.lib.less;

import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorTest {

    @Test
    public void parenthesisWithComma() {
        Less.compile( null, ".a { a: (red); }", false );
        try {
            Less.compile( null, ".a { a: (red,green); }", false );
        } catch( LessException lex ) {
            String message = lex.getMessage();
            assertEquals( "Unrecognised input", message.substring( 0, message.indexOf( '\n' ) ) );
        }
    }
}
