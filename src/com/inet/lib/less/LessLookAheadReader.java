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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * A reader with some special look ahead reading.
 */
class LessLookAheadReader extends LessObject implements Closeable {

    private final Reader        reader;

    private final StringBuilder cache = new StringBuilder();

    private final boolean       isReference;

    private int                 cachePos;

    /**
     * Create a new instance.
     * @param reader the underlying reader
     * @param fileName the filename of the less file or null if a String is parsed.
     * @param isReference true, if the less file is imported as reference
     */
    LessLookAheadReader( Reader reader, String fileName, boolean isReference ) {
        super( fileName );
        this.reader = reader;
        this.isReference = isReference;
        line = 1;
        column = 0;
    }

    /**
     * Get the next parse type. This can be -1, ';', '{' or '}'. It copy the input until this marker in the look ahead
     * cache.
     * 
     * @return the block type of the next data.
     * @throws LessException if any parsing error occur.
     */
    int nextBlockMarker() throws LessException {
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
                        if( cache.length() > 1 && cache.charAt( cache.length() - 2 ) == '@' ) { // @{  --> a inline variable and not a block start
                            do {
                                ch = reader.read();
                                if( ch < 0 ) {
                                    throw createException( "Unrecognized input: '" + cache.toString().trim() + "'" );
                                }
                                cache.append( (char)ch );
                            } while( ch != '}' );
                            break;
                        }
                        //$FALL-THROUGH$
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
                    case '`':
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

    /**
     * The read data from nextBlockMarker(). Is used for an error message only.
     * @return the look ahead cache.
     */
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
     * <li>(@{color})
     * </ul>
     * Samples for mixin params<ul>
     * <li>(.65)
     * <li>(@color)
     * <li>()
     * <li>(...)
     * <li>(red)
     * <li>(1)
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
                    return true;
                case '@':
                    return cache.charAt( i+1 ) != '{';
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
                    if( i + 2 < cache.length() && cache.charAt( i + 1 ) == '.' && cache.charAt( i + 2 ) == '.' ) { // ...
                        return true;
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
        column--; // reverse of incLineColumn()
    }

    /**
     * Skip all data until a newline occur or an EOF
     * 
     * @throws LessException if an IO error occur
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

    /**
     * Increment the line and column count depending on the character.
     * @param ch current character
     * @return the character parameter 
     */
    private char incLineColumn( int ch ) {
        if( ch == '\n' ) {
            line++;
            column = 0;
        } else {
            column++;
        }
        return (char)ch;
    }

    /**
     * The current line number for error messages.
     * @return line number
     */
    int getLine() {
        return line;
    }

    /**
     * The current column number for error messages.
     * @return column number.
     */
    int getColumn() {
        return column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * If the less file of this reader was import with "reference" keyword.
     * @return true, if reference
     */
    boolean isReference() {
        return isReference;
    }
}
