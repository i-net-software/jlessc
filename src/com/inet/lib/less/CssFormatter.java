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

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * A formatter for the CSS output. Hold some formating states.
 */
abstract class CssFormatter {

    private final StringBuilderPool                      pool;

    private final ArrayDeque<StringBuilder>              outputs = new ArrayDeque<>();

    private StringBuilder                                output;

    private URL                                          baseURL;

    private LessExtendMap                                lessExtends;

    private final ArrayList<HashMap<String, Expression>> variablesStack = new ArrayList<>();

    private final ArrayList<HashMap<String, Expression>> mixinReturnStack = new ArrayList<>();

    private int                                          mixinReturnCount;

    private final ArrayList<Rule>                        rulesStack     = new ArrayList<>();

    private int                                          rulesStackModCount;

    private final PlainCssFormatter                      formatter;

    private List<RuleFormatter>                          results = new ArrayList<>();

    boolean                                              charsetDirective;

    CssFormatter( PlainCssFormatter formatter, boolean toString ) {
        this.formatter = formatter;
        if( toString ) {
            lessExtends = new LessExtendMap();
        }
        mixinReturnStack.add( new HashMap<String, Expression>() );
        mixinReturnCount++;
        pool = new StringBuilderPool();
        output = pool.get();
        results.add( new RuleFormatter( this ) ); // header
    }

    CssFormatter( CssFormatter parent ) {
        formatter = parent.formatter;
        pool = parent.pool;
        output = pool.get();
    }

    void format( LessParser parser, URL baseURL, Appendable appendable ) throws IOException {
        this.baseURL = baseURL;
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
        for( RuleFormatter result : results ) {
            result.appendTo( appendable );
        }
        appendable.append( output );
    }

    /**
     * Get the StringBuilder for this formatter 
     * @return the StringBuilderPool
     */
    StringBuilderPool getPool() {
        return pool;
    }

    /**
     * Get the formatter for CSS directives. 
     * @return the header formatter
     */
    CssFormatter getHeader() {
        return results.get( 0 );
    }

    /**
     * Write this to the appendable.
     * @param appendable the target
     * @throws IOException if any I/O error occurs
     */
    void appendTo( Appendable appendable ) throws IOException {
        appendable.append( output );
    }

    /**
     * Add a new output buffer to the formatter.
     */
    void addOutput() {
        outputs.addLast( output );
        output = pool.get();
    }

