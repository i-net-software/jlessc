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
class Operation extends Expression {

    private final ArrayList<Expression> operands = new ArrayList<>();

    private final char                  operator;
    
    private int                         type;

    private static final HashMap<String, HashMap<String, Double>> UNIT_CONVERSIONS = new HashMap<>();
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

    /**
     * Helper for creating static unit conversions.
     * 
     * @param group the unit group like length, duration or angles. Units of one group can convert in another.
     * @param unit the unit name
     * @param factor the convert factor
     */
    private static void putConvertion( HashMap<String, Double> group, String unit, double factor ) {
        UNIT_CONVERSIONS.put(unit, group );
        group.put( unit, factor );
    }

    /**
     * Create a new instance.
     * 
     * @param obj another LessObject with parse position.
     * @param left left operand
     * @param operator the operator like +, -, *, etc.
     */
    Operation( LessObject obj, Expression left, char operator ) {
        this( obj, operator );
        if( left != null ) {
            this.operands.add( left );
        }
    }

    /**
     * Create a new instance with an empty parameter list.
     * 
     * @param obj another LessObject with parse position.
     * @param operator the operator like +, -, *, etc.
     */
    Operation( LessObject obj, char operator ) {
        super( obj, String.valueOf( operator ) );
        this.operator = operator;
    }

    /**
     * Create a new instance with an empty parameter list and a comma as operation.
     * 
     * @param obj another LessObject with parse position.
     */
    Operation( LessObject obj ) {
        super( obj, "," );
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
     * 
     * @param right the next operand
     */
    void addOperand( Expression right ) {
        this.operands.add( right );
    }

    /**
     * Add an operand on the top position to the operand list.
     * 
     * @param left operand the new operand
     */
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
                    type = LIST;
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

    /**
     * Get the highest data type of different operands
     * @param formatter current formatter
     * @return the data type
     */
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
            case LIST:
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
                        formatter.append( builder.toString() ); // new ValueExpression( this, builder.toString() ).appendTo( formatter );
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

    /**
     * Calculate the factor between 2 units.
     * 
     * @param leftUnit left unit
     * @param rightUnit right unit
     * @param fail true, should be fail if units incompatible; false, return 1 is incompatible
     * @return the factor between the 2 units.
     * @throws LessException if unit are incompatible and fail is true
     */
    static double unitFactor( String leftUnit, String rightUnit, boolean fail ) {
        if( leftUnit.length() == 0 || rightUnit.length() == 0 || leftUnit.equals( rightUnit ) ) {
            return 1;
        }
        HashMap<String, Double> leftGroup = UNIT_CONVERSIONS.get( leftUnit );
        if( leftGroup != null ) {
            HashMap<String, Double> rightGroup = UNIT_CONVERSIONS.get( rightUnit );
            if( leftGroup == rightGroup ) {
                return leftGroup.get( leftUnit ) / leftGroup.get( rightUnit );
            }
        }
        if( fail ) {
            throw new LessException( "Incompatible types" );
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
                    right /= unitFactor( unit, rightOp.unit( formatter ), false );
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

    /**
     * Calculate the number value of two operands if possible.
     * 
     * @param left the left
     * @param right the right
     * @return the result.
     */
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

    /**
     * Calculate a color on left with a number on the right side. The calculation occur for every color channel.
     * 
     * @param color the color
     * @param right the right
     * @return color value as long
     */
    private double doubleValueLeftColor( double color, double right ) {
        return rgba( doubleValue( red( color ), right ), //
                        doubleValue( green( color ), right ), //
                        doubleValue( blue( color ), right ), 1 );
    }

    /**
     * Calculate a number on left with a color on the right side. The calculation occur for every color channel.
     * 
     * @param left the left
     * @param color the color
     * @return color value as long
     */
    private double doubleValueRightColor( double left, double color ) {
        return rgba( doubleValue( left, red( color ) ), //
                        doubleValue( left, green( color ) ), //
                        doubleValue( left, blue( color ) ), 1 );
    }

    /**
     * Calculate two colors. The calculation occur for every color channel.
     * 
     * @param left the left color
     * @param right the right color
     * @return color value as long
     */
    private double doubleValue2Colors( double left, double right ) {
        return rgba( doubleValue( red( left ), red( right ) ), //
                        doubleValue( green( left ), green( right ) ), //
                        doubleValue( blue( left ), blue( right ) ), 1 );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean booleanValue(CssFormatter formatter) {
        Expression leftOp = operands.get( 0 );
        switch( operator ) {
            case '&':
            case '|':
                boolean value = leftOp.booleanValue( formatter );
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
            case '!':
                return !leftOp.booleanValue( formatter );
            case '>':
            case '<':
            case '=':
            case '≥':
            case '≤':
                int type = maxOperadType( formatter );
                switch( type ) {
                    case STRING: {
                        // need to differ between keyword without quotes and strings with quotes. The type of quote is ignored
                        String left = normlizeQuotes( leftOp.stringValue( formatter ) );
                        String right = normlizeQuotes( operands.get( 1 ).stringValue( formatter ) );
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
                        long left = Double.doubleToRawLongBits( leftOp.doubleValue( formatter ) );
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
                        double left = leftOp.doubleValue( formatter );
                        Expression rightOp = operands.get( 1 );
                        double right = rightOp.doubleValue( formatter );
                        try {
                            right /= unitFactor( leftOp.unit( formatter ), rightOp.unit( formatter ), true );
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
                        } catch (LessException ex ) {
                            return false;
                        }
                    }
                }
                //$FALL-THROUGH$
            default:
        }
        throw createException( "Not supported Oprator '" + operator + "' for Expression '" + toString() + '\'' );
    }

    /**
     * Convert single quotes to double quotes.
     * @param str input
     * @return normalize string
     */
    private String normlizeQuotes( String str ) {
        if( str.length() > 1 && str.charAt( 0 ) == '\'' && str.charAt( str.length() - 1 ) == '\'' ) {
            return '\"' + str.substring( 1, str.length() - 1 ) + '\"';
        }
        return str;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation listValue( CssFormatter formatter ) {
        switch( operator ) {
            case ' ':
            case ',':
                return this;
        }
        return super.listValue( formatter );
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * Helper class to count the resulting unit of an operation with different units
     */
    private static class Unit {
        private int          numerator;

        private int          denominator;

        private final String unit;

        /**
         * Create a new instance.
         * @param unit the unit to count.
         */
        Unit( String unit ) {
            this.unit = unit;
        }

        /**
         * The resulting usage count of the unit.
         * @return the count
         */
        int useCount() {
            return numerator - denominator;
        }
    }
}
