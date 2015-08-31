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

import java.util.Collection;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * An expression with JavaScript.
 */
public class JavaScriptExpression extends Expression {

    private int    type;

    private Object result;

    /**
     * Create a new instance.
     * @param obj another LessObject with parse position.
     * @param str the JavaScript
     */
    JavaScriptExpression( LessObject obj, String str ) {
        super( obj, str );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) {
        switch( getDataType( formatter ) ) {
            case STRING:
                formatter.append( '\"' ).append( String.valueOf( result ) ).append( '\"' );
                return;
            case LIST:
                listValue( formatter ).appendTo( formatter );
                return;
        }
        super.appendTo( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    int getDataType( CssFormatter formatter ) {
        eval( formatter );
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    double doubleValue( CssFormatter formatter ) {
        return ((Number)result).doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean booleanValue( CssFormatter formatter ) {
        return ((Boolean)result).booleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String unit( CssFormatter formatter ) {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation listValue( CssFormatter formatter ) {
        eval( formatter );
        if( type == LIST ) {
            Operation op = new Operation( this );
            for( Object obj : ((Collection)result) ) {
                op.addOperand( new ValueExpression( this, obj ) );
            }
            return op;
        } else {
            return super.listValue( formatter );
        }
    }

    /**
     * Execute the JavaScript 
     * @param formatter current formatter
     */
    private void eval( CssFormatter formatter ) {
        if( type != UNKNOWN ) {
            return;
        }
        try {
            ScriptEngineManager factory = new ScriptEngineManager( getClass().getClassLoader() );
            ScriptEngine engine = factory.getEngineByName( "JavaScript" );
            engine.setContext( new JavaScriptContext( formatter, this ) );

            String script = toString();
            script = SelectorUtils.replacePlaceHolder( formatter, script, this );
            script = script.substring( 1, script.length() - 1 );
            result = engine.eval( script );
            if( result instanceof Number ) {
                type = NUMBER;
            } else if( result instanceof Boolean ) {
                type = BOOLEAN;
            } else if( result instanceof Collection ) {
                type = LIST;
            } else if( result instanceof Map ) {
                result = ((Map)result).values();
                type = LIST;
            } else {
                type = STRING;
            }
        } catch( Exception ex ) {
            throw createException( ex );
        }
    }
}
