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

import static com.inet.lib.less.ColorUtils.*;

/**
 * Base expression with value formating.
 */
abstract class Expression extends LessObject implements Formattable {

    static final int UNKNOWN = 0;

    static final int BOOLEAN = 1;

    static final int NUMBER  = 2;

    static final int PERCENT = 3;

    static final int RGBA    = 4;

    static final int COLOR   = 5;

    static final int STRING  = 6;

    static final int LIST  = 7;

    // A color alpha value of 1 as long mask.
    static final long ALPHA_1 = 0xFFFF_0000_0000_0000L;

    static final double WHITE = Double.longBitsToDouble( ALPHA_1 | 0xFF00_FF00_FF00L );

    static final double BLACK = Double.longBitsToDouble( ALPHA_1 );

    private String str;

    private boolean important;

    /**
     * Create a new instance.
     * 
     * @param obj
     *            another LessObject with parse position.
     * @param str
     *            a string from the parser
     */
    Expression( LessObject obj, String str ) {
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
    public void appendTo( CssFormatter formatter ) {
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
                if( color == 0 && Double.doubleToRawLongBits( color ) == 0 ) {
                    formatter.append( "transparent" );
                } else {
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
                }
                return;
        }
        formatter.append( str );
    }

    /**
     * The data type of the expression
     * 
     * @param formatter
     *            the CCS target
     * 
     * @return one of the constant
     */
    abstract int getDataType( CssFormatter formatter );

    /**
     * Get the numeric value.
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    abstract double doubleValue( CssFormatter formatter );

    /**
     * Get the boolean value
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    abstract boolean booleanValue( CssFormatter formatter );

    /**
     * Get the string value
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    String stringValue( CssFormatter formatter ) {
        try {
            formatter.addOutput();
            appendTo( formatter );
            return formatter.releaseOutput();
        } catch( Exception ex ) {
            throw createException( ex );
        }
    }

    /**
     * If this expression is mark as important
     * @return true, if important
     */
    boolean isImportant() {
        return important;
    }

    /**
     * Enable the important flag.
     */
    void setImportant() {
        important = true;
    }

    /**
     * Get the value as a list
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    public Operation listValue( CssFormatter formatter ) {
        Expression expr = unpack( formatter );
        if( expr == this ) {
            throw createException( "Exprestion is not a list: " + this );
        }
        return expr.listValue( formatter );
    }

    /**
     * Get the unit of a NUMBER value.
     * 
     * @param formatter
     *            the CCS target
     * @return the unit or empty string if nothing
     */
    abstract String unit( CssFormatter formatter );

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return str;
    }

    /**
     * Unpack this expression to return the core expression
     * @param formatter the CCS target
     * @return the core expression
     */
    Expression unpack( CssFormatter formatter ) {
        Expression unpack = this;
        do { // unpack packed expressions like parenthesis or variables
            if( unpack.getClass() == FunctionExpression.class && ((FunctionExpression)unpack).toString().isEmpty() ) { //Parenthesis
                unpack = ((FunctionExpression)unpack).get( 0 );
                continue;
            }
            if( unpack.getClass() == VariableExpression.class ) {
                unpack = ((VariableExpression)unpack).getValue( formatter );
                continue;
            }
            break;
        } while(true);
        return unpack;
    }
}
