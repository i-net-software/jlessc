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

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The parser of the less stream.
 */
class LessParser implements FormattableContainer {

    private URL                         baseURL;

    private URL                         relativeURL;

    private LessLookAheadReader         reader;

    private HashMap<String, Expression> variables     = new HashMap<>();

    private List<Formattable>           rules         = new ArrayList<>();

    private int                         rulesIdx;

    private HashMultimap<String, Rule>  mixins        = new HashMultimap<>();

    /**
     * A StringBuilder which can reused inside one method. Do not call another method that also use it.
     */
    private final StringBuilder         cachesBuilder = new StringBuilder();

    private ArrayDeque<Rule>            ruleStack     = new ArrayDeque<>();

    private List<LazyImport>            lazyImports;

    /**
     * Get the parsed rules
     * 
     * @return the rules
     */
    List<Formattable> getRules() {
        return rules;
    }

    public HashMap<String, Expression> getVariables() {
        return variables;
    }

    void parse( URL baseURL, Reader input ) {
        this.baseURL = baseURL;
        try {
            this.relativeURL = new URL( "file", null, "" );
        } catch( Exception th ) {
            throw new LessException( th ); //should never occur
        }
        this.reader = new LessLookAheadReader( input, null, false );
        parse();
    }

    void parseLazy( CssFormatter formatter ) {
        if( lazyImports != null ) {
            HashMap<String, Expression> vars = variables;
            formatter.addVariables( vars );
            for( int i = 0; i < lazyImports.size(); i++ ) {
                LazyImport lazyImport = lazyImports.get( i );
                String filename = lazyImport.stringValue( formatter );
                baseURL = lazyImport.getBaseUrl();
                variables = lazyImport.getVariables();
                rulesIdx = lazyImport.lastRuleBefore() == null ? 0 : rules.indexOf( lazyImport.lastRuleBefore() ) + 1 ;
                importFile( this, filename );
            }
            formatter.removeVariables( vars );
            variables = vars;
        }
    }

    private void parse() {
        try {
            
            for( ;; ) {
                int ch = reader.nextBlockMarker();
                switch( ch ) {
                    case -1:
                        return; //TODO
                    case ';':
                        parseSemicolon( this );
                        break;
                    case '{':
                        parseBlock( this );
                        break;
                    default:
                        throw createException( "Unrecognized input: '" + reader.getLookAhead() + "'" );
                }
            }
        } catch( LessException ex ) {
            ex.addPosition( reader.getFileName(), reader.getLine(), reader.getColumn() );
            throw ex;
        } catch( RuntimeException ex ) {
            LessException lessEx = new LessException( ex );
            lessEx.addPosition( reader.getFileName(), reader.getLine(), reader.getColumn() );
            throw lessEx;
        }
    }

    private void parseSemicolon( FormattableContainer currentRule ) {
        String selector = null;
        Operation params = null;
        StringBuilder builder = cachesBuilder;
        LOOP: for( ;; ) {
            char ch;
            try {
                ch = read();
            } catch( Exception e ) {
                ch = ';'; // a not terminated line is like a lime with semicolon
            }
            switch( ch ) {
                case ':':
                    if( isMixin( builder ) ) {
                        builder.append( ch );
                        continue LOOP;
                    }
                    String name = trim( builder );
                    Expression value = parseExpression( (char)0 );
                    ch = read();
                    switch( ch ) {
                        case '}': //last line in a block does not need a semicolon
//                            back( ch );
                            break;
                        case ';':
                            break;
                        default:
                            throw createException( "Unrecognized input: '" + ch + "'" );
                    }
                    currentRule.add( new RuleProperty( name, value ) );
                    return;
                case '@':
                    ch = read();
                    if( ch == '{' ) { // @{  --> a inline variable and not a variable declaration
                        builder.append( '@' );
                        builder.append( ch );
                        do {
                            ch = read();
                            builder.append( ch );
                        } while( ch != '}' );
                        break;
                    } else {
                        back( ch );
                    }
                    throwUnrecognizedInputIfAny( builder, ch );
                    variable( currentRule.getVariables(), currentRule );
                    return;
                case '/':
                    if( !comment( isWhitespace( builder ) ? currentRule : null ) ) {
                        builder.append( ch );
                    }
                    break;
                case '(':
                    if( !reader.nextIsMixinParam( false ) ) {
                        builder.append( ch );
                        break;
                    }
                    selector = trim( builder );
                    params = parseParameterList();
                    break;
                case ';':
                case '}':
                    String sel = trim( builder );
                    if( !sel.isEmpty() ) {
                        if( selector == null ) {
                            selector = sel;
                        } else {
                            selector += ' ' + sel; // "!important" attribute
                        }
                    }
                    if( selector != null ) {
                        if( selector.contains( ":extend(" ) ) {
                            currentRule.add( new LessExtend( reader, selector, ruleStack ) );
                        } else {
                            Mixin mixin = new Mixin( reader, selector, params, mixins );
                            currentRule.add( mixin );
                        }
                    }
                    return;
                default:
                    builder.append( ch );
            }
        }
    }

