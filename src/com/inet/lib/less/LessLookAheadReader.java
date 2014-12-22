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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * A reader with some special look ahead reading.
 */
class LessLookAheadReader extends LessObject implements Closeable {

    private final Reader        reader;

    private final StringBuilder cache = new StringBuilder();

    private int                 cachePos;

    LessLookAheadReader( Reader reader, String fileName ) {
        super( fileName );
        this.reader = reader;
        line = 1;
        column = 0;
    }

    int nextBlockMarker() {
        cache.setLength( cachePos = 0 );
        int parenthesis = 0;
        boolean isSlash = false;
        try {
            for( ;; ) {
                int ch = reader.read();
                if( ch < 0 ) {
                    for( int i = 0; i < cache.length(); i++ ) {
                        if( !Character.isWhitespace( cache.charAt( i ) ) ) {
                            return ';'; // a not terminated line is like a lime with semicolon
                        }
                    }
                    return -1;
                }
                cache.append( (char)ch );
                switch( ch ) {
                    case '/':
                        if( isSlash ) {
                            if( parenthesis > 0 && cache.indexOf( "url" ) > 0 ) {
                                break; // url function with unquoted url like url(http://xyz)
                            }
                            cache.setLength( cache.length() - 2 );
                            skipLine();
                            ch = 0;
                        }
                        break;
                    case '*':
                        if( isSlash ) {
                            boolean isAsterix = false;
                            for( ;; ) {
                                ch = reader.read();
                                if( ch < 0 ) {
                                    throw createException( "Unrecognized input: '" + cache.toString().trim() + "'" );
                                }
                                cache.append( (char)ch );
                                if( ch == '/' && isAsterix ) {
                                    ch = 0;
                                    break;
                                }
                                isAsterix = ch == '*';
                            }
                        }
                        break;
                    case '{':
                    case '}':
                    case ';':
                        if( parenthesis == 0 ) {
                            return ch;
                        }
                        break;
                    case '(':
                        parenthesis++;
                        break;
                    case ')':
                        if( --parenthesis < 0 ) {
                            throw createException( "Unrecognized input: '" + cache.toString().trim() + "'" );
                        }
                        break;
                    case '"':
                    case '\'':
                        int ch2;
                        isSlash = false;
                        for( ;; ) {
                            ch2 = reader.read();
                            if( ch2 < 0 ) {
                                return ';'; // a not terminated line is like a lime with semicolon
                            }
                            cache.append( (char)ch2 );
                            if( ch == ch2 && !isSlash ) {
                                break;
                            }
                            isSlash = ch2 == '\\';
                        }
                        break;
                    case '\\':
                        cache.append( (char)reader.read() );
                        break;
                    default:
                }
                isSlash = ch == '/';
            }
        } catch( IOException ex ) {
            throw new LessException( ex );
        }
    }
    
    String getLookAhead(){
        return cache.toString();
    }


    /**
     * If the next data which are already in the cache are a mixin parameter or part of a selector name.
     * This is call after a left parenthesis.
     * Samples for selectors:<ul>
     * <li>(min-resolution: 192dpi)
     * <li>(.clearfix all)
     * <li>(audio:not([controls]))
     * <li>(odd)
     * </ul>
     * Samples for mixin params<ul>
     * <li>(.65)
     * <li>(@color)
     * <li>()
     * </ul>
     * @param isBlock selector of a block or a semicolon line
     * @return true, if it is a mixin parameter
     */
    boolean nextIsMixinParam( boolean isBlock ) {
        boolean isFirst = true;
        for( int i=cachePos; i < cache.length(); i++ ) {
            char ch = cache.charAt( i );
            switch( ch ) {
                case ')':
                    return isFirst || !isBlock;
                case '@':
                case '~':
                    return true;
                case '"':
                    return !isBlock;
                case '.':
                    if( !isFirst ) {
                        continue;
                    } else {
                        if( Character.isDigit( cache.charAt( i + 1 ) ) ) { //Number with a starting point
                            return true;
                        }
                    }
                    return false;
                case ':':
                case '[':
                    return false;
                case ' ':
                    continue;
                default:
                    isFirst = false;
            }
        }
        return false;
    }

    /**
     * Read a single character from reader or from back buffer
     * 
     * @return a character or -1 if EOF
     * @throws LessException
     *             If an I/O error occurs
     */
    char read() {
        try {
            if( cachePos < cache.length() ) {
                return incLineColumn( cache.charAt( cachePos++ ) );
            }
            int ch = reader.read();
            if( ch == -1 ) {
                throw createException( "Unexpected end of Less data" );
            }
            return incLineColumn( ch );
        } catch( IOException ex ) {
            throw new LessException( ex );
        }
    }

    /**
     * Push a char back to the stream
     * 
     * @param ch
     *            the char
     */
    void back( char ch ) {
        cachePos--;
        cache.setCharAt( cachePos, ch );
    }
    
    /**
     * Skip all data until a newline occur or an EOF
     */
    void skipLine() {
        int ch;
        do {
            try {
                ch = reader.read();
            } catch( IOException ex ) {
                throw new LessException( ex );
            }
            incLineColumn( ch );
        } while( ch != '\n' && ch != -1 );
    }

    private char incLineColumn( int ch ) {
        if( ch == '\n' ) {
            line++;
            column = 0;
        } else {
            column++;
        }
        return (char)ch;
    }

    int getLine() {
        return line;
    }

    int getColumn() {
        return column;
    }

    String getFileName() {
        return filename;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
