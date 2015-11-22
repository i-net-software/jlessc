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

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A formatter for the CSS output. Hold some formating states.
 */
class CssFormatter implements Cloneable {

    /**
     * The scope of a single stack element.
     */
    private static class Scope {
        private Rule mixin;
        private Map<String, Expression> parameters;
        private Map<String, Expression> variables;
        private final Map<String, Expression> returns = new HashMap<>();

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

    /**
     * The global state of the sequential formatting.
     */
    private static class SharedState {
        private final StringBuilderPool                      pool             = new StringBuilderPool();

        private URL                                          baseURL;

        private final ArrayList<Scope>                       stack = new ArrayList<>();

        private int                                          stackIdx;

        private int                                          rulesStackModCount;

        private final List<CssOutput>                        results          = new ArrayList<>();

        private boolean                                      charsetDirective;

        private CssFormatter                                 header;

        private boolean                                      isReference;

        private int                                          importantCount;

        private LessExtendMap                                lessExtends = new LessExtendMap();
    }

    private final SharedState               state = new SharedState();

    private LessExtendMap                   lessExtends = state.lessExtends;

    private CssOutput currentOutput;

    private final static char[]             DIGITS    = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final ArrayDeque<StringBuilder> outputs   = new ArrayDeque<>();

    private StringBuilder                   output;

    private StringBuilder                   insets    = new StringBuilder();

    private boolean                         inlineMode;

    private final DecimalFormat             decFormat = new DecimalFormat( "#.########", DecimalFormatSymbols.getInstance( Locale.ENGLISH ) );

    private int                             blockDeep;

    private boolean                         isGuard;

    private boolean                         guardDefault;

    private boolean                         wasDefaultFunction;

    /**
     * Create a initial instance.
     */
    CssFormatter() {
        state.header = copy( null );
        state.results.add( currentOutput = new CssPlainOutput( state.header.output ) ); // header
    }

    /**
     * Create a new formatter for a single rule with optional output.
     * 
     * @param output
     *            optional target
     * @return a new formatter
     * @throws LessException
     *             if any error occur.
     */
    private CssFormatter copy( @Nullable StringBuilder output ) {
        try {
            CssFormatter formatter = (CssFormatter)clone();
            formatter.output = output == null ? state.pool.get() : output;
            formatter.insets = state.pool.get();
            formatter.insets.append( insets );
            return formatter;
        } catch( CloneNotSupportedException ex ) {
            throw new LessException( ex );
        }
    }

