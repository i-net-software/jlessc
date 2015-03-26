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
 * Some utilities methods.
 */
class SelectorUtils {

    static String[] merge( String[] mainSelector, String[] base ) {
        int count = 0;

        // counting the & characters and calculate the resulting selectors
        int[] counts = new int[base.length];
        for( int j = 0; j < base.length; j++ ) {
            String selector = base[j];
            int andCount = 0;
            int idx = -1;
            while( (idx = selector.indexOf( '&', idx + 1 )) >= 0 ) {
                andCount++;
            }
            count += counts[j] = (int)Math.pow( mainSelector.length, Math.max( 1, andCount ) );
        }

        String[] sel = new String[count];
        for( int j = 0, t = 0; j < base.length; j++ ) {
            String selector = base[j];
            int idx = selector.lastIndexOf( '&' );
            if( idx < 0 ) {
                for( int m = 0; m < mainSelector.length; m++ ) {
                    sel[t++] = mainSelector[m] + ' ' + selector;
                }
            } else {
                int off = t;
                count = counts[j];
                do {
                    int a = (t - off);
                    int idx2 = idx;
                    selector = base[j];
                    do {
                        int x = a % mainSelector.length;
                        selector = selector.substring( 0, idx2 ) + mainSelector[x] + selector.substring( idx2 + 1 );
                        a /= mainSelector.length;
                    } while( (idx2 = selector.lastIndexOf( '&', idx2 - 1 )) >= 0 );
                    sel[t++] = selector;
                } while( (t - off) < count );
            }
        }
        return sel;
    }

    static void appendToWithPlaceHolder( CssFormatter formatter, String str, int i, LessObject caller ) {
        int appendIdx = 0;
        char quote = 0;
        for( ; i < str.length(); i++ ) {
            char ch = str.charAt( i );
            switch( ch ) {
                case '\"':
                case '\'':
                    if( quote == 0 ) {
                        quote = ch;
                    } else {
                        quote = 0;
                    }
                    break;
                case '@':
                    String name;
                    int nextIdx;
                    if( str.length() > i + 1 && str.charAt( i + 1 ) == '{' ) {
                        nextIdx = str.indexOf( '}', i );
                        name = '@' + str.substring( i + 2, nextIdx );
                        nextIdx++;
                    } else {
                        if( quote != 0 ) {
                            break;
                        }
                        LOOP: for( nextIdx = i + 1; nextIdx < str.length(); nextIdx++ ) {
                            ch = str.charAt( nextIdx );
                            switch( ch ) {
                                case ' ':
                                case ')':
                                case ',':
                                case '\"':
                                case '\'':
                                    break LOOP;
                            }
                        }
                        name = str.substring( i, nextIdx );
                    }

                    formatter.append( str.substring( appendIdx, i ) );
                    appendIdx = nextIdx;

                    Expression exp = formatter.getVariable( name );
                    if( exp == null ) {
                        throw caller.createException( "Undefine Variable: " + name );
                    }
                    formatter.setInineMode( true );
                    exp.appendTo( formatter );
                    formatter.setInineMode( false );

                    break;
            }
        }
        formatter.append( str.substring( appendIdx ) );
    }

}
