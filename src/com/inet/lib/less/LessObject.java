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

import javax.annotation.Nonnull;

/**
 * A base object for the parser that hold a parse position.
 */
class LessObject {

    String filename;

    int    line, column;

    /**
     * Only used from LessLookAheadReader
     * @param filename the name of the less file, can be null if a string is parsed.
     */
    LessObject( String filename ) {
        this.filename = filename;
    }

    /**
     * Create a new instance with filename, line number and column position from the LessObject.
     * 
     * @param obj
     *            another LessObject with parse position.
     */
    LessObject( LessObject obj ) {
        this.filename = obj.filename;
        this.line = obj.line;
        this.column = obj.column;
    }

    /**
     * Create a LessException with filename, line number and column of the current object.
     * @param msg the error message.
     * @return the exception
     */
    @Nonnull
    LessException createException( String msg ) {
        LessException lessEx = new LessException( msg );
        lessEx.addPosition( filename, line, column );
        return lessEx;
    }

    /**
     * Create a LessException with filename, line number and column of the current object.
     * @param msg the error message.
     * @param cause the cause
     * @return the exception
     */
    @Nonnull
    LessException createException( String msg, Throwable cause ) {
        LessException lessEx = new LessException( msg, cause );
        lessEx.addPosition( filename, line, column );
        return lessEx;
    }

    /**
     * If cause is already a LessException then filename, line number and column of the current object are added to the less stacktrace.
     * With any other type of exception a new LessException is created.
     * @param cause the cause
     * @return the exception
     */
    @Nonnull
    LessException createException( Throwable cause ) {
        LessException lessEx = cause.getClass() == LessException.class ? (LessException)cause : new LessException( cause );
        lessEx.addPosition( filename, line, column );
        return lessEx;
    }

    /**
     * Get the file name in which the current object is define.
     * @return the filename, can be null if a string was parsed.
     */
    String getFileName() {
        return filename;
    }
}
