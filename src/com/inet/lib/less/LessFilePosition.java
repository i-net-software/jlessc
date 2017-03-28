/**
 * MIT License (MIT)
 *
 * Copyright (c) 2017 Herr Ritschwumm
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
 * @author Herr Ritschwumm
 * @license: The MIT license <http://opensource.org/licenses/MIT>
 */
package com.inet.lib.less;

import java.util.Objects;

/** the Position of an error in a less file */
public final class LessFilePosition {
    private String filename;

    private int    line;

    private int    column;

    /**
     * filename may be null to indicate a file name is unavailable
     * 
     * @param filename
     *            the less file, can be null if a string was parsed
     * @param line
     *            the line number in the less file
     * @param column
     *            the column in the less file
     */
    public LessFilePosition( String filename, int line, int column ) {
        this.filename = filename;
        this.line = line;
        this.column = column;
    }

    /**
     * Get the file name.
     * 
     * @return the file name or null if in memory
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Get the line in the source file
     * 
     * @return the line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Get the column in the source file.
     * 
     * @return the column number
     */
    public int getColumn() {
        return column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object o ) {
        if( !(o instanceof LessFilePosition) ) {
            return false;
        }
        final LessFilePosition that = (LessFilePosition)o;
        return Objects.equals( filename, that.filename ) && this.line == that.line && this.column == that.column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (filename == null ? 0 : filename.hashCode()) ^ line ^ column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (filename != null ? filename + ":" : "") + line + ":" + column;
    }
}
