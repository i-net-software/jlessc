/**
 * MIT License (MIT)
 *
 * Copyright (c) 2016 Volker Berlin
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * A factory to create a reader and streams for parsing. You can override it to resolve the URL, implement a cache or use another
 * encoding as UFT-8.
 */
public class ReaderFactory {

    /**
     * Open an InputStream for the given URL.
     * 
     * @param url
     *            the url, not null
     * @return the stream, never null
     * @throws IOException
     *             If any I/O error occur on reading the URL.
     */
    public InputStream openStream( URL url ) throws IOException {
        return url.openStream();
    }

    /**
     * Create a Reader for the given URL.
     * 
     * @param url
     *            the url, not null
     * @return the reader, never null
     * @throws IOException
     *             If any I/O error occur on reading the URL.
     */
    public Reader create( URL url ) throws IOException {
        return new InputStreamReader( openStream( url ), StandardCharsets.UTF_8 );
    }
}
