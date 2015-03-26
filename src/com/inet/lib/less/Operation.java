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

import static com.inet.lib.less.ColorUtils.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A arithmetic operation.
 */
class Operation extends AbstractExpression {

    private final ArrayList<Expression> operands = new ArrayList<>();

    private final char                  operator;
    
    private int                         type;

    private static final HashMap<String, HashMap<String, Double>> unitConversions = new HashMap<>();
    static {
        HashMap<String, Double> length = new HashMap<>();
        putConvertion( length, "m", 1 );
        putConvertion( length, "cm", 0.01 );
        putConvertion( length, "mm", 0.001 );
        putConvertion( length, "in", 0.0254 );
        putConvertion( length, "px", 0.0254 / 96 );
        putConvertion( length, "pt", 0.0254 / 72 );
        putConvertion( length, "pc", 0.0254 / 72 * 12 );

        HashMap<String, Double> duration = new HashMap<>();
        putConvertion( duration, "s", 1 );
        putConvertion( duration, "ms", 0.001 );

        HashMap<String, Double> angle = new HashMap<>();
        putConvertion( angle, "rad", 1 / (2 * Math.PI) );
        putConvertion( angle, "deg", 1 / 360.0 );
        putConvertion( angle, "grad", 1 / 400.0 );
        putConvertion( angle, "turn", 1 );
    }

    private static void putConvertion( HashMap<String, Double> group, String unit, double factor ) {
        unitConversions.put(unit, group );
        group.put( unit, factor );
    }

    Operation( LessObject reader, Expression left, char operator ) {
        super( reader, String.valueOf( operator ) );
        if( left != null ) {
            this.operands.add( left );
        }
        this.operator = operator;
    }

    /**
     * Empty parameter list
     */
    Operation( LessObject reader ) {
        super( reader, "," );
        this.operator = ',';
    }

    /**
     * Get the operator of this operation
     * 
     * @return the operator
     */
    char getOperator() {
        return operator;
    }

    /**
     * Get the Operands of this operation
     * 
     * @return the operands
     */
    ArrayList<Expression> getOperands() {
        return operands;
    }

    /**
     * Add the next operand. It must use the same operator like this operation.
     * @param right the next operand
     */
    void addOperand( Expression right ) {
        this.operands.add( right );
    }

    void addLeftOperand( Expression left ) {
        this.operands.add( 0, left );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDataType( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            switch( operator ) {
                case ' ':
                case ',':
                    type = STRING;
                    break;
                case '&':
                case '|':
                case '>':
                case '<':
                case '=':
                case '≥':
                case '≤':
                    type = BOOLEAN;
                    break;
                default:
                    type = maxOperadType( formatter );
            }
        }
        return type;
    }

    private int maxOperadType( CssFormatter formatter ) {
        int dataType = operands.get( 0 ).getDataType( formatter );
        for( int i = 1; i < operands.size(); i++ ) {
            dataType = Math.max( dataType, operands.get( i ).getDataType( formatter ) );
        }
        return dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) {
        switch( getDataType( formatter ) ) {
            case BOOLEAN:
                switch( operator ) {
                    case '=':
                        for( int i = 0; i < operands.size(); i++ ) {
                            if( i > 0 ) {
                                formatter.append( operator );
                            }
                            operands.get( i ).appendTo( formatter );
                        }
                        return;
                }
                break;
            case STRING:
                switch( operator ) {
                    case ' ':
                        for( int i = 0; i < operands.size(); i++ ) {
                            if( i > 0 ) {
                                formatter.append( operator );
                            }
                            operands.get( i ).appendTo( formatter );
                        }
                        return;
                    case ',':
                        for( int i = 0; i < operands.size(); i++ ) {
                            if( i > 0 ) {
                                formatter.append( operator ).space();
                            }
                            operands.get( i ).appendTo( formatter );
                        }
                        return;
                    case '+':
                        for( int i = 0; i < operands.size(); i++ ) {
                            operands.get( i ).appendTo( formatter );
                        }
                        return;
                    case '/':
                        for( int i = 0; i < operands.size(); i++ ) {
                            if( i > 0 ) {
                                formatter.append( ' ' ).append( operator ).append( ' ' );
                            }
                            operands.get( i ).appendTo( formatter );
                        }
                        return;
                    case '~':
                        String str = operands.get( 0 ).stringValue( formatter );
                        StringBuilder builder = new StringBuilder();
                        char quote = 0;
                        for( int i = 0; i < str.length(); i++ ) {
                            char ch = str.charAt( i );
                            switch( ch ) {
                                case '"':
                                case '\'':
                                    if( quote == 0 ) {
                                        quote = ch;
                                    } else if( quote == ch ) {
                                        quote = 0;
                                    } else {
                                        builder.append( ch );
                                    }
                                    break;
                                case '\\':
                                    builder.append( str.charAt( ++i ) );
                                    break;
                                default:
                                    builder.append( ch );
                            }
                        }
                        new ValueExpression( this, builder.toString() ).appendTo( formatter );
                        return;
                    default:
                        builder = new StringBuilder( "Not supported Operation '" ).append( operator ).append( "' for '" );
                        for( int i = 0; i < operands.size(); i++ ) {
                            builder.append( operands.get( i ) );
                            if( i < operands.size() - 1 ) {
                                builder.append( "' and '" );
                            } else {
                                builder.append( "'" );
                            }
                        }
                        throw createException( builder.toString() );
                }
        }
        super.appendTo( formatter );
    }

