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
 * Some methods for calculating colors.
 */
class ColorUtils {

    // color blending functions
    private static final int MULTIPLY = 0;
    private static final int SCREEN = 1;
    private static final int OVERLAY = 2;
    private static final int SOFTLIGHT = 3;
    private static final int HARDLIGHT = 4;
    private static final int DIFFERENCE = 5;
    private static final int EXCLUSION = 6;
    private static final int AVERAGE = 7;
    private static final int NEGATION = 8;

    /**
     * Create a HSL color.
     * @param color argb color
     * @return the HSL
     */
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

    /**
     * Limit the value between 0.0 and 1.0
     * 
     * @param val
     *            the value
     * @return the limited value
     */
    static double clamp( double val ) {
        return Math.min( 1, Math.max( 0, val ) );
    }

    /**
     * Create a color from rgba values
     * @param r red in range of 0 to 255.0
     * @param g green in range of 0 to 255.0
     * @param b blue in range of 0 to 255.0
     * @param a alpha in range of 0.0 to 1.0
     * @return color value as long
     */
    static double rgba( double r, double g, double b, double a ) {
        return Double.longBitsToDouble( Math.round( a * 0xFFFF ) << 48 | (colorLargeDigit(r) << 32) | (colorLargeDigit(g) << 16) | colorLargeDigit(b) );
    }

    /**
     * Create an color value.
     * 
     * @param r
     *            red in range from 0 to 255
     * @param g
     *            green in range from 0 to 255
     * @param b
     *            blue in range from 0 to 255
     * @param a
     *            alpha in range from 0.0 to 1.0
     * @return color value as long
     */
    static double rgba( int r, int g, int b, double a ) {
        return Double.longBitsToDouble( Math.round( a * 0xFFFF ) << 48 | (colorLargeDigit(r) << 32) | (colorLargeDigit(g) << 16) | colorLargeDigit(b) );
    }

    /**
     * Create an color value.
     * 
     * @param r
     *            red in range from 0 to 255
     * @param g
     *            green in range from 0 to 255
     * @param b
     *            blue in range from 0 to 255
     * @return color value as long
     */
    static double rgb( int r, int g, int b ) {
        return Double.longBitsToDouble( Expression.ALPHA_1 | (colorLargeDigit(r) << 32) | (colorLargeDigit(g) << 16) | colorLargeDigit(b) );
    }

    /**
     * Convert an color value as long into integer color value
     * 
     * @param color
     *            color value as long
     * @return color value as int
     */
    static int argb( double color ) {
        long value = Double.doubleToRawLongBits( color );
        int result = colorDigit( ((value >>> 48)) / 256.0 ) << 24;
        result |= colorDigit( ((value >> 32) & 0xFFFF) / 256.0 ) << 16; 
        result |= colorDigit( ((value >> 16) & 0xFFFF) / 256.0 ) << 8; 
        result |= colorDigit( ((value) & 0xFFFF) / 256.0 ); 
        return result;
    }

    /**
     * Get the alpha value from a color value
     * 
     * @param color
     *            color value as long
     * @return the alpha in the range 0.0 to 1.0
     */
    static double alpha( double color ) {
        double value = (Double.doubleToRawLongBits( color ) >>> 48) / (double)0XFFFF;
        return Math.round( value * 10000 ) / 10000.0;
    }

    /**
     * Get the red value from a color value
     * 
     * @param color
     *            color value as long
     * @return the alpha in the range 0 to 255
     */
    static int red( double color ) {
        return colorDigit( ((Double.doubleToRawLongBits( color ) >> 32) & 0xFFFF) / 256.0 ); 
    }

    /**
     * Get the green value from a color value
     * 
     * @param color
     *            color value as long
     * @return the alpha in the range 0 to 255
     */
    static int green( double color ) {
        return colorDigit( ((Double.doubleToRawLongBits( color ) >> 16 & 0xFFFF)) / 256.0 ); 
    }

