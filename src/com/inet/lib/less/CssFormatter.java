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
class CssFormatter {

    private class SharedState {
        private final StringBuilderPool                      pool             = new StringBuilderPool();

        private URL                                          baseURL;

        private LessExtendMap                                lessExtends;

        private final ArrayList<HashMap<String, Expression>> variablesStack   = new ArrayList<>();

        private final ArrayList<HashMap<String, Expression>> mixinReturnStack = new ArrayList<>();

        private int                                          mixinReturnCount;

        private final ArrayList<Rule>                        rulesStack       = new ArrayList<>();

        private int                                          rulesStackModCount;

        private PlainCssFormatter                            formatter;

        private final List<CssOutput>                        results          = new ArrayList<>();

        private boolean                                      charsetDirective;

        private CssFormatter                                 header;
    }

    private final SharedState               state;

    private final ArrayDeque<StringBuilder> outputs = new ArrayDeque<>();

    private StringBuilder                   output;

    private int                             blockDeep;

    CssFormatter( PlainCssFormatter formatter, boolean toString ) {
        state = new SharedState();
        state.formatter = formatter;
        if( toString ) {
            state.lessExtends = new LessExtendMap();
        }
        state.mixinReturnStack.add( new HashMap<String, Expression>() );
        state.mixinReturnCount++;
        state.header = new CssFormatter( this );
        state.results.add( new CssPlainOutput( state.header.output ) ); // header
    }

    CssFormatter( CssFormatter parent ) {
        state = parent.state;
        output = state.pool.get();
    }

    void format( LessParser parser, URL baseURL, Appendable appendable ) throws IOException {
        state.baseURL = baseURL;
        state.lessExtends = parser.getExtends();
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
            result.appendTo( appendable, state.formatter );
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

    String[] concatenateExtends( String[] selectors ) {
        return state.lessExtends.concatenateExtends( selectors );
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
        for( int i = state.mixinReturnCount - 1; i >= 0; i-- ) {
            HashMap<String, Expression> variables = state.mixinReturnStack.get( i );
            Expression variable = variables.get( name );
            if( variable != null ) {
                return variable;
            }
        }
        for( int i = state.variablesStack.size() - 1; i >= 0; i-- ) {
            HashMap<String, Expression> variables = state.variablesStack.get( i );
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
            state.variablesStack.add( mixinParameters );
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
            state.variablesStack.remove( state.variablesStack.size() - 1 );
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
            state.variablesStack.add( variables );
        }
        while( state.mixinReturnStack.size() <= state.mixinReturnCount ) {
            state.mixinReturnStack.add( new HashMap<String, Expression>() );
        }
        state.mixinReturnStack.get( state.mixinReturnCount++ ).clear();
    }

    /**
     * Remove rule variables from the stack.
     * 
     * @param variables
     *            the variables, can be null if the current rule has no parameters.
     */
    void removeVariables( HashMap<String, Expression> variables ) {
        if( variables != null ) {
            state.variablesStack.remove( state.variablesStack.size() - 1 );
        }
        state.mixinReturnCount--;
    }

    /**
     * Add variables of a mixin to the stack.
     * 
     * @param variables
     *            the variables, can be null if the current mixin has no variables.
     */
    void addMixinVariables( HashMap<String, Expression> variables ) {
        addMixinParams( variables );
        while( state.mixinReturnStack.size() <= state.mixinReturnCount ) {
            state.mixinReturnStack.add( new HashMap<String, Expression>() );
        }
        state.mixinReturnStack.get( state.mixinReturnCount++ ).clear();
    }

    /**
     * Remove variables of a mixin from the stack.
     * 
     * @param variables
     *            the variables, can be null if the current mixin has no variables.
     */
    void removeMixinVariables( HashMap<String, Expression> variables ) {
        state.mixinReturnCount--;
        if( variables != null ) {
            int last = state.variablesStack.size() - 1;
            state.variablesStack.remove( last-- );
            HashMap<String, Expression> previousReturn = state.mixinReturnStack.get( state.mixinReturnCount );
            if( last >= 0 ) {
                HashMap<String, Expression> currentReturn = state.mixinReturnStack.get( state.mixinReturnCount - 1 );
                HashMap<String, Expression> parent = state.variablesStack.get( last );
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
        state.rulesStack.add( rule );
        state.rulesStackModCount++;
    }

    void removeRule( Rule rule ) {
        state.rulesStack.remove( state.rulesStack.size() - 1 );
        state.rulesStackModCount++;
    }

    /**
     * A mixin inline never it self.
     * 
     * @param rule
     * @return true, if the mixin is currently formatting
     */
    boolean containsRule( Rule rule ) {
        return state.rulesStack.contains( rule );
    }

    /**
     * Get a nested mixin of a parent rule.
     * 
     * @param name
     *            the name of the mixin
     * @return the mixin or null
     */
    List<Rule> getMixin( String name ) {
        for( int i = state.rulesStack.size() - 1; i >= 0; i-- ) {
            List<Rule> rules = state.rulesStack.get( i ).getMixin( name );
            if( rules != null ) {
                for( int r = 0; r < rules.size(); r++ ) {
                    if( !state.rulesStack.contains( rules.get( r ) ) ) {
                        return rules;
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
            CssFormatter block = new CssFormatter( this );
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
            CssFormatter block = new CssFormatter( this );
            state.formatter.incInsets();
            state.results.add( new CssRuleOutput( selectors, block.output ) );
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
