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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * A function (less or CSS).
 */
class FunctionExpression extends AbstractExpression implements Expression {

    private final List<Expression> parameters;

    private int                    type;

    private double                 doubleValue;

    private boolean                booleanValue;

    FunctionExpression( LessLookAheadReader reader, String str, Operation parameters ) {
        super( reader, str );
        this.parameters = parameters.getOperands();
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
        eval( formatter );
        return doubleValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean booleanValue( CssFormatter formatter ) {
        eval( formatter );
        return booleanValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unit( CssFormatter formatter ) {
        switch( super.toString() ) {
            case "unit":
                if( parameters.size() == 1 ) {
                    return "";
                } else {
                    return get( 1 ).stringValue( formatter );
                }
        }
        for( int i = 0; i < parameters.size(); i++ ) {
            String unit = parameters.get( i ).unit( formatter );
            if( !unit.isEmpty() ) {
                return unit;
            }
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) throws IOException {
        switch( super.toString() ) {
            //TODO remove the list of CSS functions, wee need to support all
//            case "attr":
//            case "url":
//            case "rotate":
//            case "linear-gradient":
//            case "radial-gradient":
//            case "rect":
//            case "ceil":
//            case "translate":
//                appendToCssFunction( formatter );
//                return;
            case "%":
                format( formatter );
                return;
            case "argb":
                int argb = getInt( 0, formatter );
                formatter.append( '#' );
                String hex = Integer.toHexString( argb );
                for( int i = hex.length(); i < 8; i++ ) {
                    formatter.append( '0' );
                }
                formatter.append( hex );
                return;
        }
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        if( type == STRING ) {
            if( super.toString().equals( "" ) ) { //parenthesis
                get( 0 ).appendTo( formatter );
            } else {
                appendToCssFunction( formatter );
            }
            return;
        }
        super.appendTo( formatter );
    }
    
    /**
     * Write the function without change. We does not know it. It can/must be a CSS function.
     * @param formatter the formatter
     * @throws IOException if any I/= error occur on writting
     */
    private void appendToCssFunction( CssFormatter formatter ) throws IOException {
        formatter.append( super.toString() ).append( '(' );
        for( int i=0; i<parameters.size(); i++ ) {
            if( i>0){
                formatter.append( ',' ).space();
            }
            parameters.get(i).appendTo( formatter );
        }
        formatter.append( ')' );
    }

    private void eval( CssFormatter formatter ) {
        try {
            switch( super.toString() ) {
                case "": //parenthesis
                    type = get( 0 ).getDataType( formatter );
                    if( type != STRING ) {
                        doubleValue = getDouble( 0, formatter );
                    }
                    return;
                case "percentage":
                    type = PERCENT;
                    doubleValue = getDouble( 0, formatter ) * 100;
                    return;
                case "floor":
                    type = NUMBER;
                    doubleValue = Math.floor( getDouble( 0, formatter ) );
                    return;
                case "ceil":
                    type = NUMBER;
                    doubleValue = Math.ceil( getDouble( 0, formatter ) );
                    return;
                case "increment":
                    type = NUMBER;
                    doubleValue = getDouble( 0, formatter ) + 1;
                    return;
                case "add":
                    type = NUMBER;
                    doubleValue = getDouble( 0, formatter ) + getDouble( 1, formatter );
                    return;
                case "length":
                    type = NUMBER;
                    doubleValue = getParamList( formatter ).size();
                    return;
                case "extract":
                    List<Expression> exList = getParamList( formatter );
                    int idx = getInt( 1, formatter );
                    if( idx <= 0 || exList.size() < idx ) {
                        type = STRING;
                        return;
                    }
                    Expression ex = exList.get( idx - 1 );
                    type = ex.getDataType( formatter );
                    if( type != STRING ) {
                        doubleValue = ex.doubleValue( formatter );
                    }
                    return;
                case "alpha":
                    type = NUMBER;
                    switch( get( 0 ).getDataType( formatter ) ) {
                        case RGBA:
                            doubleValue = alpha( getInt( 0, formatter ) );
                            break;
                        case COLOR:
                            doubleValue = 1;
                            break;
                        default:
                            type = STRING;
                    }
                    return;
                case "red":
                    type = NUMBER;
                    doubleValue = red( getInt( 0, formatter ) );
                    return;
                case "green":
                    type = NUMBER;
                    doubleValue = green( getInt( 0, formatter ) );
                    return;
                case "blue":
                    type = NUMBER;
                    doubleValue = blue( getInt( 0, formatter ) );
                    return;
                case "rgba":
                    type = RGBA;
                    int r = getColorDigit( 0, formatter );
                    int g = getColorDigit( 1, formatter );
                    int b = getColorDigit( 2, formatter );
                    double a = getPercent( 3, formatter );
                    doubleValue = rgba( r, g, b, a );
                    return;
                case "rgb":
                    type = COLOR;
                    r = getColorDigit( 0, formatter );
                    g = getColorDigit( 1, formatter );
                    b = getColorDigit( 2, formatter );
                    doubleValue = rgb( r, g, b );
                    return;
                case "argb":
                    type = STRING;
                    return;
                case "saturate":
                    type = COLOR;
                    HSL hsl = toHSL( getInt( 0, formatter ) );
                    hsl.s += getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "desaturate":
                    type = COLOR;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.s -= getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "greyscale":
                    type = COLOR;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.s = 0;
                    doubleValue = hsla( hsl );
                    return;
                case "saturation":
                    type = PERCENT;
                    hsl = toHSL( getInt( 0, formatter ) );
                    doubleValue = hsl.s * 100;
                    return;
                case "hsl":
                    type = COLOR;
                    doubleValue = hsla( getDouble( 0, formatter ), getPercent( 1, formatter ), getPercent( 2, formatter ), 0 );
                    return;
                case "hsla":
                    type = RGBA;
                    doubleValue = hsla( getDouble( 0, formatter ), getPercent( 1, formatter ), getPercent( 2, formatter ), getPercent( 3, formatter ) );
                    return;
                case "hue":
                    type = NUMBER;
                    hsl = toHSL( getInt( 0, formatter ) );
                    doubleValue = hsl.h;
                    return;
                case "lightness":
                    type = PERCENT;
                    hsl = toHSL( getInt( 0, formatter ) );
                    doubleValue = hsl.l * 100;
                    return;
                case "spin":
                    type = COLOR;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.h += getDouble( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "lighten":
                    type = COLOR;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.l += getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "darken":
                    type = COLOR;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.l -= getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "fadein":
                    type = RGBA;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.a += getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "fadeout":
                    type = RGBA;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.a -= getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "fade":
                    type = RGBA;
                    hsl = toHSL( getInt( 0, formatter ) );
                    hsl.a = getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "contrast":
                    type = COLOR;
                    int rgb = getInt( 0, formatter );
                    int dark = getInt( 1, 0, formatter );
                    int light = getInt( 2, -1, formatter );
                    double threshold = getDouble( 2, 0.43, formatter );
                    doubleValue = contrast( rgb, dark, light, threshold );
                    return;
                case "unit":
                    type = NUMBER;
                    doubleValue = getDouble( 0, formatter );
                    return;
                case "isnumber":
                    type = BOOLEAN;
                    booleanValue = get( 0 ).getDataType( formatter ) == NUMBER;
                    return;
                case "-":
                    type = get( 0 ).getDataType( formatter ) ;
                    doubleValue = -getDouble( 0, formatter );
                    return;
                //TODO remove the list of CSS functions, wee need to support all
                case "%":
//                case "url": //CSS functions
//                case "local":
//                case "format":
//                case "rotate":
//                case "radial-gradient":
//                case "linear-gradient":
//                case "rect":
                    type = STRING;
                    return;
            }
            if( super.toString().startsWith( "-" ) ) {
                type = STRING;
                return;
            }
        } catch( ParameterOutOfBoundsException ex ) {
            // ignore and continue as CSS function
        } catch (RuntimeException ex ) {
            throw createException( ex );
        }
        //TODO remove exception, wee need to support all CSS functions
//        throw createException( "Unknown function: " + super.toString() );
        type = STRING;
        return;
    }

    private void format( CssFormatter formatter ) throws IOException {
        String fmt = get( 0 ).stringValue( formatter );
        int idx = 1;
        for( int i = 0; i < fmt.length(); i++ ) {
            char ch = fmt.charAt( i );
            if( ch == '%' ) {
                ch = fmt.charAt( ++i );
                switch( ch ){
                    case '%':
                        formatter.append( ch );
                        break;
                    case 'a':
                    case 'd':
                        get( idx++ ).appendTo( formatter );
                        break;
                    case 'A':
                    case 'D':
                        String str = get( idx++ ).stringValue( formatter );
                        try {
                            str = new URI( null, null, str, null ).getRawPath();
                        } catch( URISyntaxException e ) {
                            e.printStackTrace();
                        }
                        formatter.append( str );
                        break;
                    case 's':
                        formatter.setInineMode( true );
                        get( idx++ ).appendTo( formatter );
                        formatter.setInineMode( false );
                        break;
                    case 'S':
                        formatter.setInineMode( true );
                        str = get( idx++ ).stringValue( formatter );
                        try {
                            str = new URI( null, null, str, null ).getRawPath();
                        } catch( URISyntaxException e ) {
                            e.printStackTrace();
                        }
                        formatter.append( str );
                        formatter.setInineMode( false );
                        break;
                    default:
                        formatter.append( ch );
                }
            } else {
                formatter.append( ch );
            }
        }
    }

    /**
     * Get the idx parameter from the parameter list.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private Expression get( int idx ) {
        if( parameters.size() <= idx ) {
            throw new ParameterOutOfBoundsException();
        }
        return parameters.get( idx );
    }

    /**
     * Get the idx parameter from the parameter list as color digit.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private int getColorDigit( int idx, CssFormatter formatter ) {
        Expression expression = get( idx );
        double d = expression.doubleValue( formatter );
        if( expression.getDataType( formatter ) == PERCENT ) {
            d *= 2.55;
        }
        return colorDigit(d);
    }

    /**
     * Get the idx parameter from the parameter list as integer.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private int getInt( int idx, CssFormatter formatter ) {
        return (int)get( idx ).doubleValue( formatter );
    }

    /**
     * Get the idx parameter from the parameter list.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private int getInt( int idx, int defaultValue, CssFormatter formatter ) {
        if( parameters.size() <= idx ) {
            return defaultValue;
        }
        return (int)parameters.get( idx ).doubleValue( formatter );
    }

    /**
     * Get the idx parameter from the parameter list as percent (range 0 - 1).
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private double getPercent( int idx, CssFormatter formatter ) {
        Expression expression = get( idx );
        double d = expression.doubleValue( formatter );
        if( expression.getDataType( formatter ) == PERCENT ) {
            d /= 100;
        }
        return d;
    }

    /**
     * Get the idx parameter from the parameter list as double.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private double getDouble( int idx, CssFormatter formatter ) {
        return get( idx ).doubleValue( formatter );
    }

    /**
     * Get the idx parameter from the parameter list.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     */
    private double getDouble( int idx, double defaultValue, CssFormatter formatter ) {
        if( parameters.size() <= idx ) {
            return defaultValue;
        }
        return parameters.get( idx ).doubleValue( formatter );
    }

    private List<Expression> getParamList( CssFormatter formatter ) {
        Expression ex0 = get( 0 );
        if( ex0.getClass() == VariableExpression.class ) {
            ex0 = ((VariableExpression)ex0).getValue( formatter );
            if( ex0.getClass() == Operation.class ) {
                return ((Operation)ex0).getOperands();
            }
        }
        List<Expression> result = new ArrayList<Expression>();
        result.add( ex0 );
        return result;
    }
}
