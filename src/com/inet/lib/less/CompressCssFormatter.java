/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 Volker Berlin
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
class CompressCssFormatter extends CssFormatter {

    boolean wasSemicolon;
    
    CompressCssFormatter() {
        getFormat().setMinimumIntegerDigits( 0 );
    }

    @Override
    CssFormatter space() {
        return this;
    }

    @Override
    CssFormatter newline() {
        return this;
    }

    @Override
    CssFormatter insets() {
        return this;
    }

    @Override
    CssFormatter comment( String msg ) {
        checkSemicolon();
        if( msg.startsWith( "/*!" ) ) {
            return super.append( msg );
        }
        return this;
    }

    @Override
    CssFormatter appendColor( double color, String hint ) throws IOException {
        if( !inlineMode() ) {
            int red = ColorUtils.red( color );
            if( red % 17 == 0 ) {
                int green = ColorUtils.green( color );
                if( green % 17 == 0 ) {
                    int blue = ColorUtils.blue( color );
                    if( blue % 17 == 0 ) {
                        append( '#' )
                        .append( Character.forDigit( red / 17, 16 ) )
                        .append( Character.forDigit( green / 17, 16 ) )
                        .append( Character.forDigit( blue / 17, 16 ) );
                        return this;
                    }
                }
            }
        }
        return super.appendColor( color, null );
    }

    @Override
    CssFormatter appendValue( double value, String unit ) {
        if( value == 0 ) {
            switch( unit ) {
                case "deg":
                case "s":
                    break;
                default:
                    return super.append( '0' );
            }
        }
        return super.appendValue( value, unit );
    }

    @Override
    CssFormatter semicolon() {
        wasSemicolon = true;
        return this;
    }

    private void checkSemicolon() {
        if( wasSemicolon ) {
            wasSemicolon = false;
            super.semicolon();
        }
    }

    @Override
    CssFormatter appendSelector( String selector ) {
        checkSemicolon();
        return super.appendSelector( selector );
    }

    @Override
    void appendProperty( String name, Expression value ) throws IOException {
        checkSemicolon();
        super.appendProperty( name, value );
    }

    @Override
    CssFormatter endBlock() {
        wasSemicolon = false;
        return super.endBlock();
    }
}
