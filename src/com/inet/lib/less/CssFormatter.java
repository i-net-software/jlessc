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

    private Appendable                                   output;

    private LessExtendMap                                lessExtends;

    private StringBuilder                                insets         = new StringBuilder();

    private final ArrayList<HashMap<String, Expression>> variablesStack = new ArrayList<>();

    private final ArrayList<Rule>                        rulesStack     = new ArrayList<>();

    private int                                          rulesStackModCount;

    private boolean                                      important;

    private boolean                                      inlineMode;

    private final DecimalFormat                          decFormat      = new DecimalFormat( "#.########", DecimalFormatSymbols.getInstance( Locale.ENGLISH ) );

    CssFormatter( Appendable output ) {
        this( output, false );
    }

    CssFormatter( Appendable output, boolean toString ) {
        this.output = output;
        if( toString ) {
            lessExtends = new LessExtendMap();
        }
    }

    void format( LessParser parser ) throws IOException {
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
    }

    /**
     * Replace the output target and returns the current output.
     * 
     * @param newOutput
     *            new output
     * @return old output
     */
    Appendable swapOutput( Appendable newOutput ) {
        Appendable temp = this.output;
        this.output = newOutput;
        return temp;
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

    CssFormatter append( String str ) throws IOException {
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

    CssFormatter appendColor( int value, String hint ) throws IOException {
        if( !inlineMode && hint != null ) {
            output.append( hint );
        } else {
            output.append( '#' );
            String hex = Integer.toHexString( value & 0xFFFFFF );
            for( int i = hex.length(); i < 6; i++ ) {
                output.append( '0' );
            }
            output.append( hex );
        }
        return this;
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
