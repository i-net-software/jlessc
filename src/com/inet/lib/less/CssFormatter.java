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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * A formatter for the CSS output. Hold some formating states.
 */
class CssFormatter implements Cloneable {

    /**
     * The scope of a single stack element.
     */
    private static class Scope {
        private Rule mixin;
        private HashMap<String, Expression> parameters;
        private HashMap<String, Expression> variables;
        private final HashMap<String, Expression> returns = new HashMap<>();

        /**
         * Get a variable expression from this scope
         * 
         * @param name
         *            the name of the variable starting with @
         * @return the expression or null if not found
         */
        Expression getVariable( String name ) {
            if( parameters != null ) {
                Expression variable = parameters.get( name );
                if( variable != null ) {
                    return variable;
                }
            }
            if( variables != null ) {
                Expression variable = variables.get( name );
                if( variable != null ) {
                    return variable;
                }
            }
            if( returns != null ) {
                Expression variable = returns.get( name );
                if( variable != null ) {
                    return variable;
                }
            }
            return null;
        }
    }

    private static class SharedState {
        private final StringBuilderPool                      pool             = new StringBuilderPool();

        private URL                                          baseURL;

        private final ArrayList<Scope>                       stack = new ArrayList<>();

        private int                                          stackIdx;

        private final LessExtendMap                          lessExtends      = new LessExtendMap();

        private int                                          rulesStackModCount;

        private PlainCssFormatter                            formatter;

        private final List<CssOutput>                        results          = new ArrayList<>();

        private boolean                                      charsetDirective;

        private CssFormatter                                 header;

        private String[] selectors;
    }

    private final SharedState               state;

    private final ArrayDeque<StringBuilder> outputs = new ArrayDeque<>();

    private StringBuilder                   output;

    private int                             blockDeep;

    CssFormatter( PlainCssFormatter formatter ) {
        state = new SharedState();
        state.formatter = formatter;
        state.header = copy( null );
        state.results.add( new CssPlainOutput( state.header.output ) ); // header
    }

    /**
     * Create a new formatter for a single rule with optional output. 
     * @param parent the parent CssFormatter
     */
    private CssFormatter copy( StringBuilder output ) {
        try {
            CssFormatter formatter = (CssFormatter)clone();
            formatter.output = output == null ? state.pool.get() : output;
            return formatter;
        } catch( CloneNotSupportedException ex ) {
            throw new LessException( ex );
        }
    }

    void format( LessParser parser, URL baseURL, Appendable appendable ) throws IOException {
        state.baseURL = baseURL;
        addVariables( parser.getVariables() );
        for( Formattable rule : parser.getRules() ) {
            if( rule.getClass() == Mixin.class ) {
                ((Mixin)rule).appendSubRules( null, this );
            } else {
                rule.appendTo( this );
            }
        }
        removeVariables( parser.getVariables() );

        for( CssOutput result : state.results ) {
            result.appendTo( appendable, state.lessExtends, state.formatter );
        }
    }

    /**
     * Get the formatter for CSS directives. 
     * @return the header formatter
     */
    CssFormatter getHeader() {
        return state.header;//results.get( 0 );
    }

    /**
     * Write this to the appendable.
     * @param appendable the target
     * @throws IOException if any I/O error occurs
     */
    void appendTo( Appendable appendable ) throws IOException {
        appendable.append( output );
    }

    boolean isCharsetDirective(){
        return state.charsetDirective;
    }
    
    void setCharsetDirective() {
        state.charsetDirective = true;
    }

    /**
     * Add a new output buffer to the formatter.
     */
    void addOutput() {
        if( output != null ) {
            outputs.addLast( output );
        }
        output = state.pool.get();
    }

    /**
     * Release an output and delete it.
     */
    void freeOutput() {
        output = outputs.size() > 0 ? outputs.removeLast() : null;
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
        return output == null ? -1 : output.length();
    }

    void setOutputSize( int size ) {
        output.setLength( size );
    }

    void add( LessExtend lessExtend ) {
        state.lessExtends.add( lessExtend, state.selectors );
    }

    URL getBaseURL() {
        return state.baseURL;
    }

    /**
     * Get a variable expression from the current stack
     * 
     * @param name
     *            the name of the variable starting with @
     * @return the expression or null if not found
     */
    Expression getVariable( String name ) {
        for( int i = state.stackIdx - 1; i >= 0; i-- ) {
            Expression variable = state.stack.get( i ).getVariable( name );
            if( variable != null ) {
                return variable;
            }
        }
        return null;
    }

    /**
     * Add the scope of a mixin to the stack.
     * @param mixin the mixin
     * @param parameters the calling parameters
     * @param variables the variables of the mixin
     */
    void addMixin( Rule mixin, HashMap<String, Expression> parameters, HashMap<String, Expression> variables ) {
        int idx = state.stackIdx++;
        Scope scope;
        if( state.stack.size() <= idx ) {
            scope = new Scope();
            state.stack.add( scope );
        } else {
            scope = state.stack.get( idx );
            scope.returns.clear();
        }
        scope.mixin = mixin;
        scope.parameters = parameters;
        scope.variables = variables;
    }

