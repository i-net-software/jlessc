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
 * A single less extends in the less file.
 */
class LessExtend extends LessObject implements Formattable {

    private String   selector;

    private String[] baseSelector;

    private boolean  all;

    private String extendingSelector;

    /**
     * Parse the extendSelector,create the needed LessExtend objects and add it to the container.
     * 
     * @param container
     *            the container that should add the created LessExtend
     * @param obj
     *            another LessObject with parse position.
     * @param extendSelector
     *            the completely selector like "foo:extends(bar all)"
     * @return the base selector
     */
    static String addLessExtendsTo( FormattableContainer container, LessObject obj, String extendSelector ) {
        int idx1 = extendSelector.indexOf( ":extend(" );
        int idx2 = extendSelector.indexOf( ')', idx1 );
        String selector = extendSelector.substring( 0, idx1 ).trim();
        String[] baseSelector = new String[] { selector };
        String params = extendSelector.substring( idx1 + 8, idx2 ).trim();

        for( String param : params.split( "," ) ) {
            boolean all = param.endsWith( " all" );
            if( all ) {
                param = param.substring( 0, param.length() - 4 ).trim();
            }
            container.add( new LessExtend( obj, baseSelector, param, all ) );
        }
        return selector;
    }

    /**
     * Create a new instance.
     * 
     * @param obj
     *            another LessObject with parse position.
     * @param baseSelector
     *            the base selector as single size array.
     * @param extendingSelector
     *            selector to extends
     * @param all
     *            If keyword "all" was set
     */
    private LessExtend( LessObject obj, String[] baseSelector, String extendingSelector, boolean all ) {
        super( obj );

        this.selector = baseSelector[0];
        this.baseSelector = baseSelector;
        this.extendingSelector = extendingSelector;
        this.all = all;
    }

    /**
     * If keyword "all" was set
     * @return true, if all was available
     */
    boolean isAll() {
        return all;
    }

    /**
     * Get the base selector.
     * @return the selector
     */
    String getSelector() {
        return selector;
    }

    /**
     * Get the base selector as single size array.
     * @return the selector
     */
    String[] getSelectors() {
        return baseSelector;
    }

    /**
     * Get selector to extends.
     * @return the selector
     */
    String getExtendingSelector() {
        return extendingSelector;
    }

    /**
     * For debugging of the library.
     * @return the debug string
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for( int i=0; i<baseSelector.length; i++ ) {
            if( i > 0 ) {
                builder.append( ',' );
            }
            builder.append( baseSelector[i] );
        }
        builder.append( ":extend(" );
        builder.append( extendingSelector );
        if( all ) {
            builder.append( " all" );
        }
        builder.append( ")" );
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType() {
        return EXTENDS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) {
        formatter.add( this );
    }
}
