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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The main class of JLessC library. Its contain all start points for converting LESS to CSS files.
 */
public class Less {

    /**
     * Compile the less data from a string.
     * 
     * @param baseURL
     *            the baseURL for import of external less data.
     * @param lessData
     *            the input less data
     * @param compress
     *            true, if the CSS data should be compressed without any extra formating characters.
     * @return the resulting less data
     */
    public static String compile( URL baseURL, String lessData, boolean compress ) {
        LessParser parser = new LessParser();
        parser.parse( baseURL, new StringReader( lessData ) );

        StringBuilder builder = new StringBuilder();
        CssFormatter formatter = compress ? new CompressCssFormatter() :  new CssFormatter();
        parser.parseLazy( formatter );
        try {
            formatter.format( parser, baseURL, builder );
        } catch( LessException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new LessException( ex );
        }
        return builder.toString();
    }

    /**
     * Compile the less data from a file.
     * 
     * @param lessFile
     *            the less file
     * @param compress
     *            true, if the CSS data should be compressed without any extra formating characters.
     * @return the resulting less data
     * @throws IOException
     *             if an I/O error occurs reading from the less file
     */
    public static String compile( File lessFile, boolean compress ) throws IOException {
        String lessData = new String( Files.readAllBytes( lessFile.toPath() ), StandardCharsets.UTF_8 );
        return Less.compile( lessFile.toURI().toURL(), lessData, compress );
    }
}
