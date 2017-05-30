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

import java.util.ArrayList;

/**
 * A CSS output of a media rule.
 */
class CssMediaOutput extends CssOutput {

    private String[]             selectors;

    private ArrayList<CssOutput> results = new ArrayList<>();

    private boolean              isReference;

    private LessExtendMap        lessExtends;

    /**
     * Create a instance.
     * 
     * @param selectors
     *            the selectors of the rule
     * @param output
     *            a buffer for the content of the rule.
     * @param isReference
     *            if this content was loaded via reference
     * @param lessExtends
     *            a extends container only for this media rule
     */
    CssMediaOutput( String[] selectors, StringBuilder output, boolean isReference, LessExtendMap lessExtends ) {
        this.selectors = selectors;
        this.results.add( new CssPlainOutput( output ) );
        this.isReference = isReference;
        this.lessExtends = lessExtends;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void appendTo( StringBuilder target, LessExtendMap lessExtends, CssFormatter formatter ) {
        if( hasContent( lessExtends ) ) {
            formatter.startBlockImpl( selectors );
            for( CssOutput cssOutput : results ) {
                cssOutput.appendTo( target, this.lessExtends, formatter );
            }
            formatter.endBlockImpl();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean hasContent( LessExtendMap lessExtends ) {
        for( CssOutput cssOutput : results ) {
            if( cssOutput.hasContent( this.lessExtends ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the selectors of this rule.
     * 
     * @return the selectors
     */
    String[] getSelectors() {
        return selectors;
    }

    /**
     * Start a block inside the media
     * 
     * @param selectors
     *            the selectors
     * @param output
     *            a buffer for the content of the rule.
     */
    void startBlock( String[] selectors , StringBuilder output  ) {
        this.results.add( new CssRuleOutput( selectors, output, isReference ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    StringBuilder getOutput() {
        CssOutput cssOutput = results.get( results.size() - 1 );
        if( cssOutput instanceof CssRuleOutput ) {
            cssOutput = new CssPlainOutput( new StringBuilder() );
            this.results.add( cssOutput );
        }
        return cssOutput.getOutput();
    }
}