    void parseBlock( FormattableContainer currentRule ) {
        Operation expr = null;
        String name = null;
        Expression guard = null;
        StringBuilder builder = cachesBuilder;
        boolean wasWhite = false;
        for( ;; ) {
            char ch = reader.read();
            switch( ch ) {
                case '{':
                    String selector;
                    if( name == null ) {
                        selector = trim( builder );
                    } else {
                        selector = name;
                        name = null;
                        throwUnrecognizedInputIfAny( builder, ch );
                    }
                    if( selector.endsWith( "@" ) ) { // selector with variable name like abc@{foo}abc
                        builder.append( selector );
                        builder.append( ch );
                        break;
                    }
                    if( selector.contains( ":extend(" ) ) {
                        LessExtend lessExtend = new LessExtend( reader, selector, ruleStack );
                        currentRule.add( lessExtend );
                        selector = lessExtend.getSelector();
                    }
                    Rule rule = rule( selector, expr, guard );
                    currentRule.add( rule );
                    expr = null;
                    guard = null;
                    String[] selectors = rule.getSelectors();
                    for( String sel : selectors ) {
                        mixins.add( sel.trim(), rule );
                    }
                    return;
                case '/':
                    if( !comment( isWhitespace( builder ) ? currentRule : null ) ) { //TODO quotes in selectors
                        builder.append( ch );
                    }
                    wasWhite = false;
                    break;
                case '(':
                    String cmd;
                    if( name == null ) {
                        if( (isSelector(builder) || !reader.nextIsMixinParam( true )) && builder.indexOf( " when " ) < 0 ) {
                            builder.append( ch );
                            break;
                        }
                        name = trim( builder );
                        int idx = name.indexOf( ' ' );
                        if( idx < 0 ) {
                            expr = parseParameterList();
                            break;
                        } else {
                            cmd = name.substring( idx + 1 );
                            name = name.substring( 0, idx );
                        }
                    } else {
                        cmd = trim( builder );
                    }
                    char operator;
                    boolean isNot = false;
                    switch( cmd ) {
                        case "when not":
                            isNot = true;
                            //$FALL-THROUGH$
                        case "when":
                            operator = 0;
                            break;
                        case "and not":
                            isNot = true;
                            //$FALL-THROUGH$
                        case "and":
                            operator = '&';
                            break;
                        case ",not":
                        case ", not":
                            isNot = true;
                            //$FALL-THROUGH$
                        case ",":
                            operator = '|';
                            break;
                        default:
                            throw createException( "Unrecognized input: '" + cmd + "'" );
                    }
                    Expression right = parseExpression( (char)0 );
                    ch = read();
                    if( ch != ')' ) {
                        throw createException( "Unrecognized input: '" + ch + "'" );
                    }
                    if( isNot ) {
                        right = new Operation( reader, right, '!' );
                    }
                    guard = ( operator == 0 ) ? right : concat( guard, operator, right );
                    break;
                default:
                    boolean isWhite = Character.isWhitespace( ch );
                    if( isWhite ) {
                        if( builder.length() == 0 ) {
                            break;
                        }
                        if( wasWhite ) {
                            break;
                        } else {
                            builder.append( ' ' );
                            wasWhite = true;
                        }
                    } else {
                        builder.append( ch );
                        wasWhite = false;
                    }
            }
        }
    }

