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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith( Parameterized.class )
public class LessTest {

    private File lessFile;
    private File cssFile;

    public LessTest( String name, File lessFile, File cssFile ) {
        this.lessFile = lessFile;
        this.cssFile = cssFile;
    }

    @Parameters( name = "{0}" )
    public static List<Object[]> params() throws Exception {
        ArrayList<Object[]> params = new ArrayList<>();
        URL url = LessTest.class.getResource("samples");
        File dir = new File(url.toURI());
        int baseLength = dir.getPath().length() + 1;
        params( params, dir, baseLength );
        return params;
    }

    private static void params( ArrayList<Object[]> params, File dir, int baseLength ) {
        for( File file : dir.listFiles() ) {
            if( file.getName().endsWith( ".less" ) ) {
                String basename = file.getName();
                basename = basename.substring( 0, basename.length() - 5 );
                String cssname = basename + ".css";
                File cssfile = new File( file.getParent(), cssname );
                if( cssfile.exists() ) {
                    params.add( new Object[] { cssfile.getPath().substring( baseLength ), file, cssfile } );
                }
                cssname = basename + ".css_x";
                cssfile = new File( file.getParent(), cssname );
                if( cssfile.exists() ) {
                    params.add( new Object[] { cssfile.getPath().substring( baseLength ), file, cssfile } );
                }
            } else if( file.isDirectory() ) {
                params( params, file, baseLength );
            }
        }
    }

    @Test
    public void compile() throws Exception {
        URI uri = lessFile.toURI();
        String lessData = new String( Files.readAllBytes( Paths.get( uri ) ), StandardCharsets.UTF_8 );
        String cssData = new String( Files.readAllBytes( Paths.get( cssFile.toURI() ) ), StandardCharsets.UTF_8 );

        boolean compress = cssFile.getName().endsWith( ".css_x" ) || lessFile.getParentFile().getName().equals( "compression" );
        assertEquals( cssData, Less.compile( uri.toURL(), lessData, compress ) );
    }
}