    static double unitFactor( String leftUnit, String rightUnit ) {
        if( leftUnit.length() == 0 || rightUnit.length() == 0 || leftUnit.equals( rightUnit ) ) {
            return 1;
        }
        HashMap<String, Double> leftGroup = unitConversions.get( leftUnit );
        if( leftGroup != null ) {
            HashMap<String, Double> rightGroup = unitConversions.get( rightUnit );
            if( leftGroup == rightGroup ) {
                return leftGroup.get( leftUnit ) / leftGroup.get( rightUnit );
            }
        }
        return 1;
    }

    @Override
    public double doubleValue( CssFormatter formatter ) {
        Expression leftOp = operands.get( 0 );
        int type = leftOp.getDataType( formatter );
        double value = leftOp.doubleValue( formatter );
        String unit = leftOp.unit( formatter );
        for( int i = 1; i < operands.size(); i++ ) {
            Expression rightOp = operands.get( i );
            int rightType = rightOp.getDataType( formatter );
            double right = rightOp.doubleValue( formatter );
            switch( operator ) {
                case '+':
                case '-':
                    right /= unitFactor( unit, rightOp.unit( formatter ) );
            }
            if( type == COLOR ) {
                if( rightType == COLOR ) {
                    value = doubleValue2Colors( value, right );
                } else {
                    value = doubleValueLeftColor( value, right );
                }
            } else {
                if( rightType == COLOR ) {
                    value = doubleValueRightColor( value, right );
                } else {
                    value = doubleValue( value, right );
                }
            }
        }
        return value;
    }

