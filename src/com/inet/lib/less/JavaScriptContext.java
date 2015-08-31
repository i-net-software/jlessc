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

import java.util.HashMap;

import javax.script.SimpleScriptContext;

/**
 * A ScriptContext for executing a JavaScript expression.
 */
class JavaScriptContext extends SimpleScriptContext {

    private final JavaScriptExpression    expr;

    private final HashMap<String, Object> attributes = new HashMap<>();

    /**
     * Create a new instance.
     * @param formatter the formatter with a reference to the variables of the current scope
     * @param expr the JavaScript expression
     */
    JavaScriptContext( CssFormatter formatter, JavaScriptExpression expr ) {
        this.expr = expr;
        engineScope = new JavaScriptBindings( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute( String name ) {
        switch( name ) {
            case "javax.script.filename":
                return expr.getFileName();
        }
        return attributes.get( name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute( String name, Object value, int scope ) {
        attributes.put( name, value );
    }
}