    private void variable( HashMap<String, Expression> variables, FormattableContainer currentRule ) {
        StringBuilder builder = cachesBuilder;
        builder.append( '@' );
        char ch;
        LOOP: for( ;; ) {
            ch = read();
            switch( ch ) {
                case ':':
                    if( !isVariableName(builder) ) {
                        builder.append( ch );
                        continue;
                    }
                    break LOOP;
                case ';':
                    String name = trim( builder );
                    if( name.startsWith( "@import " ) ) {
                        importFile( currentRule, name.substring( 8 ).trim() );
                        return;
                    }
                    currentRule.add( new CssAtRule( reader, name + ';') ); // directives like @charset "UTF-8";
                    return;
//                    throw createException( "Unrecognized input: '" + name + "'" );
                default:
                    builder.append( ch );
            }
        }
        String name = trim( builder );
        Expression value = parseExpression( (char)0 );
        ch = read();
        if( ch == '}' ) {
            back( ch );
        } else {
            if( ch != ';' ) {
                throw createException( "Unrecognized input: '" + ch + "'" );
            }
        }
        variables.put( name, value );
    }

    private void importFile( FormattableContainer currentRule, final String name ) {
        if( currentRule != this ) {
            //import is inside of a mixin and will be process if the mixin will be process
            currentRule.add( new CssAtRule( reader, "@import " + name + ';') );
            return;
        }
        String filename = name.trim();
        boolean isReference = reader.isReference();
        boolean isCss = false;
        boolean isLess = false;
        if( filename.startsWith( "(" ) ) {
            int endIdx = filename.indexOf( ')', 1);
            if( endIdx > 0 ) {
                StringTokenizer tokenizer = new StringTokenizer( filename.substring( 1, endIdx ), "," );
                filename = name.substring( endIdx + 1 ).trim();
                while( tokenizer.hasMoreTokens() ) {
                    String keywordStr = tokenizer.nextToken().trim();
                    switch( keywordStr ) {
                        case "inline":
                        case "once":
                        case "multiple":
                        case "optional":
                            System.err.println( "not implemented @import keyword: " + keywordStr ); //TODO
                            break;
                        case "less":
                            isLess = true;
                            isCss = false;
                            break;
                        case "css":
                            isCss = true;
                            isLess = false;
                            break;
                        case "reference":
                            isReference = true;
                            break;
                        default:
                            throw new LessException( "Unknown @import keyword: " + keywordStr );
                    }
                }
            }
        }

        Object[] old = { reader, baseURL, relativeURL }; //store on the heap to reduce the stack size
        try {
            int i;
            boolean isURL;
            if( filename.startsWith( "url(" ) ) {
                i = 4;
                isURL = true;
            } else {
                i = 0;
                isURL = false;
            }
            StringBuilder builder = cachesBuilder;
            String media = null;
            char quote = 0;
            LOOP:
            for( ; i < filename.length(); i++ ) {
                char ch = filename.charAt( i );
                switch(ch){
                    case '\"':
                    case '\'':
                        if( quote == 0 ) {
                            quote = ch;
                        } else {
                            quote = 0;
                            if( !isURL ) {
                                break LOOP;
                            }
                        }
                        break;
                    case '\\':
                        builder.append( filename.charAt( ++i ) );
                        break;
                    case ')':
                        if( quote == 0 ) {
                            break LOOP;
                        }
                        //$FALL-THROUGH$
                    default:
                        builder.append( ch );
                }
            }
            if( i < filename.length() - 1 ) {
                //additional content after url(...)
                media = filename.substring( i + 1 ).trim();
            }
            filename = trim( builder );

            if( filename.contains( "@{" ) ) { // filename with variable name, we need to parse later
                HashMap<String, Expression> importVariables = new DefaultedHashMap<>( variables );
                variables = new DefaultedHashMap<>( importVariables );
                Formattable lastRuleBefore = rules.size() == 0 ? null : rules.get( rules.size() - 1 );
                LazyImport lazy = new LazyImport( reader, baseURL, filename, importVariables, lastRuleBefore );
                if( lazyImports == null ) {
                    lazyImports = new ArrayList<>();
                }
                lazyImports.add( lazy );
                return;
            }
            if( !isLess && (isCss || filename.endsWith( "css" )) ) {
                // filenames ends with "css" will not be inline else a CSS @import directive is written
                currentRule.add( new CssAtRule( reader, "@import " + name + ';') );
                return;
            }
            baseURL = baseURL == null ? new URL( filename ) : new URL( baseURL, filename );
            if( !isLess && baseURL.getPath().endsWith( "css" ) ) {
                // URL path ends with "css" will not be inline else a CSS @import directive is written
                currentRule.add( new CssAtRule( reader, "@import " + name + ';') );
                return;
            }
            if( "file".equals( baseURL.getProtocol() ) && filename.lastIndexOf( '.' ) <= filename.lastIndexOf( '/' ) ) {
                filename += ".less";
                baseURL = (URL)old[1];
                baseURL = baseURL == null ? new URL( filename ) : new URL( baseURL, filename );
            }
            relativeURL = new URL( relativeURL, filename );
            if( isReference != reader.isReference() ) {
                add( new ReferenceInfo( isReference ) );
            }
            reader = new LessLookAheadReader( new InputStreamReader( baseURL.openStream(), StandardCharsets.UTF_8 ), filename, isReference );
            parse();
            reader.close();
        } catch( LessException ex ) {
            throw ex;
        } catch( Exception ex ) {
            throw new LessException( ex );
        } finally {
            reader = (LessLookAheadReader)old[0];
            baseURL = (URL)old[1];
            relativeURL = (URL)old[2];
            if( isReference != reader.isReference() ) {
                add( new ReferenceInfo( reader.isReference() ) );
            }
        }
    }

