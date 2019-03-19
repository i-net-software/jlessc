/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 - 2019 Volker Berlin
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Implementation of the function svg-Gradient and other URL utils.
 */
class UrlUtils {

    private static Method base64;
    private static Object encoder;

    /**
     * Remove a quote if exists.
     * 
     * @param str a string
     * @return the str without quotes
     */
    static @Nonnull String removeQuote( @Nonnull String str ) {
        if( str.length() > 1 ) {
            char ch = str.charAt( 0 );
            if( ch == '\'' || ch == '\"' ) {
                if( str.charAt( str.length() - 1 ) == ch ) {
                    return str.substring( 1, str.length() - 1 );
                }
            }
        }
        return str;
    }

    /**
     * Implementation of the function svg-Gradient.
     * 
     * @param formatter current formatter
     * @param parameters function parameters
     * @throws LessException if parameter list is wrong
     */
    static void svgGradient( CssFormatter formatter, List<Expression> parameters ) {
        if( parameters.size() < 3 ) {
            throw new LessException( "error evaluating function `svg-gradient expects direction, start_color [start_position], [color position,]..., end_color " );
        }
        String direction = parameters.get( 0 ).stringValue( formatter );
        String gradientType = "linear";
        String rectangleDimension = "x=\"0\" y=\"0\" width=\"1\" height=\"1\"";
        String gradientDirection;
        switch( direction ) {
            case "to bottom":
                gradientDirection = "x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\"";
                break;
            case "to right":
                gradientDirection = "x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"0%\"";
                break;
            case "to bottom right":
                gradientDirection = "x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\"";
                break;
            case "to top right":
                gradientDirection = "x1=\"0%\" y1=\"100%\" x2=\"100%\" y2=\"0%\"";
                break;
            case "ellipse":
            case "ellipse at center":
                gradientType = "radial";
                gradientDirection = "cx=\"50%\" cy=\"50%\" r=\"75%\"";
                rectangleDimension = "x=\"-50\" y=\"-50\" width=\"101\" height=\"101\"";
                break;
            default:
                throw new LessException( "error evaluating function `svg-gradient`: svg-gradient direction must be 'to bottom', 'to right', 'to bottom right', 'to top right' or 'ellipse at center'" );
        }
        StringBuilder builder = new StringBuilder( "<?xml version=\"1.0\" ?><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"100%\" height=\"100%\" viewBox=\"0 0 1 1\" preserveAspectRatio=\"none\">" );
        builder.append( '<' ).append( gradientType ).append( "Gradient id=\"gradient\" gradientUnits=\"userSpaceOnUse\" " ).append( gradientDirection ).append( '>' );

        for( int i = 1; i < parameters.size(); i++ ) {
            Expression param = parameters.get( i ).unpack( formatter );
            double color;
            double position;
            if( param.getClass() == Operation.class && ((Operation)param).getOperator() == ' ' ) {
                ArrayList<Expression> operands = ((Operation)param).getOperands();
                color = getColor( operands.get( 0 ), formatter );
                position = ColorUtils.getPercent( operands.get( 1 ), formatter );
            } else {
                color = getColor( param, formatter );
                position = (i - 1) / (parameters.size() - 2.0);
            }
            builder.append( "<stop offset=\"" );
            position *= 100;
            if( position == (int)position ) {
                builder.append( (int)position );
            } else {
                builder.append( formatter.getFormat().format( position ) );
            }
            builder.append( "%\" stop-color=\"" );
            formatter.addOutput();
            formatter.appendColor( color, null );
            builder.append( formatter.releaseOutput() );
            builder.append( '\"' );
            double alpha = ColorUtils.alpha( color );
            if( alpha < 1 ) {
                builder.append( " stop-opacity=\"" + alpha ).append( '\"' );
            }
            builder.append( "/>" );
        }
        builder.append( "</" ).append( gradientType ).append( "Gradient><rect " ).append( rectangleDimension ).append( " fill=\"url(#gradient)\" /></svg>" );

        byte[] bytes = builder.toString().getBytes( StandardCharsets.UTF_8 );

        formatter.append( "url('data:image/svg+xml;base64," );
        formatter.append( toBase64( bytes ) );
        formatter.append( "\')" );
    }

    /**
     * Get the color value of the expression or fire an exception if not a color.
     * 
     * @param param the expression to evaluate
     * @param formatter current formatter
     * @return the color value of the expression
     * @throws LessException if the expression is not a color value
     */
    static double getColor( Expression param, CssFormatter formatter ) throws LessException {
        switch( param.getDataType( formatter ) ) {
            case Expression.COLOR:
            case Expression.RGBA:
                return param.doubleValue( formatter );
        }
        throw new LessException( "Not a color: " + param );
    }