    /**
     * Format the a parsed less file.
     * 
     * @param parser the parser result
     * @param baseURL the URL of the less file
     * @param target the output of the resulting string
     */
    void format( LessParser parser, URL baseURL, StringBuilder target ) {
        state.baseURL = baseURL;
        addVariables( parser.getVariables() );
        state.isReference = false;
        for( Formattable rule : parser.getRules() ) {
            if( rule.getType() == Formattable.REFERENCE_INFO ) {
                state.isReference = ((ReferenceInfo)rule).isReference();
                continue;
            }
            if( rule.getClass() == Mixin.class ) {
                ((Mixin)rule).appendSubRules( null, this );
            } else {
                rule.appendTo( this );
            }
        }
        removeVariables( parser.getVariables() );

        output = target;
        for( CssOutput result : state.results ) {
            result.appendTo( target, lessExtends, this );
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
     * If already was write a character directive. Such directive can write only once.
     * 
     * @return true, if already write.
     */
    boolean isCharsetDirective(){
        return state.charsetDirective;
    }

    /**
     * Mark that there was write a character directive.
     */
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

    /**
     * Reset the output to a previous marker.
     * @param size the marker position
     */
    void setOutputSize( int size ) {
        output.setLength( size );
    }

    /**
     * Add a processed extend
     * @param lessExtend the extend
     */
    void add( LessExtend lessExtend ) {
        lessExtends.add( lessExtend, this.currentOutput.getSelectors() );
    }

    /**
     * Get the URL of the top less file.
     * @return the URL
     */
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
        if( name.equals( "@arguments" ) ) {
            for( int i = state.stackIdx - 1; i >= 0; i-- ) {
                Scope scope = state.stack.get( i );
                if( scope.parameters != null ) {
                    Operation params = new Operation( scope.mixin, ' ' );
                    for( Expression expr : scope.parameters.values() ) {
                        if( expr.getClass() == Operation.class && scope.parameters.size() == 1 ) {
                            return expr;
                        }
                        params.addOperand( expr );
                    }
                    return params;
                }
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
    void addMixin( Rule mixin, Map<String, Expression> parameters, Map<String, Expression> variables ) {
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
            Map<String, Expression> currentReturn = previous.returns;
            Map<String, Expression> vars = current.variables;
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
    void removeVariables( Map<String, Expression> variables ) {
        removeMixin();
    }

    /**
     * Add the parameters of a guard
     * 
     * @param parameters the parameters
     * @param isDefault if the default case will be evaluated, in this case the expression "default" in guard is true.
     */
    void addGuardParameters( Map<String, Expression> parameters, boolean isDefault ) {
        isGuard = true;
        wasDefaultFunction = false;
        guardDefault = isDefault;
        if( parameters != null ) {
            addMixin( null, parameters, null );
        }
    }

    /**
     * remove the parameters of a guard
     * @param parameters the parameters
     */
    void removeGuardParameters( Map<String, Expression> parameters ) {
        if( parameters != null ) {
            removeMixin();
        }
        isGuard = false;
    }

    /**
     * if we are inside of a guard
     * @return is guard
     */
    boolean isGuard() {
        return isGuard;
    }

    /**
     * Get the current value of "default" in a guard.
     * 
     * @return true if the default case will be evaluate.
     */
    boolean getGuardDefault() {
        wasDefaultFunction = true;
        return guardDefault;
    }

    /**
     * If the state of "default" was requested since the last guard parameter was added.
     * 
     * @return true, if default was called
     */
    boolean wasDefaultFunction() {
        return wasDefaultFunction;
    }

 
    /**
     * A mixin inline never it self.
     * 
     * @param rule the rule
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

    /**
     * The counter of the variable stack changes. This is an optimizing that some expression does not need reevaluate if this ID has not changed. 
     * @return the id
     */
    int stackID() {
        return state.rulesStackModCount;
    }

    /**
     * Get the current output of the formatter.
     * 
     * @return the output.
     */
    StringBuilder getOutput() {
        if( output == null ) {
            CssFormatter block = copy( null );
            state.results.add( new CssPlainOutput( block.output ) );
            output = block.output;
        }
        return output;
    }

    /**
     * Set the inline mode. In the inline mode: 
     * <li>quotes are removed from strings
     * <li>colors are written ever as full color RGB value
     * @param mode the new mode.
     */
    void setInlineMode( boolean mode ) {
        inlineMode = mode;
    }

    /**
     * Current inline mode
     * @return the mode
     */
    boolean inlineMode() {
        return inlineMode;
    }

    /**
     * Append a string to the output. In inline mode quotes are removed.
     * @param str the string
     * @return this
     */
    CssFormatter append( String str ) {
        if( inlineMode ) {
            str = UrlUtils.removeQuote( str );
        }
        output.append( str );
        return this;
    }

    /**
     * Append a color. In inline mode it is ever a 6 digit RGB value.
     * @param color the color value
     * @param hint the original spelling of the color if not calculated
     * @return this
     */
    CssFormatter appendColor( double color, @Nullable String hint ) {
        if( !inlineMode && hint != null ) {
            output.append( hint );
        } else {
            int argb = ColorUtils.argb( color );
            output.append( '#' );
            appendHex( argb, 6 );
        }
        return this;
    }

    /**
     * Append an hex value to the output.
     * 
     * @param value the value
     * @param digits the digits to write.
     */
    void appendHex( int value, int digits ) {
        if( digits > 1 ) {
            appendHex( value >>> 4, digits-1 );
        }
        output.append( DIGITS[ value & 0xF ] );
    }

    /**
     * Append a single character to the output.
     * 
     * @param ch the character
     * @return a reference to this object
     */
    CssFormatter append( char ch ) {
        output.append( ch );
        return this;
    }

    /**
     * Append a decimal number to the output.
     * 
     * @param value the number
     * @return a reference to this object
     */
    CssFormatter append( double value ) {
        if( value == (int)value ) {
            output.append( Integer.toString( (int)value ) );
        } else {
            output.append( decFormat.format( value ) );
        }
        return this;
    }

    /**
     * Append a value with a unit. In compress mode not all units are written.
     * 
     * @param value the value.
     * @param unit the unit
     * @return a reference to this object
     */
    CssFormatter appendValue( double value, String unit ) {
        append( value );
        append( unit );
        return this;
    }

    /**
     * Increment the insets of the output.
     */
    void incInsets() {
        insets.append( "  " );
    }

    /**
     * Decrement the insets of the output.
     */
    void decInsets() {
        insets.setLength( insets.length() - 2 );
    }

    /**
     * Start a new block with a list of selectors.
     * @param selectors the selectors
     * @return this
     */
    CssFormatter startBlock( String[] selectors ) {
        final List<CssOutput> results = state.results;
        if( blockDeep == 0 ) {
            output = null;
            CssOutput nextOutput = null;
            if( results.size() > 0 && !"@font-face".equals( selectors[0] ) ) {
                CssOutput cssOutput = results.get( results.size() - 1 );
                if( Arrays.equals( selectors, cssOutput.getSelectors() ) ) {
                    nextOutput = cssOutput;
                }
            }
            CssFormatter block;
            if( nextOutput == null ) {
                block = copy( null );
                if( selectors[0].startsWith( "@media" ) ) {
                    block.lessExtends = new LessExtendMap( state.lessExtends );
                    nextOutput = new CssMediaOutput( selectors, block.output, state.isReference, block.lessExtends ); 
                } else {
                    nextOutput = new CssRuleOutput( selectors, block.output, state.isReference );
                }
                results.add( nextOutput );
            } else {
                block = copy( nextOutput.getOutput() );
            }
            block.currentOutput = nextOutput;
            block.incInsets();
            block.blockDeep++;
            return block;
        } else {
            if( selectors[0].startsWith( "@media" ) ) {
                CssFormatter block = copy( null );
                block.lessExtends = new LessExtendMap( state.lessExtends );
                String[] sel = new String[]{ this.currentOutput.getSelectors()[0] + " and " + selectors[0].substring( 6 ).trim() };
                block.currentOutput = new CssMediaOutput( sel, block.output, state.isReference, block.lessExtends );
                results.add( block.currentOutput );
                block.insets.setLength( 2 );
                block.blockDeep = 1;
                return block;
            } else {
                if( blockDeep == 1 && this.currentOutput.getClass() == CssMediaOutput.class ) {
                    CssFormatter block = copy( null );
                    block.incInsets();
                    block.currentOutput = this.currentOutput;
                    ((CssMediaOutput)this.currentOutput).startBlock( selectors, block.output );
                    block.blockDeep++;
                    return block;
                } else {
                    blockDeep++;
                    startBlockImpl( selectors );
                    return this;
                }
            }
        }
    }

    /**
     * Output a new block and increment the insets.
     * 
     * @param selectors the selectors of the block.
     */
    void startBlockImpl( String[] selectors ) {
        for( int i=0; i<selectors.length; i++ ) {
            if( i > 0 ) {
                output.append( ',' );
                newline();
            }
            insets();
            append( selectors[i] );
        }
        space();
        output.append( '{' );
        newline();
        incInsets();
    }

    /**
     * Terminate a CSS block.
     * 
     * @return a reference to this object
     */
    CssFormatter endBlock() {
        blockDeep--;
        if( blockDeep == 0 ) {
            state.pool.free( insets );
            insets = null;
            inlineMode = false;
        } else {
            if( blockDeep == 1 && currentOutput.getClass() == CssMediaOutput.class ) {
                state.pool.free( insets );
                insets = null;
                inlineMode = false;
            } else {
                endBlockImpl();
            }
        }
        return this;
    }

    /**
     * Decrement the insets and output the end block.
     */
    void endBlockImpl() {
        decInsets();
        insets();
        output.append( '}' );
        newline();
    }

    /**
     * Append a property to the output like: name: value;
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     * @throws LessException
     *             if write properties in the root
     */
    void appendProperty( @Nonnull String name, @Nonnull Expression value ) {
        if( output == null ) {
            throw new LessException( "properties must be inside selector blocks, they cannot be in the root." );
        }
        insets();
        name = SelectorUtils.replacePlaceHolder( this, name, value );
        output.append( name ).append( ':' );
        space();
        if( "font".equals( name ) && value.getClass() == Operation.class ) {
            appendFontPropertyValue( (Operation)value );
        } else {
            value.appendTo( this );
        }
        if( state.importantCount > 0 || value.isImportant() ) {
            output.append( " !important" );
        }
        semicolon();
        newline();
    }

    /**
     * Special hack for property "font" which contains a value font-size/line-height
     * @param value the value
     */
    void appendFontPropertyValue( Operation value ) {
        ArrayList<Expression> operands = value.getOperands();
        char operator = value.getOperator();
        switch( operator ) {
            case '~':
            case ',':
                value.appendTo( this );
                return;
        }
        for( int i = 0; i < operands.size(); i++ ) {
            if( i > 0 ) {
                this.append( operator );
            }
            Expression operand = operands.get( i );
            if( operand.getClass() == Operation.class ) {
                appendFontPropertyValue( (Operation)operand );
            } else {
                operand.appendTo( this );
            }
        }
    }

    /**
     * Increment the important flag.
     */
    void incImportant() {
        state.importantCount++;
    }

    /**
     * Decrement the important flag.
     */
    void decImportant() {
        state.importantCount--;
    }

    /**
     * Write a single space. The compress formatter do nothing.
     * 
     * @return a reference to this object
     */
    CssFormatter space() {
        output.append( ' ' );
        return this;
    }

    /**
     * Write a newline. The compress formatter do nothing.
     * 
     * @return a reference to this object
     */
    CssFormatter newline() {
        output.append( '\n' );
        return this;
    }

    /**
     * Write a semicolon. The compress formatter do nothing before an end block.
     */
    void semicolon() {
        output.append( ';' );
    }

    /**
     * Write the current insets. The compress formatter do nothing.
     */
    void insets() {
        output.append( insets );
    }

    /**
     * Write a comment. The compress formatter do nothing.
     * @param msg the comment including the comment markers
     * @return a reference to this object
     */
    CssFormatter comment( String msg ) {
        getOutput().append( insets ).append( msg ).append( '\n' );
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