    private Rule rule( String selector, Operation params, Expression guard ) {
        Rule rule = new Rule( reader, selector, params, guard );
        parseRule( rule );
        return rule;
    }
    
    private void parseRule( Rule rule ) {
        ruleStack.add( rule );
        for( ;; ) {
            int ch = reader.nextBlockMarker();
            switch( ch ) {
                case -1:
                    throw createException( "Unexpected end of Less data" );
                case ';':
                    parseSemicolon( rule );
                    break;
                case '{':
                    parseBlock( rule );
                    break;
                case '}':
                    parseSemicolon( rule );
                    ruleStack.removeLast();
                    return;
                default:
                    throw createException( "Unrecognized input: '" + reader.getLookAhead() + "'" );
            }
        }

    }
    
/*    private void rule( Rule rule ) {
        StringBuilder builder = cachesBuilder;
        Operation expr = null;
        LOOP: for( ;; ) {
            char ch = read();
            switch( ch ) {
                case ':':
                    switch( firstNonWhitespace( builder ) ) { 
                        case '&': // pseudo-selectors
                        case '.': // Mixin
                        case '#': // Mixin
                            builder.append( ch );
                            continue LOOP;
                    }
                    String name = trim( builder );
                    Expression value = parseExpression( (char)0 );
                    ch = read();
                    switch( ch ) {
                        case '}': //last line in a block does not need a semicolon
                            back( ch );
                            break;
                        case ';':
                            break;
                        default:
                            throw createException( "Unrecognized input: '" + ch + "'" );
                    }
                    rule.add( new RuleProperty( name, value ) );
                    break;
                case ';':
                    rule.add( new Mixin( trim( builder ), expr, mixins ) );
                    expr = null;
                    break;
                case '}':
                    throwUnrecognizedInputIfAny( builder, ch );
                    return;
                case '{': // nested rules
                    name = trim( builder );
                    rule.add( rule( name, expr, null ) );
                    expr = null;
                    break;
                case '(':
                    name = trim( builder );
                    expr = parseParameterList();
                    builder.append( name );
                    break;
                case '/':
                    String comment = comment();
                    if( comment != null && isWhitespace( builder ) ) {
                        rule.add( new Comment( comment ) );
                    }
                    break;
                case '@':
                    throwUnrecognizedInputIfAny( builder, ch );
                    variable( rule.getVariables(), rule );
                    break;
                default:
                    builder.append( ch );
            }
        }
    }*/