    /**
     * Get the blue value from a color value
     * 
     * @param color
     *            color value as long
     * @return the alpha in the range 0 to 255
     */
    static int blue( double color ) {
        return colorDigit( (Double.doubleToRawLongBits( color ) & 0xFFFF) / 256.0 ); 
    }

    /**
     * Calculate a single color channel of the HSLA function
     * @param h hue value
     * @param m1 value 1 in range of 0.0 to 1.0
     * @param m2 value 2 in range of 0.0 to 1.0
     * @return channel value in range of 0.0 to 1.0
     */
    private static double hslaHue(double h, double m1, double m2) {
        h = h < 0 ? h + 1 : (h > 1 ? h - 1 : h);
        if      (h * 6 < 1) { return m1 + (m2 - m1) * h * 6; }
        else if (h * 2 < 1) { return m2; }
        else if (h * 3 < 2) { return m1 + (m2 - m1) * (2F/3 - h) * 6; }
        else                { return m1; }
    }

    /**
     * Create a color value.
     * 
     * @param hsl
     *            a HSL value
     * @return a color as long
     */
    static double hsla( HSL hsl ) {
        return hsla(hsl.h, hsl.s, hsl.l, hsl.a);
    }

    /**
     * Create a color value.
     * 
     * @param h
     *            hue value
     * @param s
     *            saturation
     * @param l
     *            lightness
     * @param a
     *            alpha
     * @return a color value as long
     */
    static double hsla( double h, double s, double l, double a ) {

        h = (h % 360) / 360;
        s = clamp(s); l = clamp(l); a = clamp(a);

        double m2 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
        double m1 = l * 2 - m2;

        return rgba( hslaHue(h + 1.0/3, m1, m2) * 255,
                     hslaHue(h        , m1, m2) * 255,
                     hslaHue(h - 1.0/3, m1, m2) * 255,
                     a);
    }

