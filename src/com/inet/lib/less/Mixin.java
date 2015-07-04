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
import java.util.List;

/**
 * The placeholder of a mixin.
 */
class Mixin extends LessObject implements Formattable {

    private final String name;
    private final HashMultimap<String,Rule> mixins;
    private final List<Expression> paramValues;
    private final boolean important;
    private List<MixinMatch> mixinRules;
    private int stackID;

    Mixin( LessLookAheadReader reader, String name, Operation paramValues, HashMultimap<String,Rule> mixins ) {
        super( reader );
        if( name.endsWith( "!important" ) ) {
            important = true;
            name = name.substring( 0, name.length() - 10 ).trim();
        } else {
            important = false;
        }
        this.name = name;
        this.paramValues = paramValues == null ? null : paramValues.getOperands();
        this.mixins = mixins;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getType() {
        return MIXIN;
    }

    @Override
    public void appendTo( CssFormatter formatter ) {
        formatter.setImportant( important );
        try {
            for( MixinMatch match : getRules( formatter ) ) {
                Rule rule = match.getRule();
                formatter.addMixin( rule, match.getMixinParameters(), rule.getVariables() );
                rule.appendPropertiesTo( formatter );
                formatter.removeMixin();
            }
        } catch( LessException ex ) {
            ex.addPosition( filename, line, column );
            throw ex;
        } catch( StackOverflowError soe ) {
            throw createException( "Maximum call stack size exceeded in mixin: " + name, soe );
        }
        formatter.setImportant( false );
    }

    void appendSubRules( String[] parentSelector, CssFormatter formatter ) {
        try {
            for( MixinMatch match : getRules( formatter ) ) {
                Rule rule = match.getRule();
                formatter.addMixin( rule, match.getMixinParameters(), rule.getVariables() );
                rule.appendMixinsTo( parentSelector, formatter );
                for( Rule subMixin : rule.getSubrules() ) {
                    if( !subMixin.isMixin() && (parentSelector == null || !subMixin.isInlineRule( formatter ) ) ) {
                        subMixin.appendTo( parentSelector, formatter );
                    }
                }
                formatter.removeMixin();
            }
        } catch( LessException ex ) {
            ex.addPosition( filename, line, column );
            throw ex;
        }
    }

    private List<MixinMatch> getRules( CssFormatter formatter ) {
        if( mixinRules != null && stackID == formatter.stackID() ) {
            return mixinRules;
        }
        List<Rule> rules = formatter.getMixin( name );
        if( rules == null ) {
            rules = mixins.get( name );
        }
        if( rules == null ) {
            int idx = name.indexOf( '>' ); // mixin with namespace?
            if( idx > 0 ) {
                String mainName = name.substring( 0, idx ).trim();
                rules = mixins.get( mainName );
                if( rules != null ) {
                    rules = rules.get( 0 ).getMixin( name.substring( idx + 1 ).trim() );
                }
            } else {
                idx = name.indexOf( '.' ); // mixin with namespace?
                if( idx > 0 ) {
                    String mainName = name.substring( 0, idx ).trim();
                    rules = mixins.get( mainName );
                    if( rules != null ) {
                        rules = rules.get( 0 ).getMixin( name.substring( idx ).trim() );
                    }
                }
            }
            if( rules == null ) {
                throw createException( "Undefine mixin: " + name );
            }
        }
        stackID = formatter.stackID();
        mixinRules = new ArrayList<>();
        boolean paramMatch = false;
        List<Rule> defaultMixins = null;
        for( Rule rule : rules ) {
            MixinMatch matching = rule.match( formatter, paramValues, false );
            if( matching != null ) {
                paramMatch = true;
                if( matching.getGuard() ) {
                    mixinRules.add( matching );
                } else if( matching.wasDefault() ) {
                    if( defaultMixins == null ) {
                        defaultMixins = new ArrayList<>();
                    }
                    defaultMixins.add( rule );
                }
            }
        }
        if( !paramMatch ) {
            throw createException( "No matching definition was found for: " + name );
        }
        if( mixinRules.size() == 0 && defaultMixins != null ) {
            for( Rule rule : defaultMixins ) {
                MixinMatch matching = rule.match( formatter, paramValues, true );
                if( matching != null && matching.getGuard() ) {
                    mixinRules.add( matching );
                }
            }
        }
        return mixinRules;
    }

}
