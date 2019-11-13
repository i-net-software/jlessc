/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 - 2019 Volker Berlin
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
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

/**
 * The parser of the less stream.
 */
class LessParser implements FormattableContainer {

    private boolean                     strictMath;

    private int                         nesting;

    private URL                         baseURL;

    private ReaderFactory               readerFactory;

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

    private HashSet<URL>                imports       = new HashSet<>();

    private List<LazyImport>            lazyImports;

    /**
     * Get the parsed rules
     * 
     * @return the rules
     */
    List<Formattable> getRules() {
        return rules;
    }

    /**
     * {@inheritDoc}
     */
    public HashMap<String, Expression> getVariables() {
        return variables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HashMultimap<String, Rule> getMixins() {
        return mixins;
    }

    /**
     * Main method for parsing of main less file.
     * 
     * @param baseURL
     *            the baseURL for import of external less data.
     * @param input
     *            the less input data
     * @param readerFactory
     *            A factory for the readers for imports.
     * @throws MalformedURLException
     *             Should never occur
     * @throws LessException
     *             if any parsing error occurred
     */
    void parse( URL baseURL, Reader input, ReaderFactory readerFactory  ) throws MalformedURLException, LessException {
        this.baseURL = baseURL;
        this.readerFactory = readerFactory;
        this.relativeURL = new URL( "file", null, "" );
        this.reader = new LessLookAheadReader( input, null, false, false );
        parse( this );
    }

    /**
     * If there are some imports with variables then this will parse after a formatter if available.
     * @param formatter the formatter to evaluate variables
     */
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

    /**
     * Parse main and import less files.
     * @param currentRule the current container. This can be the root or a rule.
     */
    private void parse( FormattableContainer currentRule ) {
        try {
            for( ;; ) {
                int ch = reader.nextBlockMarker();
                switch( ch ) {
                    case -1:
                        return; // end of input reached
                    case ';':
                        parseSemicolon( currentRule );
                        break;
                    case '{':
                        parseBlock( currentRule );
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

    /**
     * Parse an expression which ends with a semicolon. This can be a property assignment, variable assignment, a directive or other. 
     * @param currentRule the current container. This can be the root or a rule.
     */
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
                    strictMath = "font".equals( name );
                    Expression value = parseExpression( (char)0 );
                    strictMath = false;
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
                            LessExtend.addLessExtendsTo( currentRule, reader, selector );
                        } else {
                            Mixin mixin = new Mixin( reader, selector, params, currentRule.getMixins() );
                            currentRule.add( mixin );
                        }
                    }
                    return;
                default:
                    builder.append( ch );
            }
        }
    }

