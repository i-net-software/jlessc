JLessC
======

JLessC is a Less CSS compiler (http://lesscss.org) written completely in Java (pure Java). It does not need any additional libraries at runtime (no JavaScript, no ANTLR). The main target of the project is performance and compatibility to bootstrap.

Usage
----
Checkout the sources or download the binary and add it to your Java project. The follow code sequence show a sample usage.

    import com.inet.lib.less;
    ...
    
    // Compile Less data to CSS output
    String css = Less.compile( null, "@bgcol: red; #row { background-color: @bgcol; }", true ) );

License
----
MIT License

Alternative libraries
----
+ <a href="https://github.com/localmatters/lesscss4j">LessCSS4j</a> uses on ANTLR
+ <a href="https://github.com/asual/lesscss-engine">LESS Engine</a> uses Rhino
+ <a href="https://github.com/marceloverdijk/lesscss-java">Official LESS CSS Compiler for Java</a> uses Rhino