    /**
     * The less function "luminance".
     * 
     * @param color
     *            a color value as long
     * @return a value in the range from 0.0 to 1.0
     */
    static double luminance( double color ) {
        long argb = Double.doubleToRawLongBits( color );
        double r = ((argb >> 32) & 0xFFFF) / (double)0xFF00;
        double g = ((argb >> 16) & 0xFFFF) / (double)0xFF00;
        double b = ((argb) & 0xFFFF) / (double)0xFF00;
        return (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
    }

    /**
     * The less function "luma".
     * 
     * @param color
     *            a color value as long
     * @return a value in the range from 0.0 to 1.0
     */
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

    /**
     * The less function "contrast".
     * 
     * @param color
     *            a color value as long
     * @param dark
     *            a designated dark color
     * @param light
     *            a designated light color
     * @param threshold
     *            percentage 0-100% specifying where the transition from "dark" to "light" is
     * @return a color value as long
     */
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

    /**
     * Create a HSV color.
     * @param color argb color
     * @return the HSL
     */
    static HSV toHSV( double color ) {
        long argb = Double.doubleToRawLongBits( color );
        double a = alpha( color );
        double r = clamp( ((argb >> 32) & 0xFFFF) / (double)0xFF00 );
        double g = clamp( ((argb >> 16) & 0xFFFF) / (double)0xFF00 );
        double b = clamp( ((argb >> 0) & 0xFFFF) / (double)0xFF00 );

        double max = Math.max(Math.max(r, g), b);
        double min = Math.min(Math.min(r, g), b);
        double h, s, v = max;

        double d = max - min;
        if (max == 0) {
            s = 0;
        } else {
            s = d / max;
        }

        if (max == min) {
            h = 0;
        } else if( max == r ){
            h = (g - b) / d + (g < b ? 6 : 0);
        } else if( max == g ){
            h = (b - r) / d + 2;
        } else { //if( max == b ){
            h = (r - g) / d + 4;
        }
        h /= 6;
        return new HSV( h * 360, s, v,  a );
    }

    private static final int[][] HSVA_PERM = { { 0, 3, 1 }, //
                    { 2, 0, 1 }, //
                    { 1, 0, 3 }, //
                    { 1, 2, 0 }, //
                    { 3, 1, 0 }, //
                    { 0, 1, 2 }           }; //

    /**
     * Create a color value.
     * 
     * @param hue
     *            hue value
     * @param saturation
     *            saturation
     * @param value
     *            the value
     * @param alpha
     *            alpha
     * @return a color value as long
     */
    static double hsva( double hue, double saturation, double value, double alpha ) {
        hue = ((hue % 360) / 360) * 360;

        int i = (int)Math.floor( (hue / 60) % 6 );
        double f = (hue / 60) - i;

        double[] vs = { value, value * (1 - saturation), value * (1 - f * saturation), value * (1 - (1 - f) * saturation) };

        return rgba( vs[HSVA_PERM[i][0]] * 255, vs[HSVA_PERM[i][1]] * 255, vs[HSVA_PERM[i][2]] * 255, alpha );
    }

    /**
     * Calculate the mix color of 2 colors.
     * 
     * @param color1
     *            first color
     * @param color2
     *            second color
     * @param weight
     *            balance point between the two colors in range of 0 to 1.
     * @return the resulting color
     */
    static double mix( double color1, double color2, double weight ) {
        long col1 = Double.doubleToRawLongBits( color1 );
        long col2 = Double.doubleToRawLongBits( color2 );

        int alpha1 = (int)(col1  >>> 48);
        int red1 = (int)(col1  >> 32) & 0xFFFF;
        int green1 = (int)(col1  >> 16) & 0xFFFF;
        int blue1 = (int)(col1) & 0xFFFF;
        int alpha2 = (int)(col2  >>> 48);
        int red2 = (int)(col2  >> 32) & 0xFFFF;
        int green2 = (int)(col2  >> 16) & 0xFFFF;
        int blue2 = (int)(col2) & 0xFFFF;

        double w = weight * 2 - 1;
        double a = (alpha1 - alpha2) / (double)0XFFFF;

        double w1 = (((w * a == -1) ? w : (w + a) / (1 + w * a)) + 1) / 2.0;
        double w2 = 1 - w1;

        long red = Math.round(red1 * w1 + red2 * w2);
        long green = Math.round(green1 * w1 + green2 * w2);
        long blue = Math.round(blue1 * w1 + blue2 * w2);

        long alpha = Math.round(alpha1 * weight + alpha2 * (1 - weight));

        long color = (alpha << 48) | (red << 32) | (green << 16) | (blue);
        return Double.longBitsToDouble( color );
    }

    /**
     * Color blending function multiply
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double multiply( double color1, double color2 ) {
        return colorBlending( color1, color2, MULTIPLY );
    }

    /**
     * Color blending function screen
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double screen( double color1, double color2 ) {
        return colorBlending( color1, color2, SCREEN );
    }

    /**
     * Color blending function overlay
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double overlay( double color1, double color2 ) {
        return colorBlending( color1, color2, OVERLAY );
    }

    /**
     * Color blending function softlight
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double softlight( double color1, double color2 ) {
        return colorBlending( color1, color2, SOFTLIGHT );
    }

    /**
     * Color blending function hardlight
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double hardlight( double color1, double color2 ) {
        return colorBlending( color1, color2, HARDLIGHT );
    }

    /**
     * Color blending function difference
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double difference( double color1, double color2 ) {
        return colorBlending( color1, color2, DIFFERENCE );
    }

    /**
     * Color blending function exclusion
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double exclusion( double color1, double color2 ) {
        return colorBlending( color1, color2, EXCLUSION );
    }

    /**
     * Color blending function average
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double average( double color1, double color2 ) {
        return colorBlending( color1, color2, AVERAGE );
    }

    /**
     * Color blending function negation
     * 
     * @param color1 left color
     * @param color2 right color
     * @return argb color value
     */
    static double negation( double color1, double color2 ) {
        return colorBlending( color1, color2, NEGATION );
    }

    /**
     * Color blending operation
     * 
     * @param color1 left color
     * @param color2 right color
     * @param op blending operation
     * @return argb color value
     */
    private static double colorBlending( double color1, double color2, int op ) {
        long argb1 = Double.doubleToRawLongBits( color1 );
        long r1 = ((argb1 >> 32) & 0xFFFF);
        long g1 = ((argb1 >> 16) & 0xFFFF);
        long b1 = ((argb1) & 0xFFFF);

        long argb2 = Double.doubleToRawLongBits( color2 );
        long r2 = ((argb2 >> 32) & 0xFFFF);
        long g2 = ((argb2 >> 16) & 0xFFFF);
        long b2 = ((argb2) & 0xFFFF);

        r1 = colorBlendingDigit( r1, r2, op );
        g1 = colorBlendingDigit( g1, g2, op );
        b1 = colorBlendingDigit( b1, b2, op );
        argb1 = r1 << 32 | g1 << 16 | b1;

        return Double.longBitsToDouble( argb1 );
    }

    /**
     * Color blending operation for a single color channel.
     * 
     * @param longDigit1 left digit
     * @param longDigit2 right digit
     * @param op blending operation
     * @return resulting color digit
     * @throws IllegalArgumentException if the operation is unknown
     */
    private static long colorBlendingDigit( long longDigit1, long longDigit2, int op ) {
        switch( op ) {
            case MULTIPLY:
                return longDigit1 * longDigit2 / 0xFF00;
            case SCREEN:
                return longDigit1 + longDigit2 - longDigit1 * longDigit2 / 0xFF00;
            case OVERLAY:
                longDigit1 *= 2;
                if( longDigit1 <= 0xFF00 ) {
                    return colorBlendingDigit( longDigit1, longDigit2, MULTIPLY );
                } else {
                    return colorBlendingDigit( longDigit1 - 0xFF00, longDigit2, SCREEN );
                }
            case SOFTLIGHT:
                long d = 0xFF00, e = longDigit1;
                if (longDigit2 > 0x8000) {
                    e = 0xFF00;
                    d = (longDigit1 > 0x4000) ? (long)(Math.sqrt(longDigit1 / (double)0xFF00 ) * 0xFF00)
                        : ((16 * longDigit1 - 12 * 0xFF00) * longDigit1 / 0xFF00 + 4 * 0xFF00) * longDigit1 / 0xFF00;
                }
                return longDigit1 - (0xFF00 - 2 * longDigit2) * e * (d - longDigit1) / 0xFF00 / 0xFF00;
            case HARDLIGHT:
                return colorBlendingDigit( longDigit2, longDigit1, OVERLAY );
            case DIFFERENCE:
                return Math.abs( longDigit1 - longDigit2 );
            case EXCLUSION:
                return longDigit1 + longDigit2 - 2 * longDigit1 * longDigit2 / 0xFF00;
            case AVERAGE:
                return (longDigit1 + longDigit2) / 2;
            case NEGATION:
                return 0xFF00 - Math.abs( longDigit1 + longDigit2 - 0xFF00 );
            default:
                throw new IllegalArgumentException( String.valueOf( op ) );
        }
    }

    /**
     * Limit a value in the range 0 to 255.
     * 
     * @param value
     *            the color channel digit
     * @return the limited value
     */
    static int colorDigit( double value ) {
        if( value >= 255 ) {
            return 255;
        } else if( value <= 0 ) {
            return 0;
        } else {
            return (int)Math.round( value );
        }
    }

    /**
     * Limit a value in the range 0 to 65535 (0xFFFF).
     * 
     * @param value
     *            the color channel digit in the range 0.0 to 256.0
     * @return the limited value
     */
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
     * @param formatter
     *            current formatter
     * @return the percent value
     */
    static double getPercent( Expression expression, CssFormatter formatter ) {
        double d = expression.doubleValue( formatter );
        if( expression.getDataType( formatter ) == Expression.PERCENT ) {
            d /= 100;
        } else if( d > 1.0 && d == (int)d ) {
            d /= 100;
        }
        return d;
    }
}