    /**
     * Remove the scope of a mixin.
     */
    void removeMixin() {
        int idx = state.stackIdx - 1;
        Scope current = state.stack.get( idx );
        if( idx > 0 ) {
            Scope previous = state.stack.get( idx - 1 );
            HashMap<String, Expression> currentReturn = previous.returns;
            HashMap<String, Expression> vars = current.variables;
            if( vars != null ) {
                for( Entry<String, Expression> entry : vars.entrySet() ) {
                    if( previous.getVariable( entry.getKey() ) == null ) {
                        currentReturn.put( entry.getKey(), ValueExpression.eval( this, entry.getValue() ) );
                    }
                }
            }
            vars = current.returns;
            if( vars != null ) {
                for( Entry<String, Expression> entry : vars.entrySet() ) {
                    if( previous.getVariable( entry.getKey() ) == null ) {
                        currentReturn.put( entry.getKey(), ValueExpression.eval( this, entry.getValue() ) );
                    }
                }
            }
        }
        state.stackIdx--;
        state.rulesStackModCount++;
    }

    /**
     * Add rule variables to the stack.
     * 
     * @param variables
     *            the variables, can be null if the current rule has no parameters.
     */
    void addVariables( HashMap<String, Expression> variables ) {
        addMixin( null, null, variables );
    }

    /**
     * Remove rule variables from the stack.
     * 
     * @param variables
     *            the variables, can be null if the current rule has no parameters.
     */
    void removeVariables( HashMap<String, Expression> variables ) {
        removeMixin();
    }

    /**
     * A mixin inline never it self.
     * 
     * @param rule
     * @return true, if the mixin is currently formatting
     */
    boolean containsRule( Rule rule ) {
        for( int i = state.stackIdx - 1; i >= 0; i-- ) {
            if( rule == state.stack.get( i ).mixin ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a nested mixin of a parent rule.
     * 
     * @param name
     *            the name of the mixin
     * @return the mixin or null
     */
    List<Rule> getMixin( String name ) {
        for( int i = state.stackIdx - 1; i >= 0; i-- ) {
            Rule mixin = state.stack.get( i ).mixin;
            if( mixin != null ) {
                List<Rule> rules = mixin.getMixin( name );
                if( rules != null ) {
                    for( int r = 0; r < rules.size(); r++ ) {
                        if( !containsRule( rules.get( r ) ) ) {
                            return rules;
                        }
                    }
                }
            }
        }
        return null;
    }

    int stackID() {
        return state.rulesStackModCount;
    }

    StringBuilder getOutput() {
        if( output == null ) {
            CssFormatter block = copy( null );
            state.results.add( new CssPlainOutput( block.output ) );
            output = block.output;
        }
        return output;
    }

    void setInineMode( boolean mode ) {
        state.formatter.setInineMode( mode );
    }

    CssFormatter append( String str ) throws IOException {
        state.formatter.append( output, str );
        return this;
    }

    CssFormatter appendColor( double color, String hint ) throws IOException {
        state.formatter.appendColor( output, color, hint );
        return this;
    }

    void appendHex( int value, int digits ) throws IOException {
        state.formatter.appendHex( output, value, digits );
    }

    CssFormatter append( char ch ) {
        output.append( ch );
        return this;
    }

    CssFormatter append( double value ) {
        state.formatter.append( output, value );
        return this;
    }

    CssFormatter appendValue( double value, String unit ) throws IOException {
        state.formatter.appendValue( output, value, unit );
        return this;
    }

    void incInsets() {
        state.formatter.incInsets();
    }

    /**
     * Start a new block with a list of selectors.
     * @param selectors the selectors
     * @return this
     * @throws IOException 
     */
    CssFormatter startBlock( String[] selectors ) throws IOException {
        if( blockDeep == 0 ) {
            output = null;
            state.formatter.incInsets();
            final List<CssOutput> results = state.results;
            if( results.size() > 0 ) {
                CssOutput cssOutput = results.get( results.size() - 1 );
                if( cssOutput.getClass() == CssRuleOutput.class ) {
                    CssRuleOutput ruleOutput = (CssRuleOutput)cssOutput;
                    if( Arrays.equals( selectors, ruleOutput.getSelectors() ) ) {
                        CssFormatter block = copy( ruleOutput.getOutput() );
                        block.blockDeep++;
                        return block;
                    }
                }
            }
            state.selectors = selectors;
            CssFormatter block = copy( null );
            results.add( new CssRuleOutput( selectors, block.output ) );
            block.blockDeep = 1;
            return block;
        } else {
            blockDeep++;
            state.formatter.startBlock( output, selectors );
            return this;
        }
    }

    CssFormatter endBlock() throws IOException {
        blockDeep--;
        if( blockDeep == 0 ) {
            state.formatter.clean();
            state.selectors = null;
        } else {
            state.formatter.endBlock( output );
        }
        return this;
    }

    void appendProperty( String name, Expression value ) throws IOException {
        state.formatter.appendProperty( output, this, name, value );
    }

    void setImportant( boolean important ) {
        state.formatter.setImportant( important );
    }

    CssFormatter space() throws IOException {
        state.formatter.space( output );
        return this;
    }

    CssFormatter newline() throws IOException {
        state.formatter.newline( output );
        return this;
    }

    CssFormatter comment( String msg ) throws IOException {
        state.formatter.comment( getOutput(), msg );
        return this;
    }

    /**
     * Get a shared decimal format for parsing numbers with units.
     * 
     * @return the format
     */
    DecimalFormat getFormat() {
        return state.formatter.getFormat();
    }
}
