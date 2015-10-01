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


/**
 * A version of the CssFormatter that produce a compressed output.
 */
class CompressCssFormatter extends CssFormatter {

    private boolean wasSemicolon;

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
    CssFormatter space() {
        return this;
    }

    /**
     * Do nothing.
     * {@inheritDoc}
     */
    @Override
    CssFormatter newline() {
        return this;
    }

    /**
     * Do nothing.
     * {@inheritDoc}
     */
    @Override
    void insets() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    CssFormatter comment( String msg ) {
        if( msg.startsWith( "/*!" ) ) {
            checkSemicolon();
            getOutput().append( msg );
        }
        return this;
    }

    /**
     * Write a 3 digit color definition if possible.
     * {@inheritDoc}
     */
    @Override
    CssFormatter appendColor( double color, String hint ) {
        if( !inlineMode() ) {
            int red = ColorUtils.red( color );
            if( red % 17 == 0 ) {
                int green = ColorUtils.green( color );
                if( green % 17 == 0 ) {
                    int blue = ColorUtils.blue( color );
                    if( blue % 17 == 0 ) {
                        super.append( '#' )
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

    /**
     * Remove units if value is zero.
     * {@inheritDoc}
     */
    @Override
    CssFormatter appendValue( double value, String unit ) {
        if( value == 0 ) {
            switch( unit ) {
                case "deg":
                case "s":
                    break;
                default:
                    super.append( '0' );
                    return this;
            }
        }
        return super.appendValue( value, unit );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void semicolon() {
        wasSemicolon = true;
    }

    /**
     * Check is a semicolon should be write.
     */
    private void checkSemicolon() {
        if( wasSemicolon ) {
            wasSemicolon = false;
            super.semicolon();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    CssFormatter startBlock( String[] selectors ) {
        checkSemicolon();
        return super.startBlock( selectors );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void appendProperty( String name, Expression value ) {
        checkSemicolon();
        super.appendProperty( name, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    CssFormatter endBlock() {
        wasSemicolon = false;
        return super.endBlock();
    }
}
