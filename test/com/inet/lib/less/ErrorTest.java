package com.inet.lib.less;

import org.junit.Test;

import static org.junit.Assert.*;

public class ErrorTest {

    @Test
    public void parenthesisWithComma() {
        Less.compile( null, ".a { a: (red); }", false );
        try {
            Less.compile( null, ".a { a: (red,green); }", false );
            fail( "LessException expected" );
        } catch( LessException lex ) {
            String message = lex.getMessage();
            assertEquals( "Unrecognised input", message.substring( 0, message.indexOf( '\n' ) ) );
        }
    }

    @Test
    public void maxDiffTypes() {
        try {
            Less.compile( null, ".a { a: max( 1px, 1% ); }", false );
            fail( "LessException expected" );
        } catch( LessException lex ) {
            String message = lex.getMessage();
            assertEquals( "Incompatible types", message.substring( 0, message.indexOf( '\n' ) ) );
        }
    }

    @Test
    public void minDiffTypes() {
        try {
            Less.compile( null, ".a { a: min( 1px, 1% ); }", false );
            fail( "LessException expected" );
        } catch( LessException lex ) {
            String message = lex.getMessage();
            assertEquals( "Incompatible types", message.substring( 0, message.indexOf( '\n' ) ) );
        }
    }
}
