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
 * An object that can be print as CSS.
 */
interface Formattable {

    static final int PROPERTY = 0;
    static final int RULE = 1;
    static final int MIXIN = 2;
    static final int EXPRESSION = 3;
    static final int COMMENT = 4;
    static final int CSS_AT_RULE = 5;
    static final int EXTENDS = 6;
    static final int REFERENCE_INFO = 7;

    /**
     * The type of formattable. Can be used in switches
     * @return the type
     */
    int getType();

    /**
     * Write the object to the CSS output
     * 
     * @param formatter
     *            the CCS target
     */
    void appendTo( CssFormatter formatter );
}
