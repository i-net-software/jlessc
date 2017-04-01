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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converter for less regular expressions and Java regular expressiosn
 */
class RegExp {

    // In JS syntax, a \ in the replacement string has no special meaning.
    // In Java syntax, a \ in the replacement string escapes the next character,
    // so we have to translate \ to \\ before passing it to Java.
    private static final Pattern REPLACEMENT_BACKSLASH                 = Pattern.compile( "\\\\" );

    // To get \\, we have to say \\\\\\\\:
    // \\\\\\\\ --> Java string unescape --> \\\\
    // \\\\ ---> Pattern replacement unescape in replacement preprocessing --> \\
    private static final String  REPLACEMENT_BACKSLASH_FOR_JAVA        = "\\\\\\\\";

    // In JS syntax, a $& in the replacement string stands for the whole match.
    // In Java syntax, the equivalent is $0, so we have to translate $& to
    // $0 before passing it to Java. However, we have to watch out for $$&, which
    // is actually a Javascript $$ (see below) followed by a & with no special
    // meaning, and must not get translated.
    private static final Pattern REPLACEMENT_DOLLAR_AMPERSAND          = Pattern.compile( "((?:^|\\G|[^$])(?:\\$\\$)*)\\$&" );

    private static final String  REPLACEMENT_DOLLAR_AMPERSAND_FOR_JAVA = "$1\\$0";

    // In JS syntax, a $` and $' in the replacement string stand for everything
    // before the match and everything after the match.
    // In Java syntax, there is no equivalent, so we detect and reject $` and $'.
    // However, we have to watch out for $$` and $$', which are actually a JS $$
    // (see below) followed by a ` or ' with no special meaning, and must not be
    // rejected.
    private static final Pattern REPLACEMENT_DOLLAR_APOSTROPHE         = Pattern.compile( "(?:^|[^$])(?:\\$\\$)*\\$[`']" );

    // In JS syntax, a $$ in the replacement string stands for a (single) dollar
    // sign, $.
    // In Java syntax, the equivalent is \$, so we have to translate $$ to \$
    // before passing it to Java.
    private static final Pattern REPLACEMENT_DOLLAR_DOLLAR             = Pattern.compile( "\\$\\$" );

    // To get \$, we have to say \\\\\\$:
    // \\\\\\$ --> Java string unescape --> \\\$
    // \\\$ ---> Pattern replacement unescape in replacement preprocessing --> \$
    private static final String  REPLACEMENT_DOLLAR_DOLLAR_FOR_JAVA    = "\\\\\\$";

    private boolean              global;

    private final Pattern        pattern;

    /**
     * Create an new instance.
     * @param pattern the regular expression pattern
     * @param flags some flags
     * @throws ParameterOutOfBoundsException if the flags are invalid
     */
    RegExp( String pattern, String flags ) throws ParameterOutOfBoundsException {
        int patternFlags = Pattern.UNIX_LINES;
        for( int i = 0; i < flags.length(); i++ ) {
            char flag = flags.charAt( i );
            switch( flag ) {
                case 'g':
                    global = true;
                    break;
                case 'i':
                    patternFlags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                    break;
                case 'm':
                    patternFlags |= Pattern.MULTILINE;
                    break;
                default:
                    throw new ParameterOutOfBoundsException();
            }
        }
        this.pattern = Pattern.compile( pattern, patternFlags );
    }

    /**
     * Replace the matches in the input with the replacement.
     * @param input the input string
     * @param replacement the replacement
     * @return the resulting string
     * @throws ParameterOutOfBoundsException if Java can not replace it like Javascript
     */
    public String replace( String input, String replacement ) throws ParameterOutOfBoundsException {
        // Replace \ in the replacement with \\ to escape it for Java replace.
        replacement = REPLACEMENT_BACKSLASH.matcher( replacement ).replaceAll( REPLACEMENT_BACKSLASH_FOR_JAVA );

        // Replace the Javascript-ese $& in the replacement with Java-ese $0, but
        // watch out for $$&, which should stay $$&, to be changed to \$& below.
        replacement = REPLACEMENT_DOLLAR_AMPERSAND.matcher( replacement ).replaceAll( REPLACEMENT_DOLLAR_AMPERSAND_FOR_JAVA );

        // Test for Javascript-ese $` and $', which we do not support in the pure
        // Java version.
        if( REPLACEMENT_DOLLAR_APOSTROPHE.matcher( replacement ).find() ) {
            throw new ParameterOutOfBoundsException();
        }

        // Replace the Javascript-ese $$ in the replacement with Java-ese \$.
        replacement = REPLACEMENT_DOLLAR_DOLLAR.matcher( replacement ).replaceAll( REPLACEMENT_DOLLAR_DOLLAR_FOR_JAVA );

        Matcher matcher = pattern.matcher( input );
        return global ? matcher.replaceAll( replacement ) : matcher.replaceFirst( replacement );
    }
}
