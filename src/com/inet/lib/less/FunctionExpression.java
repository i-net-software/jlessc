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

import static com.inet.lib.less.ColorUtils.alpha;
import static com.inet.lib.less.ColorUtils.argb;
import static com.inet.lib.less.ColorUtils.average;
import static com.inet.lib.less.ColorUtils.blue;
import static com.inet.lib.less.ColorUtils.colorDigit;
import static com.inet.lib.less.ColorUtils.contrast;
import static com.inet.lib.less.ColorUtils.difference;
import static com.inet.lib.less.ColorUtils.exclusion;
import static com.inet.lib.less.ColorUtils.green;
import static com.inet.lib.less.ColorUtils.hardlight;
import static com.inet.lib.less.ColorUtils.hsla;
import static com.inet.lib.less.ColorUtils.hsva;
import static com.inet.lib.less.ColorUtils.luma;
import static com.inet.lib.less.ColorUtils.luminance;
import static com.inet.lib.less.ColorUtils.mix;
import static com.inet.lib.less.ColorUtils.multiply;
import static com.inet.lib.less.ColorUtils.negation;
import static com.inet.lib.less.ColorUtils.overlay;
import static com.inet.lib.less.ColorUtils.red;
import static com.inet.lib.less.ColorUtils.rgb;
import static com.inet.lib.less.ColorUtils.rgba;
import static com.inet.lib.less.ColorUtils.screen;
import static com.inet.lib.less.ColorUtils.softlight;
import static com.inet.lib.less.ColorUtils.toHSL;
import static com.inet.lib.less.ColorUtils.toHSV;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * A function (less or CSS).
 */
class FunctionExpression extends Expression {

    private final List<Expression> parameters;

    private int                    type;

    private double                 doubleValue;

    private boolean                booleanValue;

