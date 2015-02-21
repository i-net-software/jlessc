package com.inet.lib.less;

/**
 * A version of the CssFormatter that produce an uncompressed output.
 */
class DefaultFormatter extends CssFormatter {

    /**
     * Create a standard instance.
     */
    DefaultFormatter() {
        super( false );
    }

    /**
     * Create an instance.
     * 
     * @param toString
     *            true, format is called without a parser
     */
    DefaultFormatter( boolean toString ) {
        super( toString );
    }
}
