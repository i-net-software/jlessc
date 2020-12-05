/**
 * MIT License (MIT)
 *
 * Copyright (c) 2019 Volker Berlin
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;

import org.junit.Test;

public class RewriteUrlsTest {

    private void test( String rewriteUrl ) throws IOException {
        Less less = new Less();

        URL baseURL = getClass().getResource( "RewriteUrls/main.less" );

        String lessData = new Scanner( baseURL.openStream(), "UTF-8" ).useDelimiter( "\\A" ).next();

        HashMap<String, String> options = new HashMap<>();
        options.put( Less.REWRITE_URLS, rewriteUrl );

        String expectedCss = new Scanner( new URL( baseURL, rewriteUrl + ".css" ).openStream(), "UTF-8" ).useDelimiter( "\\A" ).next();
        expectedCss = expectedCss.replace( "\r\n", "\n" ); // JLess always uses Linux newlines because it's more compact

        assertEquals( expectedCss, less.compile( baseURL, lessData, options ) );
    }

    @Test
    public void off() throws IOException {
        test( "off" );
    }

    @Test
    public void local() throws IOException {
        test( "local" );
    }

    @Test
    public void all() throws IOException {
        test( "all" );
    }
}
