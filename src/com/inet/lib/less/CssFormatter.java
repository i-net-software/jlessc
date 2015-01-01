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

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * A formatter for the CSS output. Hold some formting states.
 */
class CssFormatter {

    private final static char[]                          DIGITS         = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final ArrayList<StringBuilder>               outputs = new ArrayList<>();

    private int                                          outputIdx = -1;

    private StringBuilder                                output;

    private LessExtendMap                                lessExtends;

    private StringBuilder                                insets         = new StringBuilder();

    private final ArrayList<HashMap<String, Expression>> variablesStack = new ArrayList<>();

    private final ArrayList<Rule>                        rulesStack     = new ArrayList<>();

    private int                                          rulesStackModCount;

    private boolean                                      important;

    private boolean                                      inlineMode;

    private final DecimalFormat                          decFormat      = new DecimalFormat( "#.########", DecimalFormatSymbols.getInstance( Locale.ENGLISH ) );

    CssFormatter() {
        this( false );
    }

    CssFormatter( boolean toString ) {
        if( toString ) {
            lessExtends = new LessExtendMap();
        }
    }

    void format( LessParser parser, Appendable appendable ) throws IOException {
        addOutput();
        lessExtends = parser.getExtends();
        addVariables( parser.getVariables() );
        for( Formattable rule : parser.getRules() ) {
            if( rule.getClass() == Mixin.class ) {
                ((Mixin)rule).appendSubRules( null, this );
            } else {
                rule.appendTo( this );
            }
        }
        removeVariables( parser.getVariables() );
        appendable.append( output );
    }

    /**
     * Add a new output buffer to the formatter.
     */
    void addOutput() {
        if( ++outputIdx == outputs.size() ) {
            output = new StringBuilder();
            outputs.add( output );
        } else {
            output = outputs.get( outputIdx );
            output.setLength( 0 );
        }
    }

    /**
     * Release an output and delete it.
     */
    void freeOutput() {
        output = outputs.get( --outputIdx );
    }

    /**
     * Release an output buffer, return the content and restore the previous output. 
     * @return the content of the current output
     */
    String releaseOutput() {
        String str = output.toString();
        freeOutput();
        return str;
    }

    /**
     * Release an output buffer, restore the previous output and add the content of the previous output.
     */
    void flushOutput() {
        StringBuilder current = output;
        freeOutput();
        output.append( current );
    }

    /**
     * Get the size of the curent content in the current output.
     * @return the size
     */
    int getOutputSize() {
        return output.length();
    }

    String[] concatenateExtends( String[] selectors ) {
        return lessExtends.concatenateExtends( selectors );
    }

    /**
     * Get a variable expression from the current stack
     * 
     * @param name
     *            the name of the variable starting with @
     * @return the expression or null if not found
     */
    Expression getVariable( String name ) {
        for( int i = variablesStack.size() - 1; i >= 0; i-- ) {
            HashMap<String, Expression> variables = variablesStack.get( i );
            Expression variable = variables.get( name );
            if( variable != null ) {
                return variable;
            }
        }
        return null;
    }

    /**
     * Add variables to the stack.
     * 
     * @param variables
     *            the variables, can be null if the current mixin has no variables.
     */
    void addVariables( HashMap<String, Expression> variables ) {
        if( variables != null ) {
            variablesStack.add( variables );
        }
    }

    /**
     * Remove variables from the stack.
     * 
     * @param variables
     *            the variables, can be null if the current mixin has no variables.
     */
    void removeVariables( HashMap<String, Expression> variables ) {
        if( variables != null ) {
            variablesStack.remove( variablesStack.size() - 1 );
        }
    }

    void addRule( Rule rule ) {
        rulesStack.add( rule );
        rulesStackModCount++;
    }

    void removeRule( Rule rule ) {
        rulesStack.remove( rulesStack.size() - 1 );
        rulesStackModCount++;
    }

    /**
     * A mixin inline never it self.
     * 
     * @param rule
     * @return true, if the mixin is currently formatting
     */
    boolean containsRule( Rule rule ) {
        return rulesStack.contains( rule );
    }

    /**
     * Get a nested mixin of a parent rule.
     * 
     * @param name
     *            the name of the mixin
     * @return the mixin or null
     */
    List<Rule> getMixin( String name ) {
        for( int i = rulesStack.size() - 1; i >= 0; i-- ) {
            List<Rule> rules = rulesStack.get( i ).getMixin( name );
            if( rules != null ) {
                for( int r = 0; r < rules.size(); r++ ) {
                    if( !rulesStack.contains( rules.get( r ) ) ) {
                        return rules;
                    }
                }
            }
        }
        return null;
    }

    int stackID() {
        return rulesStackModCount;
    }

    void setInineMode( boolean mode ) {
        inlineMode = mode;
    }

    boolean inlineMode() {
        return inlineMode;
    }

    CssFormatter append( String str ) {
        if( inlineMode && str.length() > 1 ) {
            char ch = str.charAt( 0 );
            if( ch == '\'' || ch == '\"' ) {
                if( str.charAt( str.length() - 1 ) == ch ) {
                    str = str.substring( 1, str.length() - 1 );
                }
            }
        }
        output.append( str );
        return this;
    }

    CssFormatter appendColor( double color, String hint ) throws IOException {
        if( !inlineMode && hint != null ) {
            output.append( hint );
        } else {
            int argb = ColorUtils.argb( color );
            output.append( '#' );
            appendHex( argb, 6 );
        }
        return this;
    }

    void appendHex( int value, int digits ) throws IOException {
        if( digits > 1 ) {
            appendHex( value >>> 4, digits-1 );
        }
        output.append( DIGITS[ value & 0xF ] );
    }

    CssFormatter append( char ch ) throws IOException {
        output.append( ch );
        return this;
    }

    CssFormatter append( double value ) throws IOException {
        if( value == (int)value ) {
            output.append( Integer.toString( (int)value ) );
        } else {
            output.append( decFormat.format( value ) );
        }
        return this;
    }

    CssFormatter appendValue( double value, String unit ) throws IOException {
        return append( value ).append( unit );
    }

    CssFormatter appendSelector( String selector ) throws IOException {
        return insets().append( selector );
    }

    CssFormatter startBlock() throws IOException {
        space();
        output.append( '{' );
        newline();
        insets.append( "  " );
        return this;
    }

    CssFormatter endBlock() throws IOException {
        insets.setLength( insets.length() - 2 );
        insets();
        output.append( '}' );
        newline();
        return this;
    }

    void appendProperty( String name, Expression value ) throws IOException {
        insets();
        output.append( name ).append( ':' );
        space();
        value.appendTo( this );
        if( important ) {
            output.append( " !important" );
        }
        semicolon().newline();
    }

    void setImportant( boolean important ) {
        this.important = important;
    }

    CssFormatter space() throws IOException {
        output.append( ' ' );
        return this;
    }

    CssFormatter newline() throws IOException {
        output.append( '\n' );
        return this;
    }

    CssFormatter semicolon() throws IOException {
        output.append( ';' );
        return this;
    }

    CssFormatter insets() throws IOException {
        output.append( insets );
        return this;
    }

    CssFormatter comment( String msg ) throws IOException {
        output.append( insets ).append( msg ).append( '\n' );
        return this;
    }

    /**
     * Get a shared decimal format for parsing numbers with units.
     * 
     * @return the format
     */
    DecimalFormat getFormat() {
        return decFormat;
    }
}
