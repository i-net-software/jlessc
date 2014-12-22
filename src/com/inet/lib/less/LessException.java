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

/**
 * A exception that occur if some can not parse or converted.
 */
class LessException extends RuntimeException {

    private String msg;

    LessException( String msg ) {
        super( msg );
        this.msg = msg;
    }

    LessException( Throwable th ) {
        super( th );
        this.msg = super.getMessage();
    }

    void addPosition( String filename, int line, int column ) {
        StringBuilder builder = new StringBuilder();
        builder.append( " on line " ).append( line ).append( ", column " ).append( column );
        if( filename != null ) {
            builder.append( ", file " ).append( filename );
        }
        if( !msg.contains( builder ) ) {
            msg += "\n\t" + builder;
        }
    }

    @Override
    public String getMessage() {
        return msg;
    }
}
