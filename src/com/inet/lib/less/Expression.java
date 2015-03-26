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
 * A expression that can be evaluate to a value.
 */
interface Expression extends Formattable {

    static final int UNKNOWN = 0;

    static final int BOOLEAN = 1;

    static final int NUMBER  = 2;

    static final int PERCENT = 3;

    static final int RGBA    = 4;

    static final int COLOR   = 5;

    static final int STRING  = 6;

    // A color alpha value of 1 as long mask.
    static final long ALPHA_1 = 0xFFFF_0000_0000_0000L;

    static final double WHITE = Double.longBitsToDouble( ALPHA_1 | 0xFF00_FF00_FF00L );

    static final double BLACK = Double.longBitsToDouble( ALPHA_1 );

    /**
     * The data type of the expression
     * 
     * @param formatter
     *            the CCS target
     * 
     * @return one of the constant
     */
    int getDataType( CssFormatter formatter );

    /**
     * Get the numeric value.
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    double doubleValue( CssFormatter formatter );

    /**
     * Get the boolean value
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    boolean booleanValue( CssFormatter formatter );

    /**
     * Get the string value
     * 
     * @param formatter
     *            the CCS target
     * @return the value
     */
    String stringValue( CssFormatter formatter );

    /**
     * Get the unit of a NUMBER value.
     * 
     * @param formatter
     *            the CCS target
     * @return the unit or empty string if nothing
     */
    String unit( CssFormatter formatter );
}
