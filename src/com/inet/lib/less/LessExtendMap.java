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

import java.util.ArrayList;
import java.util.List;

/**
 * Hold all extends that was parsed.
 */
class LessExtendMap {

    private final HashMultimap<String, LessExtend> all          = new HashMultimap<>();

    private final HashMultimap<String, LessExtend> exact        = new HashMultimap<>();

    private ArrayList<String>                      selectorList = new ArrayList<>();

    void add( LessExtend lessExtend ) {
        if( lessExtend.isAll() ) {
            String[] selectors = lessExtend.getExtendingSelectors();
            for( String selector : selectors ) {
                SelectorTokenizer tokenizer = new SelectorTokenizer( selector );
                do {
                    String token = tokenizer.next();
                    if( token == null ) {
                        break;
                    }
                    all.add( token, lessExtend );
                } while( true );
            }
        } else {
            String[] selectors = lessExtend.getExtendingSelectors();
            for( String selector : selectors ) {
                exact.add( selector, lessExtend );
            }
        }
    }

    public String[] concatenateExtends( String[] selectors ) {
        selectorList.clear();
        for( String selector : selectors ) {
            List<LessExtend> list = exact.get( selector );
            if( list != null ) {
                for( LessExtend lessExtend : list ) {
                    for( String sel : lessExtend.getSelectors() ) {
                        selectorList.add( sel );
                    }
                }
            }
            SelectorTokenizer tokenizer = new SelectorTokenizer( selector );
            do {
                String token = tokenizer.next();
                if( token == null ) {
                    break;
                }
                list = all.get( token );
                if( list != null ) {
                    for( LessExtend lessExtend : list ) {
                        for( String extendingSelector : lessExtend.getExtendingSelectors() ) {
                            if( selector.contains( extendingSelector ) ) {
                                for( String replace : lessExtend.getSelectors() ) {
                                    selectorList.add( selector.replace( extendingSelector, replace ) );
                                }
                            }
                        }
                    }
                }
            } while( true );
        }

        return SelectorUtils.concatenate( selectors, selectorList );
    }
}