    /**
     * Implementation of the function data-uri.
     * 
     * @param formatter current formatter
     * @param relativeUrlStr relative URL of the less script. Is used as base URL
     * @param urlString the url parameter of the function
     * @param type the mime type
     * @throws IOException If any I/O errors occur on reading the content
     */
    static void dataUri( CssFormatter formatter, String relativeUrlStr, final String urlString, String type ) throws IOException {
        URL url = formatter.getBaseURL();
        String urlStr = removeQuote( urlString );
        InputStream input;
        url = new URL( url, urlStr );
        try {
            try {
                input = formatter.getReaderFactory().openStream( url );
            } catch( Exception e ) {
                // try rewrite location independent of option "rewrite-urls" for backward compatibility, this is not 100% compatible with Less CSS
                url = new URL( new URL( formatter.getBaseURL(), relativeUrlStr ), urlStr );
                input = formatter.getReaderFactory().openStream( url );
            }
        } catch( Exception e ) {
            boolean quote = urlString != urlStr;
            String rewrittenUrl;
            if( formatter.isRewriteUrl( urlStr ) ) {
                URL relativeUrl = new URL( relativeUrlStr );
                relativeUrl = new URL( relativeUrl, urlStr );
                rewrittenUrl = relativeUrl.getPath();
                rewrittenUrl = quote ? urlString.charAt( 0 ) + rewrittenUrl + urlString.charAt( 0 ) : rewrittenUrl;
            } else {
                rewrittenUrl = urlString;
            }
            formatter.append( "url(" ).append( rewrittenUrl ).append( ')' );
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int count;
        byte[] data = new byte[16384];

        while( (count = input.read( data, 0, data.length )) > 0 ) {
            buffer.write( data, 0, count );
        }
        input.close();

        byte[] bytes = buffer.toByteArray();

        if( bytes.length >= 32 * 1024 ) {
            formatter.append( "url(" ).append( urlString ).append( ')' );
        } else {
            dataUri( formatter, bytes, urlStr, type );
        }
    }

    /**
     * Write the bytes as inline url.
     * 
     * @param formatter current formatter
     * @param bytes the bytes
     * @param urlStr used if mime type is null to detect the mime type
     * @param type the mime type
     */
    static void dataUri( CssFormatter formatter, byte[] bytes, String urlStr, String type ) {
        if( type == null ) {
            switch( urlStr.substring( urlStr.lastIndexOf( '.' ) + 1 ) ) {
                case "gif": 
                    type = "image/gif;base64";
                    break;
                case "png": 
                    type = "image/png;base64";
                    break;
                case "jpg":
                case "jpeg":
                    type = "image/jpeg;base64";
                    break;
                default: 
                    type = "text/html";
            }
        } else {
            type = removeQuote( type );
        }

        if( type.endsWith( "base64" ) ) {
            formatter.append( "url(\"data:" ).append( type ).append( ',' );
            formatter.append( toBase64( bytes ) );
            formatter.append( "\")" );
        } else {
            formatter.append( "url(\"data:" ).append( type ).append( ',' );
            appendEncode( formatter, bytes );
            formatter.append( "\")" );
        }
    }

    /**
     * Append the bytes URL encoded.
     * 
     * @param formatter current formatter
     * @param bytes the bytes
     */
    private static void appendEncode( CssFormatter formatter, byte[] bytes ) {
        for( byte b : bytes ) {
            if ((b >= 'a' && b <= 'z' ) || (b >= 'A' && b <= 'Z' ) || (b >= '0' && b <= '9' )) {
                formatter.append( (char )b );
            } else {
                switch( b ) {
                    case '-':
                    case '_':
                    case '*':
                    case '.':
                        formatter.append( (char )b );
                        break;
                    default:
                        formatter.append( '%' );
                        formatter.append( Character.toUpperCase( Character.forDigit((b >> 4) & 0xF, 16) ) );
                        formatter.append( Character.toUpperCase( Character.forDigit(b & 0xF, 16) ) );
                }
            }
        }
    }

    /**
     * Hack for base64 in Java 7 to Java 9.
     * <ul>
     * <li>In Java 7 we use: javax.xml.bind.DatatypeConverter.printBase64Binary( byte[] ) 
     * <li>in Java 8-9 we use: java.util.Base64.getEncoder().encodeToString( byte[] )
     * </ul>
     * @param bytes the bytes to converted
     * @return the base64 encoded string
     * @throws LessException if the reflection does not work
     */
    private static String toBase64( byte[] bytes ) {
        if( base64 == null ) {
            try {
                Class<?> clazz = Class.forName( "java.util.Base64" );
                encoder = clazz.getMethod( "getEncoder" ).invoke( null );
                base64 = encoder.getClass().getMethod( "encodeToString", byte[].class );
                base64.setAccessible( true ); /// performance optimizing
            } catch( Throwable th1 ) {
                try {
                    Class<?> clazz = Class.forName( "javax.xml.bind.DatatypeConverter" );
                    base64 = clazz.getMethod( "printBase64Binary", byte[].class );
                    encoder = null;
                    base64.setAccessible( true ); /// performance optimizing
                } catch( Throwable th2 ) {
                    th1.addSuppressed( th2 );
                    throw new LessException( th1 );
                }
            }
        }
        try {
            return (String)base64.invoke( encoder, bytes );
        } catch( Throwable th ) {
            throw new LessException( th );
        }
    }
}
