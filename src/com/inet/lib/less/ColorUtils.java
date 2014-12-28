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

    static HSL toHSL( double color ) {
        long argb = Double.doubleToRawLongBits( color );
        double a = alpha( color );
        double r = clamp( ((argb >> 32) & 0xFFFF) / (double)0xFF00 );
        double g = clamp( ((argb >> 16) & 0xFFFF) / (double)0xFF00 );
        double b = clamp( ((argb >> 0) & 0xFFFF) / (double)0xFF00 );

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

    static double rgba( double r, double g, double b, double a ) {
        return Double.longBitsToDouble( Math.round( a * 0xFFFF ) << 48 | (colorLargeDigit(r) << 32) | (colorLargeDigit(g) << 16) | colorLargeDigit(b) );
    }

    static double rgba( int r, int g, int b, double a ) {
        return Double.longBitsToDouble( Math.round( a * 0xFFFF ) << 48 | (colorLargeDigit(r) << 32) | (colorLargeDigit(g) << 16) | colorLargeDigit(b) );
    }

    static double rgb( int r, int g, int b ) {
        return Double.longBitsToDouble( Expression.ALPHA_1 | (colorLargeDigit(r) << 32) | (colorLargeDigit(g) << 16) | colorLargeDigit(b) );
    }

    static int argb( double color ) {
        long value = Double.doubleToRawLongBits( color );
        int result = colorDigit( ((value >>> 48)) / 256.0 ) << 24;
        result |= colorDigit( ((value >> 32) & 0xFFFF) / 256.0 ) << 16; 
        result |= colorDigit( ((value >> 16) & 0xFFFF) / 256.0 ) << 8; 
        result |= colorDigit( ((value) & 0xFFFF) / 256.0 ); 
        return result;
    }

    static double alpha( double color ) {
        double value = (Double.doubleToRawLongBits( color ) >>> 48) / (double)0XFFFF;
        return Math.round( value * 10000 ) / 10000.0;
    }

    static int red( double color ) {
        return colorDigit( ((Double.doubleToRawLongBits( color ) >> 32) & 0xFFFF) / 256.0 ); 
    }

    static int green( double color ) {
        return colorDigit( ((Double.doubleToRawLongBits( color ) >> 16 & 0xFFFF)) / 256.0 ); 
    }

    static int blue( double color ) {
        return colorDigit( (Double.doubleToRawLongBits( color ) & 0xFFFF) / 256.0 ); 
    }

    private static double hsla_hue(double h, double m1, double m2) {
        h = h < 0 ? h + 1 : (h > 1 ? h - 1 : h);
        if      (h * 6 < 1) { return m1 + (m2 - m1) * h * 6; }
        else if (h * 2 < 1) { return m2; }
        else if (h * 3 < 2) { return m1 + (m2 - m1) * (2F/3 - h) * 6; }
        else                { return m1; }
    }

    static double hsla (HSL hsl) {
        return hsla(hsl.h, hsl.s, hsl.l, hsl.a);
    }

    static double hsla( double h, double s, double l, double a ) {

        h = (h % 360) / 360;
        s = clamp(s); l = clamp(l); a = clamp(a);

        double m2 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
        double m1 = l * 2 - m2;

        return rgba( hsla_hue(h + 1.0/3, m1, m2) * 255,
                     hsla_hue(h        , m1, m2) * 255,
                     hsla_hue(h - 1.0/3, m1, m2) * 255,
                     a);
    }
    
    static double luma( double color ) {
        long argb = Double.doubleToRawLongBits( color );
        double r = ((argb >> 32) & 0xFFFF) / (double)0xFF00;
        double g = ((argb >> 16) & 0xFFFF) / (double)0xFF00;
        double b = ((argb) & 0xFFFF) / (double)0xFF00;

        r = (r <= 0.03928) ? r / 12.92 : Math.pow( ((r + 0.055) / 1.055), 2.4 );
        g = (g <= 0.03928) ? g / 12.92 : Math.pow( ((g + 0.055) / 1.055), 2.4 );
        b = (b <= 0.03928) ? b / 12.92 : Math.pow( ((b + 0.055) / 1.055), 2.4 );

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    static double contrast( double color, double dark, double light, double threshold ) {
        //Figure out which is actually light and dark!
        if( luma( dark ) > luma( light ) ) {
            double t = light;
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

    private static long colorLargeDigit( double value ) {
        value *= 0x100;
        if( value >= 0xFFFF ) {
            return 0xFFFF;
        } else if( value <= 0 ) {
            return 0;
        } else {
            return Math.round( value );
        }
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
