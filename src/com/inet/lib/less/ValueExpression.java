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

import java.text.ParsePosition;
import java.util.ArrayList;

/**
 * A constant value.
 */
class ValueExpression extends Expression {

    private int    type;

    private double value;

    private String unit;

    private Operation op;

    /**
     * Create a new instance.
     * @param obj another LessObject with parse position.
     * @param value the value
     */
    ValueExpression( LessObject obj, String value ) {
        super( obj, value );
    }

    /**
     * Create a new value expression from a JavaScriptExpression.
     * @param expr another LessObject with parse position.
     * @param value the value
     */
    ValueExpression( JavaScriptExpression expr, Object value ) {
        super( expr, String.valueOf( value ) );
        if( value instanceof Number ) {
            this.type = NUMBER;
            this.value = ((Number)value).doubleValue();
            this.unit = "";
        }
    }

    /**
     * Create a value expression as parameter for a mixin which not change it value in a different context.
     * @param formatter current formatter
     * @param expr current expression
     * @return a ValueExpression
     */
    public static ValueExpression eval( CssFormatter formatter, Expression expr ) {
        expr = expr.unpack( formatter ); // unpack to increase the chance to find a ValueExpression
        if( expr.getClass() == ValueExpression.class ) {
            return (ValueExpression)expr;
        }
        ValueExpression valueEx = new ValueExpression( expr, expr.stringValue( formatter ) );
        valueEx.type = expr.getDataType( formatter );
        valueEx.unit = expr.unit( formatter );
        switch( valueEx.type ) {
            case STRING:
            case BOOLEAN:
                break; //string is already set
            case LIST:
                Operation op = valueEx.op = new Operation( expr, ' ' );
                ArrayList<Expression> operants = expr.listValue( formatter ).getOperands();
                for( int j = 0; j < operants.size(); j++ ) {
                    op.addOperand( ValueExpression.eval( formatter, operants.get( j ) ) );
                }
                break;
            default:
                valueEx.value = expr.doubleValue( formatter );
        }
        return valueEx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendTo( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        switch( type ) {
            case COLOR:
                formatter.appendColor( value, toString() );
                break;
            case STRING:
                SelectorUtils.appendToWithPlaceHolder( formatter, toString(), 0, this );
                break;
            default:
                super.appendTo( formatter );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDataType( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        if( type == STRING ) {
            throw createException( "Invalid Number: " + toString() );
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String unit( CssFormatter formatter ) {
        if( type == UNKNOWN ) {
            eval( formatter );
        }
        return unit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean booleanValue( CssFormatter formatter ) {
        return Boolean.parseBoolean( toString() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation listValue( CssFormatter formatter ) {
        if( type == LIST ) {
            return op;
        }
        return super.listValue( formatter );
    }

    /**
     * Evaluate the type and value.
     * @param formatter current formatter
     */
    private void eval( CssFormatter formatter ) {
        try {
            String str = toString();
            if( str.endsWith( "%" ) ) {
                value = Double.parseDouble( str.substring( 0, str.length() - 1 ) );
                unit = "%";
                type = PERCENT;
                return;
            }
            unit = "";
            long rgb;
            if( str.startsWith( "#" ) ) {
                str = str.substring( 1 );
                switch( str.length() ) {
                    case 3:
                        rgb = 0;
                        for( int i = 0; i < 3; i++ ) {
                            char ch = str.charAt( i );
                            int digit = Character.digit( ch, 16 );
                            if( digit < 0 ) {
                                type = STRING;
                                return;
                            }
                            rgb *= 0x100;
                            rgb += digit * 17;
                            rgb *= 0x100;
                        }
                        break;
                    case 6:
                        rgb = 0;
                        for( int i = 0; i < 6; i++ ) {
                            char ch = str.charAt( i++ );
                            int digit = Character.digit( ch, 16 );
                            if( digit < 0 ) {
                                type = STRING;
                                return;
                            }
                            rgb *= 16;
                            rgb += digit;
                            ch = str.charAt( i );
                            digit = Character.digit( ch, 16 );
                            if( digit < 0 ) {
                                type = STRING;
                                return;
                            }
                            rgb *= 16;
                            rgb += digit;
                            rgb *= 0x100;
                        }
                        break;
                    default:
                        throw createException( "Invalid Color: " + toString() );
                }
            } else {
                str = str.toLowerCase();
                rgb = getRgbFromColorConst( str );
                if( rgb == -1 ) {
                    if( str.equals( "transparent" ) ) {
                        value = 0;
                        type = RGBA;
                        return;
                    } else {
                        ParsePosition pos = new ParsePosition( 0 );
                        Number number = formatter.getFormat().parse( str, pos );
                        if( number == null ) {
                            if( str.startsWith( "+" ) ) { // DecimalFormat can not parse an option plus sign
                                pos.setIndex( 1 );
                                number = formatter.getFormat().parse( str, pos );
                                if( number == null ) {
                                    type = STRING;
                                    return;
                                }
                            } else {
                                type = STRING;
                                return;
                            }
                        }
                        value = number.doubleValue();
                        if( pos.getIndex() != str.length() ) {
                            unit = str.substring( pos.getIndex() );
                        }
                        type = NUMBER;
                        return;
                    }
                }
            }
            value = Double.longBitsToDouble( ALPHA_1 | rgb );
            type = COLOR;
        } catch( NumberFormatException e ) {
            type = STRING;
        }
    }

    /**
     * Convert a well known color constant in a rgb value.
     * @param str value string
     * @return rgb value or -1 if not a known color
     */
    private static long getRgbFromColorConst( String str ) {
        long rgb;
        switch( str ) {
            case "aliceblue":
                rgb = 0xf000_f800_ff00L;
                break;
            case "antiquewhite":
                rgb = 0xfa00_eb00_d700L;
                break;
            case "aqua":
                rgb = 0xff00_ff00L;
                break;
            case "aquamarine":
                rgb = 0x7f00_ff00_d400L;
                break;
            case "azure":
                rgb = 0xf000_ff00_ff00L;
                break;
            case "beige":
                rgb = 0xf500_f500_dc00L;
                break;
            case "bisque":
                rgb = 0xff00_e400_c400L;
                break;
            case "black":
                rgb = 0x00;
                break;
            case "blanchedalmond":
                rgb = 0xff00_eb00_cd00L;
                break;
            case "blue":
                rgb = 0xFF00L;
                break;
            case "blueviolet":
                rgb = 0x8a00_2b00_e200L;
                break;
            case "brown":
                rgb = 0xa500_2a00_2a00L;
                break;
            case "burlywood":
                rgb = 0xde00_b800_8700L;
                break;
            case "cadetblue":
                rgb = 0x5f00_9e00_a000L;
                break;
            case "chartreuse":
                rgb = 0x7f00_ff00_0000L;
                break;
            case "chocolate":
                rgb = 0xd200_6900_1e00L;
                break;
            case "coral":
                rgb = 0xff00_7f00_5000L;
                break;
            case "cornflowerblue":
                rgb = 0x6400_9500_ed00L;
                break;
            case "cornsilk":
                rgb = 0xff00_f800_dc00L;
                break;
            case "crimson":
                rgb = 0xdc00_1400_3c00L;
                break;
            case "cyan":
                rgb = 0x0000_ff00_ff00L;
                break;
            case "darkblue":
                rgb = 0x0000_0000_8b00L;
                break;
            case "darkcyan":
                rgb = 0x0000_8b00_8b00L;
                break;
            case "darkgoldenrod":
                rgb = 0xb800_8600_0b00L;
                break;
            case "darkgray":
            case "darkgrey": //British
                rgb = 0xa900_a900_a900L;
                break;
            case "darkgreen":
                rgb = 0x0000_6400_0000L;
                break;
            case "darkkhaki":
                rgb = 0xbd00_b700_6b00L;
                break;
            case "darkmagenta":
                rgb = 0x8b00_0000_8b00L;
                break;
            case "darkolivegreen":
                rgb = 0x5500_6b00_2f00L;
                break;
            case "darkorange":
                rgb = 0xff00_8c00_0000L;
                break;
            case "darkorchid":
                rgb = 0x9900_3200_cc00L;
                break;
            case "darkred":
                rgb = 0x8b00_0000_0000L;
                break;
            case "darksalmon":
                rgb = 0xe900_9600_7a00L;
                break;
            case "darkseagreen":
                rgb = 0x8f00_bc00_8f00L;
                break;
            case "darkslateblue":
                rgb = 0x4800_3d00_8b00L;
                break;
            case "darkslategray":
            case "darkslategrey": //British
                rgb = 0x2f00_4f00_4f00L;
                break;
            case "darkturquoise":
                rgb = 0x0000_ce00_d100L;
                break;
            case "darkviolet":
                rgb = 0x9400_0000_d300L;
                break;
            case "deeppink":
                rgb = 0xff00_1400_9300L;
                break;
            case "deepskyblue":
                rgb = 0x0000_bf00_ff00L;
                break;
            case "dimgray":
            case "dimgrey": //British
                rgb = 0x6900_6900_6900L;
                break;
            case "dodgerblue":
                rgb = 0x1e00_9000_ff00L;
                break;
            case "firebrick":
                rgb = 0xb200_2200_2200L;
                break;
            case "floralwhite":
                rgb = 0xff00_fa00_f000L;
                break;
            case "forestgreen":
                rgb = 0x2200_8b00_2200L;
                break;
            case "fuchsia":
                rgb = 0xff00_0000_ff00L;
                break;
            case "gainsboro":
                rgb = 0xdc00_dc00_dc00L;
                break;
            case "ghostwhite":
                rgb = 0xf800_f800_ff00L;
                break;
            case "gold":
                rgb = 0xff00_d700_0000L;
                break;
            case "goldenrod":
                rgb = 0xda00_a500_2000L;
                break;
            case "gray":
            case "grey": //British
                rgb = 0x8000_8000_8000L;
                break;
            case "green":
                rgb = 0x0000_8000_0000L;
                break;
            case "greenyellow":
                rgb = 0xad00_ff00_2f00L;
                break;
            case "honeydew":
                rgb = 0xf000_ff00_f000L;
                break;
            case "hotpink":
                rgb = 0xf000_ff00_f000L;
                break;
            case "indianred":
                rgb = 0xcd00_5c00_5c00L;
                break;
            case "indigo":
                rgb = 0x4b00_0000_8200L;
                break;
            case "ivory":
                rgb = 0xff00_ff00_f000L;
                break;
            case "khaki":
                rgb = 0xf000_e600_8c00L;
                break;
            case "lavender":
                rgb = 0xe600_e600_fa00L;
                break;
            case "lavenderblush":
                rgb = 0xff00_f000_f500L;
                break;
            case "lawngreen":
                rgb = 0x7c00_fc00_0000L;
                break;
            case "lemonchiffon":
                rgb = 0xff00_fa00_cd00L;
                break;
            case "lightblue":
                rgb = 0xad00_d800_e600L;
                break;
            case "lightcoral":
                rgb = 0xf000_8000_8000L;
                break;
            case "lightcyan":
                rgb = 0xe000_ff00_ff00L;
                break;
            case "lightgoldenrodyellow":
                rgb = 0xfa00_fa00_d200L;
                break;
            case "lightgray":
            case "lightgrey": //British
                rgb = 0xd300_d300_d300L;
                break;
            case "lightgreen":
                rgb = 0x9000_ee00_9000L;
                break;
            case "lightpink":
                rgb = 0xff00_b600_c100L;
                break;
            case "lightsalmon":
                rgb = 0xff00_a000_7a00L;
                break;
            case "lightseagreen":
                rgb = 0x2000_b200_aa00L;
                break;
            case "lightskyblue":
                rgb = 0x8700_ce00_fa00L;
                break;
            case "lightslategray":
            case "lightslategrey": //British
                rgb = 0x7700_8800_9900L;
                break;
            case "lightsteelblue":
                rgb = 0xb000_c400_de00L;
                break;
            case "lightyellow":
                rgb = 0xff00_ff00_e000L;
                break;
            case "lime":
                rgb = 0x0000_ff00_0000L;
                break;
            case "limegreen":
                rgb = 0x3200_cd00_3200L;
                break;
            case "linen":
                rgb = 0xfa00_f000_e600L;
                break;
            case "magenta":
                rgb = 0xff00_0000_ff00L;
                break;
            case "maroon":
                rgb = 0x8000_0000_0000L;
                break;
            case "mediumaquamarine":
                rgb = 0x6600_cd00_aa00L;
                break;
            case "mediumblue":
                rgb = 0x0000_0000_cd00L;
                break;
            case "mediumorchid":
                rgb = 0xba00_5500_d300L;
                break;
            case "mediumpurple":
                rgb = 0x9300_7000_d800L;
                break;
            case "mediumseagreen":
                rgb = 0x3c00_b300_7100L;
                break;
            case "mediumslateblue":
                rgb = 0x7b00_6800_ee00L;
                break;
            case "mediumspringgreen":
                rgb = 0x0000_fa00_9a00L;
                break;
            case "mediumturquoise":
                rgb = 0x4800_d100_cc00L;
                break;
            case "mediumvioletred":
                rgb = 0xc700_1500_8500L;
                break;
            case "midnightblue":
                rgb = 0x1900_1900_7000L;
                break;
            case "mintcream":
                rgb = 0xf500_ff00_fa00L;
                break;
            case "mistyrose":
                rgb = 0xff00_e400_e100L;
                break;
            case "moccasin":
                rgb = 0xff00_e400_b500L;
                break;
            case "navajowhite":
                rgb = 0xff00_de00_ad00L;
                break;
            case "navy":
                rgb = 0x8000L;
                break;
            case "oldlace":
                rgb = 0xfd00_f500_e600L;
                break;
            case "olive":
                rgb = 0x8000_8000_0000L;
                break;
            case "olivedrab":
                rgb = 0x6b00_8e00_2300L;
                break;
            case "orange":
                rgb = 0xff00_a500_0000L;
                break;
            case "orangered":
                rgb = 0xff00_4500_0000L;
                break;
            case "orchid":
                rgb = 0xda00_7000_d600L;
                break;
            case "palegoldenrod":
                rgb = 0xee00_e800_aa00L;
                break;
            case "palegreen":
                rgb = 0x9800_fb00_9800L;
                break;
            case "paleturquoise":
                rgb = 0xaf00_ee00_ee00L;
                break;
            case "palevioletred":
                rgb = 0xd800_7000_9300L;
                break;
            case "papayawhip":
                rgb = 0xff00_ef00_d500L;
                break;
            case "peachpuff":
                rgb = 0xff00_da00_b900L;
                break;
            case "peru":
                rgb = 0xcd00_8500_3f00L;
                break;
            case "pink":
                rgb = 0xff00_c000_cb00L;
                break;
            case "plum":
                rgb = 0xdd00_a000_dd00L;
                break;
            case "powderblue":
                rgb = 0xb000_e000_e600L;
                break;
            case "purple":
                rgb = 0x8000_0000_8000L;
                break;
            case "red":
                rgb = 0xFF00_0000_0000L;
                break;
            case "rosybrown":
                rgb = 0xbc00_8f00_8f00L;
                break;
            case "royalblue":
                rgb = 0x4100_6900_e100L;
                break;
            case "saddlebrown":
                rgb = 0x8b00_4500_1300L;
                break;
            case "salmon":
                rgb = 0xfa00_8000_7200L;
                break;
            case "sandybrown":
                rgb = 0xf400_a400_6000L;
                break;
            case "seagreen":
                rgb = 0x2e00_8b00_5700L;
                break;
            case "seashell":
                rgb = 0xff00_f500_ee00L;
                break;
            case "sienna":
                rgb = 0xa000_5200_2d00L;
                break;
            case "silver":
                rgb = 0xc000_c000_c000L;
                break;
            case "skyblue":
                rgb = 0x8700_ce00_eb00L;
                break;
            case "slateblue":
                rgb = 0x6a00_5a00_cd00L;
                break;
            case "slategray":
            case "slategrey": //British
                rgb = 0x7000_8000_9000L;
                break;
            case "snow":
                rgb = 0xff00_fa00_fa00L;
                break;
            case "springgreen":
                rgb = 0x0000_ff00_7f00L;
                break;
            case "steelblue":
                rgb = 0x4600_8200_b400L;
                break;
            case "tan":
                rgb = 0xd200_b400_8c00L;
                break;
            case "teal":
                rgb = 0x0000_8000_8000L;
                break;
            case "thistle":
                rgb = 0xd800_bf00_d800L;
                break;
            case "tomato":
                rgb = 0xff00_6300_4700L;
                break;
            case "turquoise":
                rgb = 0x4000_e000_d000L;
                break;
            case "violet":
                rgb = 0xee00_8200_ee00L;
                break;
            case "wheat":
                rgb = 0xf500_de00_b300L;
                break;
            case "white":
                rgb = 0xFF00_FF00_FF00L;
                break;
            case "whitesmoke":
                rgb = 0xf500_f500_f500L;
                break;
            case "yellow":
                rgb = 0xff00_ff00_0000L;
                break;
            case "yellowgreen":
                rgb = 0x9a00_cd00_3200L;
                break;
            default:
                rgb = -1;
        }
        return rgb;
    }
}