    /**
     * Release an output and delete it.
     */
    void freeOutput() {
        output = outputs.removeLast();
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
     * Get the size of the current content in the current output.
     * @return the size
     */
    int getOutputSize() {
        return output.length();
    }

    String[] concatenateExtends( String[] selectors ) {
        return lessExtends.concatenateExtends( selectors );
    }

    URL getBaseURL() {
        return baseURL;
    }

    /**
     * Get a variable expression from the current stack
     * 
     * @param name
     *            the name of the variable starting with @
     * @return the expression or null if not found
     */
    Expression getVariable( String name ) {
        for( int i = mixinReturnCount - 1; i >= 0; i-- ) {
            HashMap<String, Expression> variables = mixinReturnStack.get( i );
            Expression variable = variables.get( name );
            if( variable != null ) {
                return variable;
            }
        }
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
     * Add mixin parameters to the stack.
     * 
     * @param mixinParameters
     *            the mixinParameters, can be null if the current mixin has no parameters.
     */
    void addMixinParams( HashMap<String, Expression> mixinParameters ) {
        if( mixinParameters != null ) {
            variablesStack.add( mixinParameters );
        }
    }

    /**
     * Remove mixin parameters from the stack.
     * 
     * @param mixinParameters
     *            the mixinParameters, can be null if the current mixin has no parameters.
     */
    void removeMixinParams( HashMap<String, Expression> mixinParameters ) {
        if( mixinParameters != null ) {
            variablesStack.remove( variablesStack.size() - 1 );
        }
    }

    /**
     * Add rule variables to the stack.
     * 
     * @param variables
     *            the variables, can be null if the current rule has no parameters.
     */
    void addVariables( HashMap<String, Expression> variables ) {
        if( variables != null ) {
            variablesStack.add( variables );
        }
        while( mixinReturnStack.size() <= mixinReturnCount ) {
            mixinReturnStack.add( new HashMap<String, Expression>() );
        }
        mixinReturnStack.get( mixinReturnCount++ ).clear();
    }

    /**
     * Remove rule variables from the stack.
     * 
     * @param variables
     *            the variables, can be null if the current rule has no parameters.
     */
    void removeVariables( HashMap<String, Expression> variables ) {
        if( variables != null ) {
            variablesStack.remove( variablesStack.size() - 1 );
        }
        mixinReturnCount--;
    }

    /**
     * Add variables of a mixin to the stack.
     * 
     * @param variables
     *            the variables, can be null if the current mixin has no variables.
     */
    void addMixinVariables( HashMap<String, Expression> variables ) {
        addMixinParams( variables );
        while( mixinReturnStack.size() <= mixinReturnCount ) {
            mixinReturnStack.add( new HashMap<String, Expression>() );
        }
        mixinReturnStack.get( mixinReturnCount++ ).clear();
    }

    /**
     * Remove variables of a mixin from the stack.
     * 
     * @param variables
     *            the variables, can be null if the current mixin has no variables.
     */
    void removeMixinVariables( HashMap<String, Expression> variables ) {
        mixinReturnCount--;
        if( variables != null ) {
            int last = variablesStack.size() - 1;
            variablesStack.remove( last-- );
            HashMap<String, Expression> previousReturn = mixinReturnStack.get( mixinReturnCount );
            if( last >= 0 ) {
                HashMap<String, Expression> currentReturn = mixinReturnStack.get( mixinReturnCount - 1 );
                HashMap<String, Expression> parent = variablesStack.get( last );
                for( Entry<String, Expression> entry : variables.entrySet() ) {
                    if( !parent.containsKey( entry.getKey() ) && !currentReturn.containsKey( entry.getKey() ) ) {
                        currentReturn.put( entry.getKey(), ValueExpression.eval( this, entry.getValue() ) );
                    }
                }
                for( Entry<String, Expression> entry : previousReturn.entrySet() ) {
                    if( !parent.containsKey( entry.getKey() ) && !currentReturn.containsKey( entry.getKey() ) ) {
                        currentReturn.put( entry.getKey(), entry.getValue() );
                    }
                }
            }
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
        formatter.setInineMode( mode );
    }

    CssFormatter append( String str ) {
        formatter.append( output, str );
        return this;
    }

    CssFormatter appendColor( double color, String hint ) throws IOException {
        formatter.appendColor( output, color, hint );
        return this;
    }

    void appendHex( int value, int digits ) throws IOException {
        formatter.appendHex( output, value, digits );
    }

    CssFormatter append( char ch ) {
        output.append( ch );
        return this;
    }

    CssFormatter append( double value ) {
        formatter.append( output, value );
        return this;
    }

    CssFormatter appendValue( double value, String unit ) {
        formatter.appendValue( output, value, unit );
        return this;
    }

    void incInsets() {
        formatter.incInsets();
    }

    /**
     * Start a new block with a list of selectors.
     * @param selectors the selectors
     * @return this
     */
    CssFormatter startBlock( String[] selectors ) {
        formatter.startBlock( output, selectors );
        return this;
    }

    CssFormatter endBlock() {
        formatter.endBlock( output );
        return this;
    }

    void appendProperty( String name, Expression value ) throws IOException {
        formatter.appendProperty( output, this, name, value );
    }

    void setImportant( boolean important ) {
        formatter.setImportant( important );
    }

    CssFormatter space() {
        formatter.space( output );
        return this;
    }

    CssFormatter newline() {
        formatter.newline( output );
        return this;
    }

    CssFormatter comment( String msg ) {
        formatter.comment( output, msg );
        return this;
    }

    /**
     * Get a shared decimal format for parsing numbers with units.
     * 
     * @return the format
     */
    DecimalFormat getFormat() {
        return formatter.getFormat();
    }
}
