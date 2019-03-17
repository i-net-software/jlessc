/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 - 2019 Volker Berlin
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
import java.util.Collections;
import java.util.Map;

/**
 * The main class of JLessC library. Its contain all start points for converting LESS to CSS files.
 */
public class Less {

    /**
     * Key for compress option. true, if the CSS data should be compressed without any extra formating characters.
     */
    public static final String COMPRESS     = "compress";

    /**
     * Rewrites URLs to make them relative to the base less file. 'all' rewrites all URLs, 'local' just those starting
     * with a '.', 'off' simple inline it.
     */
    public static final String REWRITE_URLS = "rewrite-urls";

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
     * @throws LessException 
     *            if any error occur on compiling.
     */
    public static String compile( URL baseURL, String lessData, boolean compress ) throws LessException {
        return compile( baseURL, lessData, compress, new ReaderFactory() );
    }

    /**
     * Compile the less data from a string.
     * 
     * @param baseURL
     *            the baseURL for import of external less data.
     * @param lessData
     *            the input less data
     * @param compress
     *            true, if the CSS data should be compressed without any extra formating characters.
     * @param readerFactory
     *            A factory for the readers for imports.
     * @return the resulting less data
     * @throws LessException 
     *            if any error occur on compiling.
     */
    public static String compile( URL baseURL, String lessData, boolean compress, ReaderFactory readerFactory ) throws LessException {
        Map<String,String> params = Collections.singletonMap( COMPRESS, Boolean.toString( compress ) );
        return compile( baseURL, lessData, params, readerFactory );
    }

    /**
     * Compile the less data from a string.
     * 
     * @param baseURL
     *            the baseURL for import of external less data.
     * @param lessData
     *            the input less data
     * @param options
     *            some optional options, see constants for details
     * @return the resulting less data
     * @throws LessException 
     *            if any error occur on compiling.
     */
    public static String compile( URL baseURL, String lessData,  Map<String, String> options ) throws LessException {
        return compile( baseURL, lessData, options, new ReaderFactory() );
    }

    /**
     * Compile the less data from a string.
     * 
     * @param baseURL
     *            the baseURL for import of external less data.
     * @param lessData
     *            the input less data
     * @param options
     *            some optional options, see constants for details
     * @param readerFactory
     *            A factory for the readers for imports.
     * @return the resulting less data
     * @throws LessException
     *             if any error occur on compiling.
     */
    public static String compile( URL baseURL, String lessData, Map<String, String> options, ReaderFactory readerFactory ) throws LessException {
        try {
            if( options == null ) {
                options = Collections.emptyMap();
            }
            LessParser parser = new LessParser();
            parser.parse( baseURL, new StringReader( lessData ), readerFactory );

            boolean compress = Boolean.parseBoolean( options.get( COMPRESS ) );
            CssFormatter formatter = compress ? new CompressCssFormatter() : new CssFormatter();
            parser.parseLazy( formatter );
            StringBuilder builder = new StringBuilder();
            formatter.format( parser, baseURL, readerFactory, builder, options );
            return builder.toString();
        } catch( LessException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new LessException( ex );
        }
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
        return Less.compile( lessFile.toURI().toURL(), lessData, compress, new ReaderFactory() );
    }

    /**
     * Compile the less data from a file.
     * 
     * @param lessFile
     *            the less file
     * @param compress
     *            true, if the CSS data should be compressed without any extra formating characters.
     * @param readerFactory
     *            A factory for the readers for imports.
     * @return the resulting less data
     * @throws IOException
     *             if an I/O error occurs reading from the less file
     */
    public static String compile( File lessFile, boolean compress, ReaderFactory readerFactory ) throws IOException {
        String lessData = new String( Files.readAllBytes( lessFile.toPath() ), StandardCharsets.UTF_8 );
        return Less.compile( lessFile.toURI().toURL(), lessData, compress, readerFactory );
    }
}
