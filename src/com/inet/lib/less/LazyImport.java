/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 -2015 Volker Berlin
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

import java.net.URL;
import java.util.HashMap;

/**
 * Hold the context for a lazy import for filenames with variables.
 */
class LazyImport extends ValueExpression {

    private final URL                         baseURL;

    private final HashMap<String, Expression> variables;

    private final Formattable                 lastRuleBefore;

    /**
     * Create a new instance.
     * @param obj another LessObject with parse position.
     * @param baseURL current baseURL
     * @param filename value of the filename, can contain place holders.
     * @param variables variables
     * @param lastRuleBefore pointer to the rules where the import should be included.
     */
    LazyImport( LessObject obj, URL baseURL, String filename, HashMap<String, Expression> variables, Formattable lastRuleBefore ) {
        super( obj, filename );
        this.baseURL = baseURL;
        this.variables = variables;
        this.lastRuleBefore = lastRuleBefore;
    }

    /**
     * The base url or the url of the parent less file.
     * 
     * @return the url or null
     */
    URL getBaseUrl() {
        return baseURL;
    }

    /**
     * Get the variables with default values before the import.
     * 
     * @return the variables
     */
    HashMap<String, Expression> getVariables() {
        return variables;
    }

    /**
     * Get the last rule before the import. New rules must added after this position.
     * 
     * @return a formattable
     */
    Formattable lastRuleBefore() {
        return lastRuleBefore;
    }
}
