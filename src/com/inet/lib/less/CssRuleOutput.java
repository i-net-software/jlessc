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
 * A CSS output of a single rule.
 */
class CssRuleOutput extends CssOutput {

    private String[] selectors;
    private StringBuilder output;

    /**
     * Create a instance.
     * @param selectors the selectors of the rule
     * @param output a buffer for the content of the rule. 
     */
    CssRuleOutput( String[] selectors, StringBuilder output ) {
        this.selectors = selectors;
        this.output = output;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void appendTo( StringBuilder target, LessExtendMap lessExtends, CssFormatter formatter ) {
        if( output.length() > 0 ) {
            selectors = lessExtends.concatenateExtends( selectors );
            formatter.startBlockImpl( selectors );
            target.append( output );
            formatter.endBlockImpl();
        }
    }

    /**
     * Get the selectors of this rule.
     * @return the selectors
     */
    String[] getSelectors() {
        return selectors;
    }

    /**
     * Get the output of this rule.
     * @return the output
     */
    StringBuilder getOutput() {
        return output;
    }
}
