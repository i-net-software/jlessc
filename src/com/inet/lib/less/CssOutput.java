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
 * Container for formatted CSS result.
 */
abstract class CssOutput {

    /**
     * Write the this output to the target
     * @param target the target
     * @param lessExtends all extends in the less
     * @param formatter a formatter
     */
    abstract void appendTo( StringBuilder target, LessExtendMap lessExtends, CssFormatter formatter );

    /**
     * If this output has content
     * 
     * @param lessExtends
     *            current extends container
     * @return true, if there is content
     */
    abstract boolean hasContent( LessExtendMap lessExtends );

    /**
     * Get the selectors of this rule.
     * 
     * @return the selectors
     */
    abstract String[] getSelectors();

    /**
     * Get the native output buffer
     * 
     * @return the buffer
     */
    abstract StringBuilder getOutput();
}
