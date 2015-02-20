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
 * A reference to a variable
 */
class VariableExpression extends AbstractExpression {

    VariableExpression( LessObject reader, String name ) {
        super( reader, name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) throws IOException {
        getValue( formatter ).appendTo( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDataType( CssFormatter formatter ) {
        return getValue( formatter ).getDataType( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue( CssFormatter formatter ) {
        return getValue( formatter ).doubleValue( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean booleanValue( CssFormatter formatter ) {
        return getValue( formatter ).booleanValue( formatter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unit( CssFormatter formatter ) {
        return getValue( formatter ).unit( formatter );
    }

    Expression getValue( CssFormatter formatter ) {
        String name = toString();
        Expression value = formatter.getVariable( name );
        if( value != null ) {
            return value;
        }
        if( name.startsWith( "@@" ) ) {
            name = name.substring( 1 );
            value = formatter.getVariable( name );
            if( value != null ) {
                formatter.setInineMode( true );
                name = '@' + value.stringValue( formatter );
                formatter.setInineMode( false );
                value = formatter.getVariable( name );
                if( value != null ) {
                    return value;
                }
            }
        }
        throw createException( "Undefine Variable: " + name );
    }
}