    private double doubleValue( double left, double right ) {
        switch( operator ) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                return left / right;
            default:
                throw createException( "Not supported Oprator '" + operator + "' for Expression '" + toString() + '\'' );
        }
    }

    private double doubleValueLeftColor( double color, double right ) {
        return rgba( doubleValue( red( color ), right ), //
                        doubleValue( green( color ), right ), //
                        doubleValue( blue( color ), right ), 1 );
    }

    private double doubleValueRightColor( double left, double color ) {
        return rgba( doubleValue( left, red( color ) ), //
                        doubleValue( left, green( color ) ), //
                        doubleValue( left, blue( color ) ), 1 );
    }

    private double doubleValue2Colors( double left, double right ) {
        return rgba( doubleValue( red( left ), red( right ) ), //
                        doubleValue( green( left ), green( right ) ), //
                        doubleValue( blue( left ), blue( right ) ), 1 );
    }

    @Override
    public boolean booleanValue(CssFormatter formatter) {
        switch( operator ) {
            case '&':
            case '|':
                boolean value = operands.get( 0 ).booleanValue( formatter );
                for( int i = 1; i < operands.size(); i++ ) {
                    boolean right = operands.get( i ).booleanValue( formatter );
                    switch( operator ) {
                        case '&':
                            value &= right;
                            break;
                        case '|':
                            value |= right;
                            break;
                    }
                }
                return value;
            case '>':
            case '<':
            case '=':
            case '≥':
            case '≤':
                int type = maxOperadType( formatter );
                switch( type ) {
                    case STRING: {
                        String left = operands.get( 0 ).stringValue( formatter );
                        String right = operands.get( 1 ).stringValue( formatter );
                        switch( operator ) {
                            case '>':
                                return left.compareTo( right ) > 0;
                            case '<':
                                return left.compareTo( right ) < 0;
                            case '=':
                                return left.compareTo( right ) == 0;
                            case '≥':
                                return left.compareTo( right ) >= 0;
                            case '≤':
                                return left.compareTo( right ) <= 0;
                        }
                        break;
                    }
                    case COLOR:
                    case RGBA:{
                        long left = Double.doubleToRawLongBits( operands.get( 0 ).doubleValue( formatter ) );
                        long right = Double.doubleToRawLongBits( operands.get( 1 ).doubleValue( formatter ) );
                        switch( operator ) {
                            case '>':
                                return left > right;
                            case '<':
                                return left < right;
                            case '=':
                                return left == right;
                            case '≥':
                                return left >= right;
                            case '≤':
                                return left <= right;
                        }
                    }
                        //$FALL-THROUGH$
                    default: {
                        double left = operands.get( 0 ).doubleValue( formatter );
                        double right = operands.get( 1 ).doubleValue( formatter );
                        switch( operator ) {
                            case '>':
                                return left > right;
                            case '<':
                                return left < right;
                            case '=':
                                return left == right;
                            case '≥':
                                return left >= right;
                            case '≤':
                                return left <= right;
                        }
                    }
                }
                //$FALL-THROUGH$
            default:
        }
        throw createException( "Not supported Oprator '" + operator + "' for Expression '" + toString() + '\'' );
    }

    /**
     * Calculate the unit if there are different units. It use the numerator and denominator count.
     * @param formatter the CCS target
     * @param list previous result
     * @return null or a with with minimum one entry
     */
    private ArrayList<Unit> unit( CssFormatter formatter, ArrayList<Unit> list ){
        for( int i = 0; i < operands.size(); i++ ) {
            Expression exp = operands.get( i );
            if( exp.getClass() == Operation.class ) {
                Operation op = (Operation)exp;
                switch( op.operator ) {
                    case '*':
                    case '/':
                        list = op.unit( formatter, list );
                        break;
                    default:
                }
            } else {
                String unitStr = exp.unit( formatter );
                if( !unitStr.isEmpty() ) {
                    if( list == null ) {
                        list = new ArrayList<>();
                    }
                    Unit unit = null;
                    for( int j = 0; j < list.size(); j++ ) {
                        Unit unitLoop = list.get( j );
                        if( unitLoop.unit.equals( unitStr ) ) {
                            unit = unitLoop;
                            break;
                        }
                    }
                    if( unit == null ) {
                        unit = new Unit( unitStr );
                        list.add( unit );
                    }
                    if( i == 0 || operator == '*' ) {
                        unit.numerator++;
                    } else {
                        unit.denominator++;
                    }
                }
            }
        }
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unit( CssFormatter formatter ) {
        switch( operator ) {
            case '*':
            case '/':
                ArrayList<Unit> list = unit( formatter, null );
                if( list != null ) {
                    Unit unit = list.get( 0 );
                    int useCount = unit.useCount();
                    for( int i=1; i<list.size(); i++ ){
                        if( list.get( i ).useCount() > useCount ) {
                            unit = list.get( i );
                        }
                    }
                    return unit.unit;
                }
                return "";
        }
        for( int i = 0; i < operands.size(); i++ ) {
            String unit = operands.get( i ).unit( formatter );
            if( !unit.isEmpty() ) {
                return unit;
            }
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for( int i = 0; i < operands.size(); i++ ) {
            if( i > 0 ) {
                builder.append( ' ' ).append( operator ).append( ' ' );
            }
            builder.append( operands.get( i ) );
        }
        return builder.toString();
    }

    static int level( char operator ) {
        switch( operator ) {
            case ',':
                return 1;
            case ' ':
                return 2;
            case ':':
                return 3;
            case '=':
            case '>':
            case '<':
                return 4;
            case '+':
            case '-':
                return 5;
            case '*':
            case '/':
                return 6;
            case '~':
                return 7;
       }
        return 0;
    }

    private static class Unit {
        private int          numerator;

        private int          denominator;

        private final String unit;

        Unit( String unit ) {
            this.unit = unit;
        }

        int useCount() {
            return numerator - denominator;
        }
    }
}
