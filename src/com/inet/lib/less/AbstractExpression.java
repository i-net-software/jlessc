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

import static com.inet.lib.less.ColorUtils.*;

import java.io.IOException;

/**
 * Base expression with value formating.
 */
abstract class AbstractExpression extends LessObject implements Expression {

    private String str;

    /**
     * Create a new instance.
     * 
     * @param obj
     *            another LessObject with parse position.
     * @param str
     *            a string from the parser
     */
    AbstractExpression( LessObject obj, String str ) {
        super( obj );
        this.str = str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getType() {
        return EXPRESSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) throws IOException {
        switch( getDataType( formatter ) ) {
            case BOOLEAN:
                formatter.append( Boolean.toString( booleanValue( formatter ) ) );
                return;
            case PERCENT:
                double d = doubleValue( formatter );
                formatter.append( d );
                formatter.append( '%' );
                return;
            case NUMBER:
                d = doubleValue( formatter );
                formatter.appendValue( d, unit( formatter ) );
                return;
            case COLOR:
                formatter.appendColor( doubleValue( formatter ), null );
                return;
            case RGBA:
                double color = doubleValue( formatter );
                final double alpha = alpha( color );
                if( alpha >= 1 ) {
                    formatter.appendColor( color, null );
                } else {
                    formatter.append( "rgba(" );
                    formatter.append( red( color ) ).append( ',' ).space();
                    formatter.append( green( color ) ).append( ',' ).space();
                    formatter.append( blue( color ) ).append( ',' ).space();
                    formatter.append( alpha ).append( ')' );
                }
                return;
        }
        formatter.append( str );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String stringValue( CssFormatter formatter ) {
        try {
            StringBuilder builder = new StringBuilder();
            Appendable output = formatter.swapOutput( builder );
            appendTo( formatter );
            formatter.swapOutput( output );
            return builder.toString();
        } catch( IOException ex ) {
            throw createException( ex );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return str;
    }
}
