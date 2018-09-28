package com.inet.lib.less;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ErrorTest {

    private void assertLessException( String less, String expectedErrorMessage ) {
        try {
            Less.compile( null, less, false );
            fail( "LessException expected" );
        } catch( LessException lex ) {
            String message = lex.getMessage();
            assertEquals( expectedErrorMessage, message.substring( 0, message.indexOf( '\n' ) ) );
        }
    }
    @Test
    public void parenthesisWithComma() {
        Less.compile( null, ".a { a: (red); }", false );
        assertLessException( ".a { a: (red,green); }", "Unrecognized input" );
    }

    @Test
    public void maxDiffTypes() {
        assertLessException( ".a { a: max( 1px, 1% ); }", "Incompatible types" );
    }

    @Test
    public void minDiffTypes() {
        assertLessException( ".a { a: min( 1px, 1% ); }", "Incompatible types" );
    }

    @Test
    public void unrecognizedInput1() {
        assertLessException( "a: > 5;", "Unrecognized input: '>'" );
    }

    @Test
    public void unrecognizedInput2() {
        assertLessException( "a: 1 <> 5;", "Unrecognized input: '>'" );
    }

    @Test
    public void unrecognizedInput3() {
        assertLessException( "a:}", "Unrecognized input: 'a:}'" );
    }

    @Test
    public void unrecognizedInput4() {
        assertLessException( "a:);", "Unrecognized input: 'a:)'" );
    }

    @Test
    public void unrecognizedInput5() {
        assertLessException( "/*comment", "Unrecognized input: '/*comment'" );
    }

    @Test
    public void unrecognizedInput6() {
        assertLessException( "@{a;", "Unrecognized input: '@{a;'" );
    }

    @Test
    public void unrecognizedInput7() {
        assertLessException( ".a()xyz{}", "Unrecognized input: 'xyz{'" );
    }

    @Test
    public void unknownImportKeyword() {
        assertLessException( "@import (xyz) 'c.data';", "Unknown @import keyword: xyz" );
    }

    @Test
    public void undefinedVariableInSelectorInput() {
        assertLessException( "a@{b}c{a:1;}", "Undefined Variable: @b in a@{b}c" );
    }

    @Test
    public void unexpectedEOF1() {
        assertLessException( "@a:'a", "Unexpected end of Less data" );
    }

    @Test
    public void unexpectedEOF2() {
        assertLessException( "a{b", "Unexpected end of Less data" );
    }

    @Test
    public void propsInRoot() {
        assertLessException( "a: 5;", "Properties must be inside selector blocks, they cannot be in the root." );
    }

    /**
     * test for a JIT error
     * https://github.com/i-net-software/jlessc/issues/20
     */
    @Test
    public void colorConst() {
        for( int i = 0; i < 10000; i++ ) {
            assertEquals( Integer.toString( i ), ".t{c:#d3d3d3}", Less.compile( null, ".t{c:lightgrey}", true ) );
        }
    }
}