    private Expression parseExpression( char leftOperator ) {
        StringBuilder builder = cachesBuilder;
        Expression left = null;
        boolean wasWhite = false;
        LOOP: for( ;; ) {
            char ch = read();
            switch( ch ) {
                case '-':
                    if( builder.length() == 0 && left == null ) {
                        builder.append( ch );
                        wasWhite = false;
                        break;
                    } else {
                        char ch2 = read();
                        if( Character.isWhitespace( ch2 ) ) {
                            back( ch2 );
                            // continue as operation
                        } else {
                            if( wasWhite ) {
                                if( Operation.level( leftOperator ) >= Operation.level( ' ' ) ) {
                                    back( ch2 );
                                    back( ch );
                                    back( ' ' );
                                    if( builder.length() > 0 ) {
                                        left = concat( left, ' ', buildExpression( trim( builder ) ) );
                                    }
                                    if( left == null ) {
                                        throw createException( "Unrecognized input: '" + ch + "'" );
                                    }
                                    return left;
                                }
                                if( builder.length() > 0 ) {
                                    left = concat( left, ' ', buildExpression( trim( builder ) ) );
                                }
                                builder.append( ch );
                                builder.append( ch2 );
                                wasWhite = false;
                                break;
                             } else {
                                if( isIdentifier( builder ) ) {
                                    builder.append( ch );
                                    back( ch2 );
                                    wasWhite = false;
                                    break;
                                } else {
                                    back( ch2 );
                                    // continue as operation
                                }
                             }
                        }
                    }
                    //$FALL-THROUGH$
                case '+':
                    if( ch == '+' && builder.length() == 1 && builder.charAt( 0 ) == 'U' ) {
                        //unicode-range syntax
                        builder.append( ch );
                        break;
                    }
                    if( builder.length() == 0 && left == null ) {
                        builder.append( ch );
                        wasWhite = false;
                        break;
                    }
                    //$FALL-THROUGH$
                case '*':
                case '/':
                case ',':
                case '=':
                case '>':
                case '<':
                    if( Operation.level( leftOperator ) >= Operation.level( ch ) ) {
                        back( ch );
                        if( builder.length() > 0 ) {
                            left = concat( left, ' ', buildExpression( trim( builder ) ) );
                        }
                        if( left == null ) {
                            throw createException( "Unrecognized input: '" + ch + "'" );
                        }
                        return left;
                    }
                    switch( ch ) {
                        case '/':
                            if( comment( null ) ) {
                                continue LOOP;
                            }
                            break;
                        case '>':
                            char ch2 = read();
                            if( ch2 == '=' ) {
                                ch = '≥';
                            } else {
                                back( ch2 );
                            }
                            break;
                        case '<':
                            ch2 = read();
                            if( ch2 == '=' ) {
                                ch = '≤';
                            } else {
                                back( ch2 );
                            }
                            break;
                        case '=':
                            ch2 = read();
                            switch( ch2 ) {
                                case '<':
                                    ch = '≤';
                                    break;
                                case '>':
                                    ch = '≤';
                                    break;
                                default:
                                    back( ch2 );
                            }
                            break;
                    }
                    wasWhite = false;
                    if( left == null ) {
                        String str = trim( builder );
                        if( str.isEmpty() ) {
                            if( ch == ':' ) { // Selector ?
                                builder.append( ch );
                                break;
                            }
                            throw createException( "Unrecognized input: '" + ch + "'" );
                        }
                        left = buildExpression( str );
                    } else {
                        if( builder.length() > 0 ) {
                            left = concat( left, ' ', buildExpression( trim( builder ) ) );
                        }
                    }
                    left = concat( left, ch, parseExpression( ch ) );
                    break;
                case ':': // Key value parameter like @color:#abc
                    if( left == null ) {
                        if( builder.length() == 0 || builder.charAt( 0 ) != '@' ) {
                            builder.append( ':' );
                            break;
                        }
                        String str = trim( builder );
                        left = buildExpression( str );
                    } else {
                        throwUnrecognizedInputIfAny( builder, ch );
                    }
                    left = concat( left, ch, parseExpression( ch ) );
                    break;
                case '(':
                    String str = trim( builder );
                    if( wasWhite && str.length() > 0 ) {
                        left = concat( left, ' ', buildExpression( str ) );
                        str = "";
                    }
                    wasWhite = false;
                    Expression right;
                    switch( str ) {
                        case "url":
                            right = new FunctionExpression( reader, str, parseUrlParam() );
                            break;
                        case "data-uri":
                        case "colorize-image":
                            Operation op = parseParameterList();
                            op.addLeftOperand( new ValueExpression( reader, relativeURL.toString() ) );
                            right = new FunctionExpression( reader, str, op );
                            break;
                        case "e":
                            op = parseParameterList();
                            right = new Operation( reader, op.getOperands().get( 0 ), '~' );
                            break;
                        default:
                            right = new FunctionExpression( reader, str, parseParameterList() );
                    }
                    left = concat( left, ' ', right );
                    break;
                case ';':
                case ')':
                case '}':
                    back( ch );
                    str = trim( builder );
                    if( !str.isEmpty() ) {
                        left = concat( left, ' ', buildExpression( str ) );
                    }
                    if( left == null ) {
                        if( leftOperator != 0 ) {
                            throw createException( "Unrecognized input: '" + leftOperator + "'" );
                        }
                        if( ch != ')' ) {
                            left = new ValueExpression( reader, "" ); // empty value
                        }
                    }
                    return left;
                case '~': // escape operator
                    if( wasWhite && builder.length() > 0 ) {
                        left = concat( left, ' ', buildExpression( trim( builder ) ) );
                    } else {
                        throwUnrecognizedInputIfAny( builder, ch );
                    }
                    wasWhite = false;
                    left = concat( left, ' ', new Operation( reader, parseExpression( ch ), ch ) );
                    break;
                case '"':
                case '\'':
                    if( wasWhite && builder.length() > 0 ) {
                        left = concat( left, ' ', buildExpression( trim( builder ) ) );
                    } else {
                        throwUnrecognizedInputIfAny( builder, ch );
                    }
                    wasWhite = false;
                    left = concat( left, ' ', new ValueExpression( reader, readQuote( ch ) ) );
                    break;
                case '`':
                    left = concat( left, ' ', new JavaScriptExpression( reader, readQuote( ch ) ) );
                    break;
                default:
                    boolean isWhite = Character.isWhitespace( ch );
                    if( isWhite ) {
                        if( builder.length() == 0 && left == null ) {
                            break;
                        }
                        wasWhite = true;
                    } else {
                        if( wasWhite ) {
                            if( builder.length() > 0 ) {
                                if( Operation.level( leftOperator ) >= Operation.level( ' ' ) ) {
                                    back( ch );
                                    back( ' ' );
                                    return buildExpression( trim( builder ) );
                                }
                                left = concat( left, ' ', buildExpression( trim( builder ) ) );
                            }
                            if( left != null ) {
                                back( ch );
                                left = concat( left, ' ', parseExpression( ' ' ) );
                                wasWhite = false;
                                break;
                            }
                            wasWhite = false;
                        }
                        builder.append( ch );
                    }
                    break;
            }
        }
    }
        
