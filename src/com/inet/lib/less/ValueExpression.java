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
import java.text.ParsePosition;

/**
 * A constant value.
 */
class ValueExpression extends AbstractExpression {

    private int    type;

    private double value;

    private String unit;

    ValueExpression( LessObject reader, String str ) {
        super( reader, str );
    }

    public static ValueExpression eval( CssFormatter formatter, Expression expr ) {
        if( expr instanceof ValueExpression ) {
            return (ValueExpression)expr;
        }
        ValueExpression valueEx = new ValueExpression( (LessObject)expr, expr.stringValue( formatter ) );
        valueEx.type = expr.getDataType( formatter );
        valueEx.unit = expr.unit( formatter );
        switch( valueEx.type ) {
            case STRING:
            case BOOLEAN:
                break; //string is already set
            default:
                valueEx.value = expr.doubleValue( formatter );
        }
        return valueEx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) throws IOException {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        switch( type ) {
            case COLOR:
                formatter.appendColor( (int)value, toString() );
                break;
            case STRING:
                SelectorUtils.appendToWithPlaceHolder( formatter, toString(), 0, this );
                break;
            default:
                super.appendTo( formatter );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDataType( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        if( type == STRING ) {
            throw createException( "Invalid Number: " + toString() );
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unit( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        return unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean booleanValue( CssFormatter formatter ) {
        return Boolean.parseBoolean( toString() );
    }

    /**
     * Evaluate the type and value.
     */
    private void eval( CssFormatter formatter ) {
        try {
            unit = "";
            String str = toString();
            if( str.endsWith( "%" ) ) {
                value = Double.parseDouble( str.substring( 0, str.length() - 1 ) );
                type = PERCENT;
                return;
            }
            if( str.startsWith( "#" ) ) {
                str = str.substring( 1 );
                switch( str.length() ) {
                    case 3:
                        value = 0;
                        for( int i = 0; i < 3; i++ ) {
                            char ch = str.charAt( i );
                            int digit = Character.digit( ch, 16 );
                            if( digit < 0 ) {
                                type = STRING;
                                return;
                            }
                            value *= 256;
                            value += digit * 17;
                        }
                        type = COLOR;
                        return;
                    case 6:
                        value = Integer.parseInt( str, 16 );
                        type = COLOR;
                        return;
                }
                throw createException( "Invalid Color: " + toString() );
            }
            switch( str ) {
                case "blue":
                    value = 0xFF;
                    break;
                case "red":
                    value = 0xFF0000;
                    break;
                case "black":
                    value = 0x00;
                    break;
                case "orange":
                    value = 0xffa500;
                    break;
                case "white":
                    value = 0xFFFFFF;
                    break;
                default:
                    ParsePosition pos = new ParsePosition( 0 );
                    Number number = formatter.getFormat().parse( str, pos );
                    if( number == null ) {
                        type = STRING;
                        return;
                    }
                    value = number.doubleValue();
                    if( pos.getIndex() != str.length() ) {
                        unit = str.substring( pos.getIndex() );
                    }
                    type = NUMBER;
                    return;
            }
            type = COLOR;
        } catch( NumberFormatException e ) {
            type = STRING;
        }
    }
}
