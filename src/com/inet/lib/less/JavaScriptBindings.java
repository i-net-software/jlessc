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

import javax.script.SimpleBindings;

/**
 * Bindings to the current less variables
 */
class JavaScriptBindings extends SimpleBindings {

    private CssFormatter formatter;

    /**
     * Create a new bindings
     * @param formatter the CCS target
     */
    JavaScriptBindings( CssFormatter formatter ) {
        this.formatter = formatter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey( Object key ) {
        String keyStr = (String)key;
        if( keyStr.startsWith( "nashorn." ) ) {
            return super.containsKey( key );
        }
        Expression var = formatter.getVariable( '@' + keyStr );
        return var != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get( Object key ) {
        String keyStr = (String)key;
        if( keyStr.startsWith( "nashorn." ) ) {
            return super.get( key );
        }
        Expression var = formatter.getVariable( '@' + keyStr );
        if( var == null ) {
            return null;
        }
        Object obj;
        switch( var.getDataType( formatter ) ) {
            case Expression.NUMBER:
                obj = new Double( var.doubleValue( formatter ) );
                break;
            case Expression.BOOLEAN:
                obj = Boolean.valueOf( var.booleanValue( formatter ) );
                break;
            case Expression.STRING:
            default:
                obj = var.stringValue( formatter );
        }
        return new JavaScriptObject( obj );
    }
}