    /**
     * Create a new instance.
     * 
     * @param obj
     *            another LessObject with parse position.
     * @param name
     *            the name of the method, if empty then it are parenthesis
     * @param parameters
     *            the parameters of the function
     */
    FunctionExpression( LessObject obj, @Nonnull String name, @Nonnull Operation parameters ) {
        super( obj, name );
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
                if( parameters.size() <= 1 ) {
                    return "";
                } else {
                    return get( 1 ).stringValue( formatter );
                }
            case "convert":
                return get( 1 ).stringValue( formatter );
            case "sin":
            case "cos":
            case "tan":
            case "length":
                return "";
            case "acos":
            case "asin":
            case "atan":
                return "rad";
            case "pow":
                return get( 0 ).unit( formatter );
            case "alpha":
            case "red":
            case "green":
            case "blue":
            case "rgba":
            case "rgb":
            case "argb":
            case "saturate":
            case "desaturate":
            case "greyscale":
            case "hsl":
            case "hsla":
            case "hue":
            case "spin":
            case "lighten":
            case "darken":
            case "fadein":
            case "fadeout":
            case "fade":
            case "hsv":
            case "hsva":
            case "hsvhue":
            case "contrast":
                // color values has no unit
                return "";
            case "saturation":
            case "lightness":
            case "hsvsaturation":
            case "hsvvalue":
            case "luma":
            case "luminance":
                return "%";
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
    public void appendTo( CssFormatter formatter ) {
        try {
            switch( super.toString() ) {
                case "%":
                    format( formatter );
                    return;
                case "escape":
                    escape( formatter );
                    return;
                case "argb":
                    double color = getDouble( 0, formatter );
                    int argb = argb( color );
                    formatter.append( '#' );
                    formatter.appendHex( argb, 8 );
                    return;
                case "svg-gradient":
                    UrlUtils.svgGradient( formatter, parameters );
                    return;
                case "colorize-image":
                    CustomFunctions.colorizeImage( formatter, parameters );
                    return;
                case "replace":
                    String str = get( 0 ).stringValue( formatter );
                    formatter.setInlineMode( true );
                    String pattern = get( 1 ).stringValue( formatter );
                    String replacement = get( 2 ).stringValue( formatter );
                    String flags = parameters.size() > 3 ? get( 3 ).stringValue( formatter ) : "";
                    formatter.setInlineMode( false );
                    if( str.length() > 1 ) {
                        char ch = str.charAt( 0 );
                        boolean quote = false;
                        if( ch == '\'' || ch == '\"' ) {
                            if( str.charAt( str.length() - 1 ) == ch ) {
                                str = str.substring( 1, str.length() - 1 );
                                quote = true;
                            }
                        }
                        str = new RegExp( pattern, flags ).replace( str, replacement );
                        if( quote ) {
                            str = ch + str + ch;
                        }
                    }
                    formatter.append( str );
                    return;
                case "get-unit":
                    formatter.append( unit( formatter ) );
                    return;
                case "url":
                    String url = get( 1 ).stringValue( formatter );
    //                char quoteChar = 0;
    //                boolean quote = false;
    //                if( url.length() >= 2 ) {
    //                    quoteChar = url.charAt( 0 );
    //                    if( quoteChar == '\'' || quoteChar == '\"' ) {
    //                        if( url.charAt( url.length() - 1 ) == quoteChar ) {
    //                            url = url.substring( 1, url.length() - 1 );
    //                            quote = true;
    //                        }
    //                    }
    //                }
    //                if( url.startsWith( "../" ) ) {
    //                    String baseUrl = get( 0 ).stringValue( formatter );
    //                    baseUrl = baseUrl.substring( 0, baseUrl.lastIndexOf( '/' ) + 1 );
    //                    boolean append = false;
    //                    do {
    //                        if( baseUrl.length() > 0 ) {
    //                            url = url.substring( 3 );
    //                            baseUrl = baseUrl.substring( 0, baseUrl.lastIndexOf( '/', baseUrl.length() - 2 ) + 1 );
    //                            append = true;
    //                        } else {
    //                            break;
    //                        }
    //                    } while( url.startsWith( "../" ) );
    //                    if( append ) {
    //                        url = baseUrl + url;
    //                    }
    //                }
                    formatter.append( "url(" );
    //                if( quote ) {
    //                    formatter.append( quoteChar );
    //                }
                    formatter.append( url );
    //                if( quote ) {
    //                    formatter.append( quoteChar );
    //                }
                    formatter.append( ")" );
                    return;
                case "data-uri":
                    String baseUrl = get( 0 ).stringValue( formatter );
                    String type;
                    if( parameters.size() >= 3 ) {
                        type = get( 1 ).stringValue( formatter );
                        url = get( 2 ).stringValue( formatter );
                    } else {
                        type = null;
                        url = get( 1 ).stringValue( formatter );
                    }
                    UrlUtils.dataUri( formatter, baseUrl, url, type );
                    return;
                case "extract":
                    Expression expr = extract( formatter );
                    if( expr != null ) {
                        expr.appendTo( formatter );
                        return;
                    }
                    break;
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
        } catch ( Throwable th ) {
            throw createException( th );
        }
    }

    /**
     * Write the function without change. We does not know it. It can/must be a CSS function.
     * @param formatter the formatter
     */
    private void appendToCssFunction( CssFormatter formatter ) {
        formatter.append( super.toString() ).append( '(' );
        for( int i=0; i<parameters.size(); i++ ) {
            if( i>0){
                formatter.append( ',' ).space();
            }
            parameters.get(i).appendTo( formatter );
        }
        formatter.append( ')' );
    }

    /**
     * Evaluate this function.
     * 
     * @param formatter the current formation context
     */
    private void eval( CssFormatter formatter ) {
        try {
            switch( super.toString().toLowerCase() ) {
                case "": //parenthesis
                    if( parameters.size() > 1 ) {
                        throw ((LessObject)get( 0 )).createException( "Unrecognised input" );
                    }
                    type = get( 0 ).getDataType( formatter );
                    if( type != STRING ) {
                        doubleValue = getDouble( 0, formatter );
                    }
                    return;
                case "percentage":
                    type = PERCENT;
                    doubleValue = getDouble( 0, formatter ) * 100;
                    return;
                case "convert":
                    type = NUMBER;
                    String unit = get( 1 ).stringValue( formatter );
                    Expression param = get( 0 );
                    doubleValue = param.doubleValue( formatter ) * Operation.unitFactor( param.unit( formatter ), unit, false );
                    return;
                case "abs":
                    type = getNumberDataType( formatter );
                    doubleValue = Math.abs( getDouble( 0, formatter ) );
                    return;
                case "ceil":
                    type = getNumberDataType( formatter );
                    doubleValue = Math.ceil( getDouble( 0, formatter ) );
                    return;
                case "floor":
                    type = getNumberDataType( formatter );
                    doubleValue = Math.floor( getDouble( 0, formatter ) );
                    return;
                case "mod":
                    type = NUMBER;
                    doubleValue = getDouble( 0, formatter ) % getDouble( 1, formatter );
                    return;
                case "pi":
                    type = NUMBER;
                    doubleValue = Math.PI;
                    return;
                case "round":
                    type = getNumberDataType( formatter );
                    int decimalPlaces = getInt( 1, 0, formatter );
                    doubleValue = getDouble( 0, formatter );
                    for( int i = 0; i < decimalPlaces; i++ ) {
                        doubleValue *= 10;
                    }
                    doubleValue = Math.round( doubleValue );
                    for( int i = 0; i < decimalPlaces; i++ ) {
                        doubleValue /= 10;
                    }
                    return;
                case "min":
                    type = NUMBER;
                    doubleValue = get( 0 ).doubleValue( formatter );
                    unit = unit( formatter );
                    for( int i = 1; i < parameters.size(); i++ ) {
                        param = parameters.get( i );
                        doubleValue = Math.min( doubleValue, param.doubleValue( formatter ) / Operation.unitFactor( unit, param.unit( formatter ), true ) );
                    }
                    return;
                case "max":
                    type = NUMBER;
                    doubleValue = get( 0 ).doubleValue( formatter );
                    unit = unit( formatter );
                    for( int i = 1; i < parameters.size(); i++ ) {
                        param = parameters.get( i );
                        doubleValue = Math.max( doubleValue, param.doubleValue( formatter ) / Operation.unitFactor( unit, param.unit( formatter ), true ) );
                    }
                    return;
                case "sqrt":
                    type = NUMBER;
                    doubleValue = Math.sqrt( getDouble( 0, formatter ) );
                    return;
                case "pow":
                    type = NUMBER;
                    doubleValue = Math.pow( getDouble( 0, formatter ), getDouble( 1, formatter ) );
                    return;
                case "sin":
                    type = NUMBER;
                    doubleValue = Math.sin( getRadians( formatter ) );
                    return;
                case "cos":
                    type = NUMBER;
                    doubleValue = Math.cos( getRadians( formatter ) );
                    return;
                case "tan":
                    type = NUMBER;
                    doubleValue = Math.tan( getRadians( formatter ) );
                    return;
                case "acos":
                    type = NUMBER;
                    doubleValue = Math.acos( getRadians( formatter ) );
                    return;
                case "asin":
                    type = NUMBER;
                    doubleValue = Math.asin( getRadians( formatter ) );
                    return;
                case "atan":
                    type = NUMBER;
                    doubleValue = Math.atan( getRadians( formatter ) );
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
                    extract( formatter );
                    return;
                case "alpha":
                    type = NUMBER;
                    switch( get( 0 ).getDataType( formatter ) ) {
                        case RGBA:
                            doubleValue = alpha( getDouble( 0, formatter ) );
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
                    doubleValue = red( getDouble( 0, formatter ) );
                    return;
                case "green":
                    type = NUMBER;
                    doubleValue = green( getDouble( 0, formatter ) );
                    return;
                case "blue":
                    type = NUMBER;
                    doubleValue = blue( getDouble( 0, formatter ) );
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
                case "color":
                    param = get( 0 );
                    String str = UrlUtils.removeQuote( param.stringValue( formatter ) );
                    doubleValue = getColor( new ValueExpression( param, str ), formatter );
                    return;
                case "argb":
                    type = STRING;
                    return;
                case "saturate":
                    type = COLOR;
                    HSL hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.s += getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "desaturate":
                    type = COLOR;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.s -= getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "greyscale":
                    type = COLOR;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.s = 0;
                    doubleValue = hsla( hsl );
                    return;
                case "mix":
                    double c1 = getColor( 0, formatter );
                    double c2 = getColor( 1, formatter );
                    double weight = getPercent( 2, 0.5, formatter );
                    doubleValue = mix( c1, c2, weight );
                    return;
                case "tint":
                    c1 = getColor( 0, formatter );
                    weight = getPercent( 1, 0.5, formatter );
                    doubleValue = mix( WHITE, c1, weight );
                    return;
                case "shade":
                    c1 = getColor( 0, formatter );
                    weight = getPercent( 1, 0.5, formatter );
                    doubleValue = mix( BLACK, c1, weight );
                    return;
                case "saturation":
                    type = PERCENT;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    doubleValue = hsl.s * 100;
                    return;
                case "hsl":
                    type = COLOR;
                    doubleValue = hsla( getDouble( 0, formatter ), getPercent( 1, formatter ), getPercent( 2, formatter ), 1 );
                    return;
                case "hsla":
                    type = RGBA;
                    doubleValue = hsla( getDouble( 0, formatter ), getPercent( 1, formatter ), getPercent( 2, formatter ), getPercent( 3, formatter ) );
                    return;
                case "hue":
                    type = NUMBER;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    doubleValue = hsl.h;
                    return;
                case "lightness":
                    type = PERCENT;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    doubleValue = hsl.l * 100;
                    return;
                case "spin":
                    type = COLOR;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.h += getDouble( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "lighten":
                    hsl = toHSL( getColor( 0, formatter ) );
                    hsl.l += getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "darken":
                    hsl = toHSL( getColor( 0, formatter ) );
                    hsl.l -= getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "fadein":
                    type = RGBA;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.a += getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "fadeout":
                    type = RGBA;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.a -= getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "fade":
                    type = RGBA;
                    hsl = toHSL( getDouble( 0, formatter ) );
                    hsl.a = getPercent( 1, formatter );
                    doubleValue = hsla( hsl );
                    return;
                case "hsv":
                    type = COLOR;
                    doubleValue = hsva( getDouble( 0, formatter ), getPercent( 1, formatter ), getPercent( 2, formatter ), 1 );
                    return;
                case "hsva":
                    type = RGBA;
                    doubleValue = hsva( getDouble( 0, formatter ), getPercent( 1, formatter ), getPercent( 2, formatter ), getPercent( 3, formatter ) );
                    return;
                case "hsvhue":
                    doubleValue = toHSV( getColor( 0, formatter ) ).h;
                    type = NUMBER;
                    return;
                case "hsvsaturation":
                    doubleValue = toHSV( getColor( 0, formatter ) ).s * 100;
                    type = PERCENT;
                    return;
                case "hsvvalue":
                    doubleValue = toHSV( getColor( 0, formatter ) ).v * 100;
                    type = PERCENT;
                    return;
                case "contrast":
                    double color = getColor( 0, formatter );
                    double dark = getDouble( 1, BLACK, formatter );
                    double light = getDouble( 2, WHITE, formatter );
                    double threshold = getPercent( 3, 0.43, formatter );
                    doubleValue = contrast( color, dark, light, threshold );
                    return;
                case "luma":
                    color = getColor( 0, formatter );
                    type = PERCENT;
                    doubleValue = luma( color ) * 100;
                    return;
                case "luminance":
                    color = getColor( 0, formatter );
                    type = PERCENT;
                    doubleValue = luminance( color ) * 100;
                    return;
                case "multiply":
                    doubleValue = multiply( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "screen":
                    doubleValue = screen( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "overlay":
                    doubleValue = overlay( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "softlight":
                    doubleValue = softlight( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "hardlight":
                    doubleValue = hardlight( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "difference":
                    doubleValue = difference( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "exclusion":
                    doubleValue = exclusion( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "average":
                    doubleValue = average( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "negation":
                    doubleValue = negation( getColor( 0, formatter ), getColor( 1, formatter ) );
                    return;
                case "unit":
                    type = NUMBER;
                    doubleValue = getDouble( 0, formatter );
                    return;
                case "iscolor":
                    type = BOOLEAN;
                    int type0 = get( 0 ).getDataType( formatter );
                    booleanValue = type0 == COLOR || type0 == RGBA;
                    return;
                case "isnumber":
                    type = BOOLEAN;
                    booleanValue = get( 0 ).getDataType( formatter ) == NUMBER;
                    return;
                case "isstring":
                    type = BOOLEAN;
                    booleanValue = get( 0 ).getDataType( formatter ) == STRING;
                    return;
                case "iskeyword":
                    type = BOOLEAN;
                    param = get( 0 );
                    if( param.getDataType( formatter ) == STRING ) {
                        str = param.stringValue( formatter );
                        booleanValue = str == UrlUtils.removeQuote( str );
                    } else {
                        booleanValue = false;
                    }
                    return;
                case "ispixel":
                    type = BOOLEAN;
                    param = get( 0 );
                    booleanValue = param.unit( formatter ).equals( "px" );
                    return;
                case "isem":
                    type = BOOLEAN;
                    param = get( 0 );
                    booleanValue = param.unit( formatter ).equals( "em" );
                    return;
                case "ispercentage":
                    type = BOOLEAN;
                    param = get( 0 );
                    booleanValue = param.unit( formatter ).equals( "%" );
                    return;
                case "isunit":
                    type = BOOLEAN;
                    unit = get( 1 ).stringValue( formatter );
                    param = get( 0 );
                    booleanValue = param.unit( formatter ).equals( unit );
                    return;
                case "default":
                    if( formatter.isGuard() ) {
                        type = BOOLEAN;
                        booleanValue = formatter.getGuardDefault();
                        return;
                    }
                    break;
                case "-":
                    type = get( 0 ).getDataType( formatter ) ;
                    doubleValue = -getDouble( 0, formatter );
                    return;
                case "%":
                case "escape":
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
        type = STRING;
        return;
    }

    /**
     * Implements the format function "%"
     * 
     * @param formatter the current formation context
     */
    private void format( CssFormatter formatter ) {
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
                        formatter.setInlineMode( true );
                        get( idx++ ).appendTo( formatter );
                        formatter.setInlineMode( false );
                        break;
                    case 'S':
                        formatter.setInlineMode( true );
                        str = get( idx++ ).stringValue( formatter );
                        try {
                            str = new URI( null, null, str, null ).getRawPath();
                        } catch( URISyntaxException e ) {
                            e.printStackTrace();
                        }
                        formatter.append( str );
                        formatter.setInlineMode( false );
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
     * Implementation of the escape function: http://lesscss.org/functions/#string-functions-escape
     * 
     * @param formatter
     *            current formatter
     */
    private void escape( CssFormatter formatter ) {
        String url = get( 0 ).stringValue( formatter );
        url = UrlUtils.removeQuote( url );
        for( int i = 0; i < url.length(); i++ ) {
            char ch = url.charAt( i );
            if( ch < 0x80 ) {
                if( ch > ' ' ) {
                    switch( ch ){
                        case '#':
                        case '^':
                        case '(':
                        case ')':
                        case '{':
                        case '}':
                        case '|':
                        case ':':
                        case '>':
                        case '<':
                        case ';':
                        case '[':
                        case ']':
                        case '=':
                            break;
                        default:
                            formatter.append( ch );
                            continue;
                    }
                }
                formatter.append( '%' );
                formatter.appendHex( ch, 2 );
            } else {
                byte[] bytes = String.valueOf( ch ).getBytes(StandardCharsets.UTF_8);
                for( int j = 0; j < bytes.length; j++ ) {
                    formatter.append( '%' );
                    formatter.appendHex( bytes[j], 2 );
                }
            }
        }
    }

    /** 
     * NUMBER or PERCENT as data type.
     * @param formatter current formatter
     * @return the type
     */
    private int getNumberDataType( CssFormatter formatter ) {
        if( get( 0).getDataType( formatter ) == PERCENT ) {
            return PERCENT;
        }
        return NUMBER;
    }

    /**
     * Get the idx parameter from the parameter list.
     * 
     * @param idx
     *            the index starting with 0
     * @return the expression
     * @throws ParameterOutOfBoundsException if the parameter with the index does not exists
     */
    Expression get( int idx ) {
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
     * @param formatter
     *            current formatter
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
     * @param formatter
     *            current formatter
     * @return the expression
     */
    private int getInt( int idx, CssFormatter formatter ) {
        return (int)get( idx ).doubleValue( formatter );
    }

    /**
     * Get the idx parameter from the parameter list as a int value.
     * 
     * @param idx
     *            the index starting with 0
     * @param defaultValue
     *            the result if such a parameter idx does not exists.
     * @param formatter
     *            current formatter
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
     * @param formatter
     *            current formatter
     * @return the the percent value
     */
    private double getPercent( int idx, CssFormatter formatter ) {
        return ColorUtils.getPercent( get( idx ), formatter );
    }

    /**
     * Get the idx parameter from the parameter list as percent (range 0 - 1).
     * 
     * @param idx
     *            the index starting with 0
     * @param defaultValue
     *            the result if such a parameter idx does not exists.
     * @param formatter
     *            current formatter
     * @return the the percent value
     */
    private double getPercent( int idx, double defaultValue, CssFormatter formatter ) {
        if( parameters.size() <= idx ) {
            return defaultValue;
        }
        return ColorUtils.getPercent( get( idx ), formatter );
    }

    /**
     * Get the idx parameter from the parameter list as color value. And set the type variable.
     * 
     * @param idx
     *            the index starting with 0
     * @param formatter
     *            current formatter
     * @return the the color value
     */
    private double getColor( int idx, CssFormatter formatter ) {
        return getColor( get( idx ), formatter );
    }

    /**
     * Get a color value from the expression. And set the type variable.
     * 
     * @param exp
     *            the expression
     * @param formatter
     *            current formatter
     * @return the the color value
     * @throws ParameterOutOfBoundsException if the parameter with the index does not exists
     */
    private double getColor( Expression exp, CssFormatter formatter ) {
        type = exp.getDataType( formatter );
        switch( type ) {
            case COLOR:
            case RGBA:
                return exp.doubleValue( formatter );
        }
        throw new ParameterOutOfBoundsException();
    }

    /**
     * Get the value in radians.
     * @param formatter the CSS formatter
     * @return the radians
     */
    double getRadians( CssFormatter formatter ) {
        final Expression exp = get( 0 );
        String unit = exp.unit( formatter );
        return exp.doubleValue( formatter ) * Operation.unitFactor( unit, "rad", false );
    }

    /**
     * Get the idx parameter from the parameter list as double.
     * 
     * @param idx
     *            the index starting with 0
     * @param formatter
     *            current formatter
     * @return the expression
     */
    private double getDouble( int idx, CssFormatter formatter ) {
        return get( idx ).doubleValue( formatter );
    }

    /**
     * Get the idx parameter from the parameter list as double.
     * 
     * @param idx
     *            the index starting with 0
     * @param defaultValue
     *            the result if such a parameter idx does not exists.
     * @param formatter
     *            current formatter
     * @return the expression
     */
    private double getDouble( int idx, double defaultValue, CssFormatter formatter ) {
        if( parameters.size() <= idx ) {
            return defaultValue;
        }
        return parameters.get( idx ).doubleValue( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation listValue( CssFormatter formatter ) {
        switch( super.toString().toLowerCase() ) {
            case "extract":
                return extract( formatter ).listValue( formatter );
        }
        return super.listValue( formatter );
    }

    /**
     * Get for extract and length the first parameter as parameter list.
     * 
     * @param formatter
     *            current formatter
     * @return the list
     */
    private List<Expression> getParamList( CssFormatter formatter ) {
        Expression ex0 = get( 0 ).unpack( formatter );
        if( ex0.getDataType( formatter ) == LIST ) {
            Operation op = ex0.listValue( formatter );
            List<Expression> operants = op.getOperands();
            if( operants.size() == 1 ) {
                Expression ex0_0 = operants.get( 0 );
                if( ex0_0.getDataType( formatter ) == LIST ) {
                    return ex0_0.listValue( formatter ).getOperands();
                }
            }
            return operants;
        }
        List<Expression> result = new ArrayList<Expression>();
        result.add( ex0 );
        return result;
    }

    /**
     * Function extract. Change the type and doubleValue
     * @param formatter current CSS output
     * @return the extracted expression if it is a string
     */
    private Expression extract( CssFormatter formatter ) {
        List<Expression> exList = getParamList( formatter );
        int idx = getInt( 1, formatter );
        if( idx <= 0 || exList.size() < idx ) {
            type = STRING;
            return null;
        }
        Expression ex = exList.get( idx - 1 );
        type = ex.getDataType( formatter );
        switch( type ) {
            case STRING:
            case LIST:
                break;
            default:
                doubleValue = ex.doubleValue( formatter );
        }
        return ex;

    }
}
