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

import java.util.Map;

/**
 * A single match of a mixin to a rule.
 */
class MixinMatch {

    private Rule                        rule;

    private Map<String, Expression>     mixinParameters;

    private boolean                     guard;

    private boolean                     wasDefault;

    /**
     * Result of an MixinMatch
     * @param rule the mixin
     * @param mixinParameters the calling parameters
     * @param guard if the guard match
     * @param wasDefault if there is a default() guard function
     */
    MixinMatch( Rule rule, Map<String, Expression> mixinParameters, boolean guard, boolean wasDefault ) {
        this.rule = rule;
        this.mixinParameters = mixinParameters;
        this.guard = guard;
        this.wasDefault = wasDefault;
    }

    /**
     * The rule of the match.
     * @return the rule
     */
    Rule getRule() {
        return rule;
    }

    /**
     * The calling parameters 
     * @return the parameters
     */
    Map<String, Expression> getMixinParameters() {
        return mixinParameters;
    }

    /**
     * if the guard is TRUE
     * @return if true
     */
    boolean getGuard() {
        return guard;
    }

    /**
     * If the guard of this mixin use the default function
     * @return true, if default function is used
     */
    boolean wasDefault() {
        return wasDefault;
    }
}
