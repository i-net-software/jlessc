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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A CSS rule.
 */
class Rule extends LessObject implements Formattable, FormattableContainer {

    private static final HashMap<String, Expression> NO_MATCH  = new HashMap<>();

    private String[]                    selectors;

    private final List<Expression>      params;
    
    private VariableExpression          varArg;

    private Expression                  guard;

    private List<Formattable>           properties = new ArrayList<>();

    private List<Rule>                  subrules = new ArrayList<>();
    
    private HashMap<String, Expression> variables  = new HashMap<>();

    Rule( LessLookAheadReader reader, String selectors, Operation params, Expression guard ) {
        super( reader );
        this.selectors = selectors.split( "," );
        for( int i = 0; i < this.selectors.length; i++ ) {
            this.selectors[i] = this.selectors[i].trim();
        }
        if( params == null ) {
            this.params = null;
        } else {
            this.params = params.getOperands();
            int count = this.params.size();
            if( count > 0 ) {
                Expression lastEx = this.params.get( count-1 );
                if( lastEx.getClass() == VariableExpression.class ) {
                    String name = lastEx.toString();
                    if( name.endsWith( "..." ) ) {
                        varArg = new VariableExpression( (VariableExpression)lastEx, name.substring( 0, name.length() - 3 ) );
                        this.params.remove( count-1 );
                    }
                }
            }
        }
        this.guard = guard;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getType() {
        return RULE;
    }

    @Override
    public void add( Formattable formattable ) {
        properties.add( formattable );
        if( formattable instanceof Rule ) {
            subrules.add( (Rule)formattable );
        }
    }

    @Override
    public void appendTo( CssFormatter formatter ) {
        if( isValidCSS( formatter ) ) {
            try {
                appendTo( null, formatter );
            } catch( LessException ex ) {
                ex.addPosition( filename, line, column );
                throw ex;
            }
        }
    }

    void appendTo( String[] mainSelector, CssFormatter formatter ) {
        try {
            String[] sel = selectors;

            for( int s = 0; s < sel.length; s++ ) {
                final String selector = sel[s];
                String str = SelectorUtils.replacePlaceHolder( formatter, selector, this );
                if( selector != str ) {
                    if( sel == selectors ) {
                        sel = sel.clone(); // we does not want change the declaration of this selectors
                    }
                    sel[s] = str;
                }
            }

            if( mainSelector == null ) {        // main ruls
                //sel = sel;
            } else if( sel[0].charAt( 0 ) == '@' ) {
                // media
                media( sel, mainSelector, formatter );
                return;
            } else {
                sel = SelectorUtils.merge( mainSelector, sel );
            }
            formatter.addMixin( this, null, variables );

            if( sel[0].startsWith( "@" ) ) {
                ruleset( sel, formatter );
            } else {
                if( properties.size() > 0 ) {
                    int size0 = formatter.getOutputSize();
                    CssFormatter block = formatter.startBlock( sel );
                    int size1 = block.getOutputSize();
                    appendPropertiesTo( block );
                    int size2 = block.getOutputSize();
                    block.endBlock();
                    if( block == formatter && size1 == size2 ) {
                        formatter.setOutputSize( size0 );
                    }
                }

                for( Formattable prop : properties ) {
                    if( prop instanceof Mixin ) {
                        ((Mixin)prop).appendSubRules( sel, formatter );
                    }
                }

                for( Rule rule : subrules ) {
                    if( rule.isValidCSS( formatter ) && !rule.isInlineRule( formatter) ) {
                        rule.appendTo( params != null ? mainSelector : sel, formatter );
                    }
                }
            }
            formatter.removeMixin();
        } catch( LessException ex ) {
            ex.addPosition( filename, line, column );
            throw ex;
        } catch( Exception ex ) {
            throw createException( ex );
        }
    }

    private void media( String[] mediaSelector, String[] blockSelector, CssFormatter formatter ) {
        if( properties.size() > 0 ) {
            int size0 = formatter.getOutputSize();
            CssFormatter block = formatter.startBlock( mediaSelector );
            if( block != formatter ) {
                size0 = block.getOutputSize();
            }
            block.startBlock( blockSelector );
            int size1 = block.getOutputSize();
            appendPropertiesTo( block );
            int size2 = block.getOutputSize();
            block.endBlock();
            int size3 = block.getOutputSize();
            for( Formattable prop : properties ) {
                if( prop instanceof Mixin ) {
                    ((Mixin)prop).appendSubRules( blockSelector, block );
                }
            }
            int size4 = block.getOutputSize();
            block.endBlock();
            if( size1 == size2 && size3 == size4 ) {
                block.setOutputSize( size0 );
            }
        }

        for( Rule rule : subrules ) {
            final String[] ruleSelector = rule.getSelectors();
            String name = ruleSelector[0];
            name = SelectorUtils.replacePlaceHolder( formatter, name, this );
            if( name.startsWith( "@media" ) ) {
                rule.media( new String[]{mediaSelector[0] + " and " + name.substring( 6 ).trim()}, blockSelector, formatter );
            } else {
                rule.media( mediaSelector, SelectorUtils.merge( blockSelector, ruleSelector ), formatter );
            }
        }
    }

    /**
     * 
     * @param sel
     * @param formatter
     */
    private void ruleset( String[] sel, CssFormatter formatter ) {
        formatter = formatter.startBlock( sel );
        appendPropertiesTo( formatter );

        for( Formattable prop : properties ) {
            if( prop instanceof Mixin ) {
                ((Mixin)prop).appendSubRules( null, formatter );
            }
        }

        for( Rule rule : subrules ) {
            rule.appendTo( formatter );
        }
        formatter.endBlock();
    }

    void appendMixinsTo( String[] parentSelector, CssFormatter formatter ) {
        for( Formattable prop : properties ) {
            switch( prop.getType()) {
                case MIXIN:
                    ((Mixin)prop).appendSubRules( parentSelector, formatter );
                    break;
                case CSS_AT_RULE:
                case COMMENT:
                    prop.appendTo( formatter );
                    break;
            }
        }
    }

    void appendPropertiesTo( CssFormatter formatter ) {
        for( Formattable prop : properties ) {
            switch( prop.getType() ) {
                case Formattable.RULE:
                    Rule rule = (Rule)prop;
                    // inline rules
                    if( rule.isValidCSS( formatter ) && rule.isInlineRule( formatter) ) {
                        formatter.addVariables( rule.variables );
                        rule.appendPropertiesTo( formatter );
                        formatter.removeVariables( rule.variables );
                    }
                    break;
                default:
                    prop.appendTo( formatter );
            }
        }
    }

    /**
     * Get the mixin parameters as map if the given param values match to this rule.
     * @param formatter current formatter
     * @param paramValues the values of the caller
     * @return null, if empty list match; NO_MATCH, if the values not match to the params of this list, Or the map with the parameters
     */
    private HashMap<String, Expression> getMixinParams( CssFormatter formatter, List<Expression> paramValues ) {
        if( (params == null && paramValues == null) || (paramValues == null && params.size() == 0) || (params == null && paramValues.size() == 0) ) {
            return null;
        }
        if( params == null && paramValues != null ) {
            return NO_MATCH;
        }
        if( paramValues == null ) {
            paramValues = Collections.emptyList();
        }
        if( params.size() < paramValues.size() && varArg == null ) {
            return NO_MATCH;
        }

        try {
            HashMap<String, Expression> vars = new HashMap<>();
            // Set the parameters with default values first
            int paramsCount = params.size();
            for( int i = 0; i < paramsCount; i++ ) {
                Expression param = params.get( i );
                Class<?> paramType = param.getClass();
                if( paramType ==  Operation.class && ((Operation)param).getOperator() == ':' && ((Operation)param).getOperands().size() == 2 ) {
                    ArrayList<Expression> keyValue = ((Operation)param).getOperands();
                    String name = keyValue.get( 0 ).toString();
                    vars.put( name, keyValue.get( 1 ) );
                }
            }

            // Set the calling values as parameters
            paramsCount = Math.min( paramsCount, paramValues.size() );
            for( int i = 0; i < paramsCount; i++ ) {
                Expression value = paramValues.get( i );
                Class<?> valueType = value.getClass();
                Expression param = params.get( i );
                Class<?> paramType = param.getClass();
                // First check if it is a named parameter
                if( valueType == Operation.class && ((Operation)value).getOperator() == ':' && ((Operation)value).getOperands().size() == 2 ) {
                    ArrayList<Expression> keyValue = ((Operation)value).getOperands();
                    vars.put( keyValue.get( 0 ).toString(), ValueExpression.eval( formatter, keyValue.get( 1 ) ) );
                } else {
                    if( paramType == VariableExpression.class ) {
                        vars.put( param.toString(), ValueExpression.eval( formatter, value ) );
                    } else if( paramType ==  Operation.class && ((Operation)param).getOperator() == ':' && ((Operation)param).getOperands().size() == 2 ) {
                        ArrayList<Expression> keyValue = ((Operation)param).getOperands();
                        vars.put( keyValue.get( 0 ).toString(), ValueExpression.eval( formatter, value ) );
                    } else {
                        throw createException( "Wrong formatted parameters: " + params );
                    }
                }
            }

            if( varArg != null ) {
                Operation value = new Operation( varArg );
                for( int i = params.size(); i < paramValues.size(); i++ ) {
                    value.addOperand( ValueExpression.eval( formatter, paramValues.get( i ) ) );
                }
                if( vars.size() == params.size() ) {
                    vars.put( varArg.toString(), value );
                    return vars;
                }
                return NO_MATCH;
            }
            if( vars.size() == params.size() ) {
                return vars;
            }
            return NO_MATCH;
        } catch( LessException ex ) {
            ex.addPosition( filename, line, column );
            throw ex;
        }
    }

    String[] getSelectors() {
        return selectors;
    }

    public HashMap<String, Expression> getVariables() {
        return variables;
    }

    List<Rule> getSubrules() {
        return subrules;
    }

    /**
     * Get a nested mixin of this rule.
     * 
     * @param name
     *            the name of the mixin
     * @return the mixin or null
     */
    List<Rule> getMixin( String name ) {
        ArrayList<Rule> rules = null;
        for( Rule rule : subrules ) {
            for( String sel : rule.selectors ) {
                if( name.equals( sel ) ) {
                    if( rules == null ) {
                        rules = new ArrayList<>();
                    }
                    rules.add( rule );
                    break;
                }
            }
        }
        return rules;
    }

    /**
     * String only for debugging.
     */
    @Override
    public String toString() {
        CssFormatter formatter = new CssFormatter();
        try {
            appendTo( null, formatter );
        } catch( Exception ex ) {
            formatter.getOutput();
            formatter.append( ex.toString() );
        }
        return formatter.releaseOutput();
    }

    MixinMatch match( CssFormatter formatter, List<Expression> paramValues ) {
        if( guard == null && formatter.containsRule( this ) ) {
            return null;
        }
        HashMap<String, Expression> mixinParameters = getMixinParams( formatter, paramValues );
        if( mixinParameters == NO_MATCH ) {
            return null;
        }
        boolean matching = true;

        if( guard != null ) {
            if( mixinParameters != null ) {
                formatter.addVariables( mixinParameters );
            }

            matching = guard.booleanValue( formatter );

            if( mixinParameters != null ) {
                formatter.removeVariables( mixinParameters );
            }
        }

        return new MixinMatch( this, mixinParameters, matching);
    }

    boolean isMixin() {
        return params != null;
    }

    /**
     * If this rule is a CSS rule (not a mixin declaration) and if an guard exists if it true.
     * @param formatter the CCS target
     * @return true, if a CSS rule
     */
    private boolean isValidCSS( CssFormatter formatter ) {
        if( params == null ) {
            if( guard != null ) {
                //CSS Guards
                guard = ValueExpression.eval( formatter, guard );
                return guard.booleanValue( formatter );
            }
            return true;
        }
        return false;
    }

    boolean isInlineRule( CssFormatter formatter ) {
        if( selectors.length == 1 && selectors[0].equals( "&" ) ) {
            return hasOnlyInlineProperties( formatter );
        }
        return false;
    }

    boolean hasOnlyInlineProperties( CssFormatter formatter ) {
        for( Formattable prop : properties ) {
            if( prop instanceof Mixin ) {
                return false;
            }
        }

        for( Rule rule : subrules ) {
            if( rule.isValidCSS( formatter ) && rule.isInlineRule( formatter) ) {
                return false;
            }
        }
        return true;
    }
}