    /**
     * Parse a block including also the selectors.
     * 
     * @param currentRule container of the block
     */
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
                        selector = LessExtend.addLessExtendsTo( currentRule, reader, selector );
                    }
                    Rule rule = rule( currentRule, selector, expr, guard );
                    currentRule.add( rule );
                    expr = null;
                    guard = null;
                    String[] selectors = rule.getSelectors();
                    for( String sel : selectors ) {
                        currentRule.getMixins().add( sel.trim(), rule );
                    }
                    return;
                case '/':
                    if( !comment( isWhitespace( builder ) ? currentRule : null ) ) {
                        builder.append( ch );
                    }
                    wasWhite = false;
                    break;
                case '\"':
                case '\'':
                    readQuote( ch, builder );
                    wasWhite = false;
                    break;
                case '(':
                    String cmd;
                    if( name == null ) {
                        int idx = builder.lastIndexOf( " when" );
                        if( idx < 0 || (idx + 5 != builder.length() && builder.indexOf( " when " ) < 0) ) {
                            if( isSelector( builder ) || !reader.nextIsMixinParam( true ) ) {
                                builder.append( ch );
                                break;
                            }
                        }
                        name = trim( builder );
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

    /**
     * Parse an expression which starts with an "@".
     * 
     * @param variables container for variables
     * @param currentRule parent container which contains the variable
     */
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
                    if( name.endsWith( "()" ) ) {
                        currentRule.add( new VariableExpression( reader, name.substring( 0, name.length() - 2 ) ) ); // reference to detached ruleset
                    } else {
                        currentRule.add( new CssAtRule( reader, name + ';', true ) ); // directives like @charset "UTF-8";
                    }
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

    /**
     * Parse and handle an <code>@import</code> directive.
     * 
     * @param currentRule current container. This can be the root or a rule.
     * @param name the content of the directive like a file name and keywords
     * @throws LessException if any parsing error occurred
     */
    private void importFile( FormattableContainer currentRule, final String name ) {
        String filename = name.trim();
        boolean isReference = reader.isReference();
        boolean isCss = false;
        boolean isLess = false;
        boolean isMultiple = reader.isMultiple();
        boolean isInline = false;
        boolean isOptional = false;
        if( filename.startsWith( "(" ) ) {
            int endIdx = filename.indexOf( ')', 1);
            if( endIdx > 0 ) {
                StringTokenizer tokenizer = new StringTokenizer( filename.substring( 1, endIdx ), "," );
                filename = name.substring( endIdx + 1 ).trim();
                while( tokenizer.hasMoreTokens() ) {
                    String keywordStr = tokenizer.nextToken().trim();
                    switch( keywordStr ) {
                        case "inline":
                            isInline = true;
                            break;
                        case "optional":
                            isOptional = true;
                            break;
                        case "once":
                            isMultiple = false;
                            break;
                        case "multiple":
                            isMultiple = true;
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
                Rule rule = new Rule( reader, currentRule, "@media " + media, null, null );
                currentRule.add( rule );
                currentRule = rule;
            }
            filename = trim( builder );

            if( filename.contains( "@{" ) ) { // filename with variable name, we need to parse later
                if( currentRule != this ) {
                    //import is inside of a mixin and will be process if the mixin will be process
                    currentRule.add( new CssAtRule( reader, "@import " + name + ';', true ) );
                    return;
                }
                HashMap<String, Expression> importVariables = new DefaultedHashMap<>( variables );
                variables = new DefaultedHashMap<>( importVariables );
                Formattable lastRuleBefore = rules.size() == 0 ? null : rules.get( rules.size() - 1 );
                LazyImport lazy = new LazyImport( reader, baseURL, name, importVariables, lastRuleBefore );
                if( lazyImports == null ) {
                    lazyImports = new ArrayList<>();
                }
                lazyImports.add( lazy );
                return;
            }
            if( !isLess && !isInline && (isCss || filename.endsWith( "css" )) ) {
                // filenames ends with "css" will not be inline else a CSS @import directive is written
                currentRule.add( new CssAtRule( reader, "@import " + name + ';', true ) );
                return;
            }
            baseURL = baseURL == null ? new URL( filename ) : new URL( baseURL, filename );
            if( !isLess && !isInline && baseURL.getPath().endsWith( "css" ) ) {
                // URL path ends with "css" will not be inline else a CSS @import directive is written
                currentRule.add( new CssAtRule( reader, "@import " + name + ';', true ) );
                return;
            }
            if( "file".equals( baseURL.getProtocol() ) && filename.lastIndexOf( '.' ) <= filename.lastIndexOf( '/' ) ) {
                filename += ".less";
                baseURL = (URL)old[1];
                baseURL = baseURL == null ? new URL( filename ) : new URL( baseURL, filename );
            }
            relativeURL = new URL( relativeURL, filename );
            if( imports.add( baseURL ) || isMultiple ) {
                if( isReference != reader.isReference() ) {
                    add( new ReferenceInfo( isReference ) );
                }
                Reader importReader = readerFactory.create( baseURL );
                if( isInline ) {
                    Scanner scanner = new Scanner(importReader).useDelimiter( "\\A" );
                    if( scanner.hasNext() ) {
                        currentRule.add( new CssAtRule( reader, scanner.next(), false ) );
                    }
                } else {
                    reader = new LessLookAheadReader( importReader, filename, isReference, isMultiple );
                    parse( currentRule );
                    reader.close();
                }
            }
        } catch( LessException ex ) {
            throw ex;
        } catch( IOException ex ) {
            if( !isOptional ) {
                throw new LessException( ex );
            }
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

    /**
     * Create a rule and parse the content of an block.
     * 
     * @param selector the selectors
     * @param parent the parent in the hierarchy
     * @param params the parameters if it is a mixin.
     * @param guard an optional guard expression
     * @return the rule
     */
    @Nonnull
    private Rule rule( FormattableContainer parent, String selector, Operation params, Expression guard ) {
        Rule rule = new Rule( reader, parent, selector, params, guard );
        parseRule( rule );
        return rule;
    }

    /**
     * Parse the content of an block.
     * 
     * @param rule the container for the content
     */
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

    /**
     * Parse the next expression.
     * 
     * @param leftOperator the operator on the left side or 0 if this is the first expression.
     * @return the expression or null if it an empty function parameter list like ()
     */
    private Expression parseExpression( char leftOperator ) {
        StringBuilder builder = cachesBuilder;
        Expression left = null;
        boolean wasWhite = false;
        LOOP: for( ;; ) {
            char ch = read();
            switch( ch ) {
                case '{':
                    if( builder.length() > 0 ) {
                        left = concat( left, ' ', buildExpression( trim( builder ) ) );
                    }
                    Rule rule = rule( this, "", null, null );
                    left = concat( left, ' ', new ValueExpression( rule ) );
                    break;
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
                                    ch = '≥';
                                    break;
                                default:
                                    back( ch2 );
                            }
                            break;
                        default:
                    }
                    wasWhite = false;
                    if( left == null ) {
                        String str = trim( builder );
                        if( str.isEmpty() ) {
                            throw createException( "Unrecognized input: '" + ch + "'" );
                        }
                        left = buildExpression( str );
                    } else {
                        if( builder.length() > 0 ) {
                            left = concat( left, ' ', buildExpression( trim( builder ) ) );
                        }
                    }
                    left = concat( left, ch, parseExpression( ch ) );
                    if( strictMath && nesting == 0 && ch == '/' ) {
                        ((Operation)left).setDataType( Expression.STRING );
                    }
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
                        case "calc":
                            int parenthesisCount = 0;
                            right = null;
                            CALC: do {
                                ch = read();
                                switch( ch ) {
                                    case '~':
                                        if( isWhitespace( builder ) ) {
                                            right = new Operation( reader, parseExpression( ch ), ch );
                                            continue;
                                        }
                                        break;
                                    case '(':
                                        parenthesisCount++;
                                        break;
                                    case ')':
                                        if( parenthesisCount == 0 ) {
                                            String val = trim( builder );
                                            if( right == null ) {
                                                right = new ValueExpression( reader, val, Expression.STRING );
                                            }
                                            break CALC;
                                        }
                                        parenthesisCount--;
                                        break;
                                }
                                builder.append( ch );
                            } while( true );
                            op = new Operation( reader, right, ';' );
                            right = new FunctionExpression( reader, str, op );
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
                case '!':
                    if( builder.length() > 0 ) {
                        left = concat( left, ' ', buildExpression( trim( builder ) ) );
                    }
                    if( left != null ) {
                        for( ;; ) {
                            ch = read();
                            switch( ch ) {
                                case ';':
                                case ')':
                                case '}':
                                    str = trim( builder );
                                    if( str.equals( "important" ) ) {
                                        left.setImportant();
                                        back( ch );
                                    } else {
                                        left = concat( left, ' ', buildExpression( '!' + str ) );
                                    }
                                    return left;
                                default:
                                    builder.append( ch );
                            }
                        }
                    }
                    //$FALL-THROUGH$ 
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

    /**
     * Parse a parameter list for a function.
     * 
     * @return the operation
     */
    @Nonnull
    Operation parseParameterList() {
        Expression left = null;
        char ch;
        do {
            nesting++;
            Expression expr = parseExpression( (char)0 );
            nesting--;
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

    /**
     * Read a quoted string.
     * 
     * @param quote the quote character.
     * @return the string with quotes
     */
    private String readQuote( char quote ) {
        StringBuilder builder = cachesBuilder;
        builder.setLength( 0 );
        readQuote( quote, builder );
        String str = builder.toString();
        builder.setLength( 0 );
        return str;
    }

    /**
     * Read a quoted string and append it to the builder.
     * 
     * @param quote the quote character.
     * @param builder the target
     */
    private void readQuote( char quote, StringBuilder builder ) {
        builder.append( quote );
        boolean isBackslash = false;
        for( ;; ) {
            char ch = read();
            builder.append( ch );
            if( ch == quote && !isBackslash ) {
                return;
            }
            isBackslash = ch == '\\';
        }
    }

    /**
     * Parse the parameter of an "url" function which has some curious fallbacks.
     * 
     * @return the parameter of the function
     */
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
                    op.addOperand( new ValueExpression( reader, val, Expression.STRING ) );
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
     * @param operator
     *            the expression operation
     * @param right
     *            the right, can not be null
     * @return the resulting expression
     */
    private Expression concat( Expression left, char operator, Expression right ) {
        if( left == null ) {
            return right;
        }
        Operation op;
        if( left.getClass() == Operation.class && ((Operation)left).getOperator() == operator ) {
            op = (Operation)left;
        } else if( right != null && right.getClass() == Operation.class && ((Operation)right).getOperator() == operator ) {
            op = (Operation)right;
            op.addLeftOperand( left );
            return op;
        } else {
            op = new Operation( reader, left, operator );
        }
        if( right != null ) {
            op.addOperand( right );
        }
        return op;
    }

    /**
     * Create an expression from the given atomic string.
     * 
     * @param str the expression like a number, color, variable, string, etc
     * @return the expression
     */
    @Nonnull
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
     * @return a character
     * @throws LessException
     *             If an I/O error occurs or EOF
     */
    private char read() throws LessException {
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
    private static String trim( StringBuilder builder ) {
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
    private static boolean isWhitespace( StringBuilder builder ) {
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
    private static boolean isMixin( StringBuilder builder ){
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
    private static boolean isSelector( StringBuilder builder ) {
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
     * @return true, if identifier
     */
    private static boolean isIdentifier( StringBuilder builder ) {
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
                        continue FIRST;
                    }
                    return true; // special case of properties that starts with two minus letters. see https://developer.mozilla.org/de/docs/Web/CSS/var
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
    private static boolean isVariableName( StringBuilder builder ) {
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
    private static char firstNonWhitespace( StringBuilder builder ) {
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
     * @param builder the StringBuilder
     * @param ch the last character
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
        return reader.createException( msg );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add( Formattable formattable ) {
        if( formattable.getClass() == Rule.class && ((Rule)formattable).isMixin() ) {
            return;
        }
        rules.add( rulesIdx++, formattable );
    }
}
