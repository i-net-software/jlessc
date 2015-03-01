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

/**
 * A version of the CssFormatter that produce a compressed output.
 */
class CompressCssFormatter extends PlainCssFormatter {

    boolean wasSemicolon;

    /**
     * {@inheritDoc}
     */
    void clean() {
        super.clean();
        wasSemicolon = false;
    }

    /**
     * Create an instance.
     */
    CompressCssFormatter() {
        getFormat().setMinimumIntegerDigits( 0 );
    }

    /**
     * Do nothing.
     * {@inheritDoc}
     */
    @Override
    void space( Appendable output ) {
    }

    /**
     * Do nothing.
     * {@inheritDoc}
     */
    @Override
    void newline( Appendable output ) {
    }

    /**
     * Do nothing.
     * {@inheritDoc}
     */
    @Override
    void insets( Appendable output ) {
    }

    /**
     * {@inheritDoc}
     * @throws IOException 
     */
    @Override
    void comment( StringBuilder output, String msg ) throws IOException {
        if( msg.startsWith( "/*!" ) ) {
            checkSemicolon( output );
            super.append( output, msg );
        }
    }

    /**
     * Write a 3 digit color definition if possible.
     * {@inheritDoc}
     */
    @Override
    void appendColor( StringBuilder output, double color, String hint ) throws IOException {
        if( !inlineMode() ) {
            int red = ColorUtils.red( color );
            if( red % 17 == 0 ) {
                int green = ColorUtils.green( color );
                if( green % 17 == 0 ) {
                    int blue = ColorUtils.blue( color );
                    if( blue % 17 == 0 ) {
                        output.append( '#' )
                        .append( Character.forDigit( red / 17, 16 ) )
                        .append( Character.forDigit( green / 17, 16 ) )
                        .append( Character.forDigit( blue / 17, 16 ) );
                        return;
                    }
                }
            }
        }
        super.appendColor( output, color, null );
    }

    /**
     * Remove units if value is zero.
     * {@inheritDoc}
     * @throws IOException 
     */
    @Override
    void appendValue( StringBuilder output, double value, String unit ) throws IOException {
        if( value == 0 ) {
            switch( unit ) {
                case "deg":
                case "s":
                    break;
                default:
                    output.append( '0' );
                    return;
            }
        }
        super.appendValue( output, value, unit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void semicolon( Appendable output ) {
        wasSemicolon = true;
    }

    private void checkSemicolon( Appendable output ) throws IOException {
        if( wasSemicolon ) {
            wasSemicolon = false;
            super.semicolon( output );
        }
    }

    /**
     * {@inheritDoc}
     * @throws IOException 
     */
    @Override
    void startBlock( Appendable output, String[] selectors ) throws IOException {
        checkSemicolon( output );
        super.startBlock( output, selectors );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void appendProperty( StringBuilder output, CssFormatter formatter, String name, Expression value ) throws IOException {
        checkSemicolon( output );
        super.appendProperty( output, formatter, name, value );
    }

    /**
     * {@inheritDoc}
     * @throws IOException 
     */
    @Override
    void endBlock( Appendable output ) throws IOException {
        wasSemicolon = false;
        super.endBlock( output );
    }
}