    Operation parseParameterList() {
        Expression left = null;
        char ch;
        do {
            Expression expr = parseExpression( (char)0 );
            left = concat( left, ';', expr );
            ch = read();
        } while( ch == ';' );
        if( ch != ')' ) {
            throw createException( "Unrecognized input: '" + ch + "'" );
        }
        if( left == null ) {
            return new Operation( reader );
        }
        if( left.getClass() == Operation.class ) {
            switch( ((Operation)left).getOperator() ) {
                case ',':
                case ';':
                    return (Operation)left;
            }
        }
        return new Operation( reader, left, ',' );
    }
    
    private String readQuote( char quote ) {
        StringBuilder builder = cachesBuilder;
        builder.setLength( 0 );
        builder.append( quote );
        boolean isBackslash = false;
        for( ;; ) {
            char ch = read();
            builder.append( ch );
            if( ch == quote && !isBackslash ) {
                String str = builder.toString();
                builder.setLength( 0 );
                return str;
            }
            isBackslash = ch == '\\';
        }
    }

    private Operation parseUrlParam() {
        StringBuilder builder = cachesBuilder;
        builder.setLength( 0 );
        Operation op = new Operation( reader, new ValueExpression( reader, relativeURL.getPath() ), ';' );
        for( ;; ) {
            char ch = read();
            switch( ch ) {
                case '"':
                case '\'':
                    String val = builder.toString();
                    String quote = readQuote( ch );
                    builder.append( val );
                    builder.append( quote );
                    break;
                case '\\': //escape
                    builder.append( ch );
                    builder.append( read() );
                    break;
                case '@':
                    if( builder.length() == 0 ) {
                        reader.back( ch );
                        op.addOperand( parseExpression( (char)0 ) );
                        read();
                        return op;
                    }
                    builder.append( ch );
                    break;
                case ')':
                    val = trim( builder );
                    op.addOperand( new ValueExpression( reader, val ) );
                    return op;
                default:
                    builder.append( ch );
            }
        }
    }

