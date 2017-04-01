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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Contains functions which are not in the less standard. 
 */
class CustomFunctions {

    /**
     * Colorize an image and inline it as base64.
     * @param formatter current formatter
     * @param parameters the parameters (url, main_color, contrast_color)
     * @throws IOException if any I/O error occur
     */
    static void colorizeImage( CssFormatter formatter, List<Expression> parameters ) throws IOException {
        if( parameters.size() < 4 ) {
            throw new LessException( "error evaluating function colorize-image expects url, main_color, contrast_color " );
        }

        String relativeURL = parameters.get( 0 ).stringValue( formatter );
        String urlString = parameters.get( 1 ).stringValue( formatter );
        URL url = new URL( formatter.getBaseURL(), relativeURL );
        String urlStr = UrlUtils.removeQuote( urlString );
        url = new URL( url, urlStr );
        int mainColor = ColorUtils.argb( UrlUtils.getColor( parameters.get( 2 ), formatter ) );
        int contrastColor = ColorUtils.argb( UrlUtils.getColor( parameters.get( 3 ), formatter ) );

        BufferedImage loadedImage = ImageIO.read( url.openStream() );

        // convert the image in a standard color model
        int width = loadedImage.getWidth( null );
        int height = loadedImage.getHeight( null );
        BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
        Graphics2D bGr = image.createGraphics();
        bGr.drawImage( loadedImage, 0, 0, null );
        bGr.dispose();

        final float[] mainColorHsb = Color.RGBtoHSB( (mainColor >> 16) & 0xFF, (mainColor >> 8) & 0xFF, mainColor & 0xFF, null );
        final float[] contrastColorHsb = Color.RGBtoHSB( (contrastColor >> 16) & 0xFF, (contrastColor >> 8) & 0xFF, contrastColor & 0xFF, null );

        // get the pixel data
        WritableRaster raster = image.getRaster();
        DataBufferInt buffer = (DataBufferInt)raster.getDataBuffer();
        int[] data = buffer.getData();

        float[] hsb = new float[3];
        int hsbColor = 0;
        int lastRgb = data[0] + 1;
        for( int i = 0; i < data.length; i++ ) {
            int rgb = data[i];
            if( rgb == lastRgb ) {
                data[i] = hsbColor;
                continue;
            }
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            Color.RGBtoHSB( r, g, b, hsb );

            float[] hsbColorize;
            if( hsb[1] == 1.0f ) {
                hsbColorize = hsb;
                hsb[0] = hsb[0] * 3f / 4f + mainColorHsb[0] / 4f;
                hsb[1] = hsb[1] * 3f / 4f + mainColorHsb[1] / 4f;
                hsb[2] = hsb[2] * 3f / 4f + mainColorHsb[2] / 4f;
            } else {
                if( hsb[2] == 1.0f ) {
                    hsbColorize = contrastColorHsb;
                } else {
                    hsbColorize = mainColorHsb;
                }
            }
            lastRgb = rgb;
            hsbColor = Color.HSBtoRGB( hsbColorize[0], hsbColorize[1], hsbColorize[2] );
            hsbColor = (rgb & 0xFF000000) | (hsbColor & 0xFFFFFF);
            data[i] = hsbColor;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write( image, "PNG", out );

        UrlUtils.dataUri( formatter, out.toByteArray(), urlString, "image/png;base64" );
    }
}
