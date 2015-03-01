/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 - 2015 Volker Berlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * UT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author Volker Berlin
 * @license: The MIT license <http://opensource.org/licenses/MIT>
 */
package com.inet.lib.less;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A formatter for the CSS output. Hold some formating states.
 */
class PlainCssFormatter {

    private final static char[]                          DIGITS         = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private StringBuilder                                insets         = new StringBuilder();

    private boolean                                      important;

    private boolean                                      inlineMode;

    private final DecimalFormat                          decFormat      = new DecimalFormat( "#.########", DecimalFormatSymbols.getInstance( Locale.ENGLISH ) );


    void clean() {
        insets.setLength( 0 );
        inlineMode = false;
    }

    void setInineMode( boolean mode ) {
        inlineMode = mode;
    }

    boolean inlineMode() {
        return inlineMode;
    }

    void append( Appendable output, String str ) throws IOException {
        if( inlineMode ) {
            str = UrlUtils.removeQuote( str );
        }
        output.append( str );
    }

    void appendColor( StringBuilder output, double color, String hint ) throws IOException {
        if( !inlineMode && hint != null ) {
            output.append( hint );
        } else {
            int argb = ColorUtils.argb( color );
            output.append( '#' );
            appendHex( output, argb, 6 );
        }
    }

    void appendHex( StringBuilder output, int value, int digits ) throws IOException {
        if( digits > 1 ) {
            appendHex( output, value >>> 4, digits-1 );
        }
        output.append( DIGITS[ value & 0xF ] );
    }

    void append( StringBuilder output, double value ) {
        if( value == (int)value ) {
            output.append( Integer.toString( (int)value ) );
        } else {
            output.append( decFormat.format( value ) );
        }
    }

    void appendValue( StringBuilder output, double value, String unit ) throws IOException {
        append( output, value );
        append( output, unit );
    }

    void incInsets() {
        insets.append( "  " );
    }

    void decInsets() {
        insets.setLength( insets.length() - 2 );
    }

    /**
     * Start a new block with a list of selectors.
     * @param selectors the selectors
     * @throws IOException 
     */
    void startBlock( Appendable output, String[] selectors ) throws IOException {
        for( int i=0; i<selectors.length; i++ ) {
            if( i > 0 ) {
                output.append( ',' );
                newline( output );
            }
            insets( output );
            append( output, selectors[i] );
        }
        space( output );
        output.append( '{' );
        newline( output );
        incInsets();
    }

    void endBlock( Appendable output ) throws IOException {
        decInsets();
        insets( output );
        output.append( '}' );
        newline( output );
    }

    void appendProperty( StringBuilder output, CssFormatter formatter, String name, Expression value ) throws IOException {
        insets( output );
        SelectorUtils.appendToWithPlaceHolder( formatter, name, 0, (LessObject)value );
        output.append( ':' );
        space( output );
        value.appendTo( formatter );
        if( important ) {
            output.append( " !important" );
        }
        semicolon( output );
        newline( output );
    }

    void setImportant( boolean important ) {
        this.important = important;
    }

    void space( Appendable output ) throws IOException {
        output.append( ' ' );
    }

    void newline( Appendable output ) throws IOException {
        output.append( '\n' );
    }

    void semicolon( Appendable output ) throws IOException {
        output.append( ';' );
    }

    void insets( Appendable output ) throws IOException {
        output.append( insets );
    }

    void comment( StringBuilder output, String msg ) throws IOException {
        output.append( insets ).append( msg ).append( '\n' );
    }

    /**
     * Get a shared decimal format for parsing numbers with units.
     * 
     * @return the format
     */
    DecimalFormat getFormat() {
        return decFormat;
    }
}
