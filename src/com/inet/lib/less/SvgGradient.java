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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

/**
 * Implementation of the function svg-Gradient
 */
class SvgGradient {

    /**
     * Implementation of the function svg-Gradient
     */
    static void svgGradient( CssFormatter formatter, List<Expression> parameters ) throws IOException {
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
            Expression param = parameters.get( i );
            double color;
            double position;
            if( param.getClass() == Operation.class && ((Operation)param).getOperator() == ' ' ) {
                ArrayList<Expression> operands = ((Operation)param).getOperands();
                color = operands.get( 0 ).doubleValue( formatter );
                position = ColorUtils.getPercent( operands.get( 1 ), formatter );
            } else {
                color = param.doubleValue( formatter );
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
            Appendable original = formatter.swapOutput( builder );
            formatter.appendColor( color, null );
            formatter.swapOutput( original );
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
        formatter.append( DatatypeConverter.printBase64Binary( bytes ) );
        formatter.append( "\')" );
    }

}
