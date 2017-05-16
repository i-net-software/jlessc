/**
 * MIT License (MIT)
 *
 * Copyright (c) 2015 Volker Berlin
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
 * Wrapper class to accessible a variable through scripting engine. Must be public. Lesscss expected that the values are accessible through the method toJS().
 */
public class JavaScriptObject {

    private Object obj;

    /**
     * New wrapper object.
     * @param obj the native value
     */
    JavaScriptObject( Object obj ) {
        this.obj = obj;
    }

    /**
     * Unwrap method. Must be public.
     * @return the value
     */
    public Object toJS() {
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return obj.toString();
    }
}
