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
 * Some methods for calculating colors.
 */
class ColorUtils {

    static HSL toHSL( int argb ) {
        double a = ((argb >> 24) & 0xFF) / 255.0;
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = ((argb >> 0) & 0xFF) / 255.0;

        double max = Math.max( Math.max( r, g ), b );
        double min = Math.min( Math.min( r, g ), b );
        double h, s, l = (max + min) / 2, d = max - min;

        if( max == min ) {
            h = s = 0;
        } else {
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

            if( max == r ) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if( max == g ) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }
        return new HSL( h * 360, s, l, a );
    }

    static double clamp( double val ) {
        return Math.min( 1, Math.max( 0, val ) );
    }

    static int rgba( double r, double g, double b, double a ) {
        return rgba( (int)Math.round( r ), (int)Math.round( g ), (int)Math.round( b ), a );
    }

    static int rgba( int r, int g, int b, double a ) {
        return (colorDigit(a * 255.1) << 24) | (colorDigit(r) << 16) | (colorDigit(g) << 8) | colorDigit(b);
    }

    static int rgb( int r, int g, int b ) {
        return (colorDigit(r) << 16) | (colorDigit(g) << 8) | colorDigit(b);
    }

    static double alpha( int argb ) {
        return Math.round( ((argb >> 24) & 0xFF) / 2.55) / 100.0; 
    }

    static int red( int rgb ) {
        return (rgb >> 16) & 0xFF; 
    }

    static int green( int rgb ) {
        return (rgb >> 8) & 0xFF; 
    }

    static int blue( int rgb ) {
        return rgb & 0xFF; 
    }

    private static double hsla_hue(double h, double m1, double m2) {
        h = h < 0 ? h + 1 : (h > 1 ? h - 1 : h);
        if      (h * 6 < 1) { return m1 + (m2 - m1) * h * 6; }
        else if (h * 2 < 1) { return m2; }
        else if (h * 3 < 2) { return m1 + (m2 - m1) * (2.0/3 - h) * 6; }
        else                { return m1; }
    }
    static int hsla (HSL hsl) {
        return hsla(hsl.h, hsl.s, hsl.l, hsl.a);
    }
    static int hsla (double h, double s, double l, double a) {

        h = (h % 360) / 360;
        s = clamp(s); l = clamp(l); a = clamp(a);

        double m2 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
        double m1 = l * 2 - m2;

        return rgba( hsla_hue(h + 1.0/3, m1, m2) * 255,
                     hsla_hue(h        , m1, m2) * 255,
                     hsla_hue(h - 1.0/3, m1, m2) * 255,
                     a);
    }
    
    static double luma( int rgb ) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = ((rgb >> 0) & 0xFF) / 255.0;

        r = (r <= 0.03928) ? r / 12.92 : Math.pow( ((r + 0.055) / 1.055), 2.4 );
        g = (g <= 0.03928) ? g / 12.92 : Math.pow( ((g + 0.055) / 1.055), 2.4 );
        b = (b <= 0.03928) ? b / 12.92 : Math.pow( ((b + 0.055) / 1.055), 2.4 );

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    static int contrast( int color, int dark, int light, double threshold ) {
        //Figure out which is actually light and dark!
        if( luma( dark ) > luma( light ) ) {
            int t = light;
            light = dark;
            dark = t;
        }
        if( luma( color ) < threshold ) {
            return light;
        } else {
            return dark;
        }
    }

    static int colorDigit( double value ) {
        if( value >= 255 ) {
            return 255;
        } else if( value <= 0 ) {
            return 0;
        } else {
            return (int)Math.round( value );
        }
    }
    
    static void appendColor( Appendable output, int rgb ) throws IOException {
        output.append( '#' );
        String hex = Integer.toHexString( rgb & 0xFFFFFF );
        for( int i = hex.length(); i < 6; i++ ) {
            output.append( '0' );
        }
        output.append( hex );
    }

    /**
     * Get the expression value as percent (range 0 - 1).
     * 
     * @param expression
     *            the expression
     * @return the percent value
     */
    static double getPercent( Expression expression, CssFormatter formatter ) {
        double d = expression.doubleValue( formatter );
        if( expression.getDataType( formatter ) == Expression.PERCENT ) {
            d /= 100;
        }
        return d;
    }
}