    /**
     * Concatenate 2 expressions to one expression.
     * 
     * @param left
     *            the left, can be null
     * @param right
     *            the right, can not be null
     * @return the resulting expression
     */
    private Expression concat( Expression left, char operator, Expression right ) {
        if( left == null ) {
            return right;
        }
        if( right == null ) {
            return left;
        }
        Operation op;
        if( left.getClass() == Operation.class && ((Operation)left).getOperator() == operator ) {
            op = (Operation)left;
        } else {
            op = new Operation( reader, left, operator );
        }
        op.addOperand( right );
        return op;
    }

    private Expression buildExpression( String str ) {
        switch( str.charAt( 0 ) ) {
            case '@':
                return new VariableExpression( reader, str );
            case '-':
                if( str.startsWith( "-@" ) ) {
                    return new FunctionExpression( reader, "-", new Operation( reader, buildExpression( str.substring( 1 ) ), (char)0 ) );
                }
                //$FALL-THROUGH$
            default:
                return new ValueExpression( reader, str );
        }
    }

    /**
     * Parse comments
     * 
     * @param container optional container for the parsed comments
     * 
     * @return return true, if a comment was parsed, false if the slash must be parse anywhere else
     */
    private boolean comment( FormattableContainer container ) {
        char ch = read();
        switch( ch ) {
//            case '/': // line comments will be skip
//                reader.skipLine();
//                return true;
            case '*': // block comments
                StringBuilder builder = new StringBuilder();
                builder.append( "/*" );
                boolean wasAsterix = false;
                for( ;; ) {
                    ch = read();
                    builder.append( ch );
                    switch( ch ) {
                        case '*':
                            wasAsterix = true;
                            break;
                        case '/':
                            if( wasAsterix ) {
                                if( container != null ) {
                                    container.add( new Comment( trim( builder ) ) );
                                }
                                return true;
                            }
                            //$FALL-THROUGH$
                        default:
                            wasAsterix = false;
                    }
                }
            default:
                back( ch );
                return false;
        }
    }

    /**
     * Read a single character from reader or from back buffer
     * 
     * @return a character or -1 if EOF
     */
    private char read() {
        return reader.read();
    }

    /**
     * Push a char back to the stream
     * 
     * @param ch
     *            the char
     */
    private void back( char ch ) {
        reader.back( ch );
    }

    /**
     * Get a trim string from the builder and clear the builder.
     * 
     * @param builder
     *            the builder.
     * @return a trim string
     */
    private String trim( StringBuilder builder ) {
        //TODO can be optimize
        String str = builder.toString().trim();
        builder.setLength( 0 );
        return str;
    }

    /**
     * If the builder is empty or contains only whitespaces
     * 
     * @param builder
     *            the builder.
     * @return true if there is no content
     */
    private boolean isWhitespace( StringBuilder builder ) {
        for( int i = 0; i < builder.length(); i++ ) {
            if( !Character.isWhitespace( builder.charAt( i ) ) ) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * If the builder contains a Mixin name.
     * 
     * @param builder
     *            the builder.
     * @return true if content can be an Mixin
     */
    private boolean isMixin( StringBuilder builder ){
        switch( firstNonWhitespace( builder ) ) { 
            case '&': // pseudo-selectors
            case '.': // Mixin
            case '#': // Mixin
                return true;
        }
        return false;
    }

    /**
     * If the builder contains a selector and not a mixin.
     * @param builder the builder
     * @return true, if it 100% a selector and not a mixin with parameters
     */
    private boolean isSelector( StringBuilder builder ) {
        int length = builder.length();
        if( length == 0 ) {
            return false;
        }
        switch( builder.charAt( 0 ) ) { 
            case '.': // Mixin
            case '#': // Mixin
                break;
            default:
                return true;
        }
        for( int i = 1; i < length; i++ ) {
            char ch = builder.charAt( i );
            switch( ch ) {
                case '>':
                case ':':
                    return true;
            }
        }
        return false;
    }

    /**
     * Check if the current content is an identifier or an expression. This is call to decide if a minus is part of this
     * identifier or an operation. Identifier are:
     * <li>variables
     * <li>mixins
     * <li>selectors
     * </li>
     * 
     * @param builder
     *            the builder to check
     * @return
     */
    private boolean isIdentifier( StringBuilder builder ) {
        char ch = 0;
        int i;
        for( i = 0; i < builder.length(); ) {
            ch = builder.charAt( i++ );
            if( !Character.isWhitespace( ch ) ) {
                break;
            }
        }
        FIRST: do {
            switch( ch ) {
                case '&': // pseudo-selectors
                case '.': // Mixin
                case '#': // Mixin
                case '@': // variable
                    return true;
                case '-':
                    if( i < builder.length() ) {
                        ch = builder.charAt( i++ );
                    }
                    continue FIRST;
                case 'U':
                    if( builder.length() > 1 && builder.charAt( 1 ) == '+' ) {
                        //unicode-range
                        return true;
                    }
            }
            break;
        } while( true );
        if( Character.isLetter( ch ) ) {
            return true;
        }
        return false;
    }

    /**
     * If the builder contains a valid variable name
     * @param builder the builder
     * @return true, if a variable; false, if it is an at-rule.
     */
    private boolean isVariableName( StringBuilder builder ) {
        for( int i = 1; i < builder.length(); ) {
            char ch = builder.charAt( i++ );
            switch( ch ) {
                case '(':
                case '\"':
                    return false;
            }
        }
        return true;
    }

    /**
     * The first character in the builder that is not a whitespace. Else there are only whitespaces
     * 
     * @param builder
     *            the builder.
     * @return the first non whitespace
     */
    private char firstNonWhitespace( StringBuilder builder ) {
        for( int i = 0; i < builder.length(); i++ ) {
            if( !Character.isWhitespace( builder.charAt( i ) ) ) {
                return builder.charAt( i );
            }
        }
        return ' ';
    }

    /**
     * Throw an unrecognized input exception if there content in the StringBuilder.
     * 
     * @param builder
     */
    private void throwUnrecognizedInputIfAny( StringBuilder builder, int ch ) {
        if( !isWhitespace( builder ) ) {
            throw createException( "Unrecognized input: '" + trim( builder ) + (char)ch + "'" );
        }
        builder.setLength( 0 );
    }

    /**
     * Create a Exception with a message. Can be add more information later
     * 
     * @param msg
     *            the message
     * @return the created exception
     */
    private LessException createException( String msg ) {
        return new LessException( msg );
    }

    @Override
    public void add( Formattable formattable ) {
        if( formattable.getClass() == Rule.class && ((Rule)formattable).isMixin() ) {
            return;
        }
        rules.add( rulesIdx++, formattable );
    }
}
