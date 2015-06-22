JLessC
======

JLessC is a [Less CSS compiler](http://lesscss.org) written completely in Java (pure Java). It does not need any additional libraries at runtime (no JavaScript, no ANTLR). The main target of the project is performance and compatibility to bootstrap.

Required Java Version
----
JlessC requires Java SE 7 or higher. It is tested with Java SE 7 and 8 on [travis-ci.org](https://travis-ci.org/i-net-software/jlessc).

[![Build Status](https://travis-ci.org/i-net-software/jlessc.svg)](https://travis-ci.org/i-net-software/jlessc)

Add Dependency
----
Gradle users should add the library using the following dependency:

    dependencies {
        compile 'de.inetsoftware:jlessc:+'
    }

Maven users should add the library using the following dependency:

    <dependency>
      <groupId>de.inetsoftware</groupId>
      <artifactId>jlessc</artifactId>
      <version>RELEASE</version>
    </dependency>

Usage
----
Checkout the sources or download the binary and add it to your Java project. The following code sequence shows a sample usage:

    import com.inet.lib.less.Less;
    ...
    
    // Compile Less data to CSS output
    String css = Less.compile( null, "@bgcol: red; #row { background-color: @bgcol; }", true );

Benchmark
----
JLessC runs a benchmark test on the travis build system using different less compilers. We always use the latest version of every compiler. We measure the compile time of the bootstrap sample in our test suite. The table shows [a result for Java SE 8](https://travis-ci.org/i-net-software/jlessc/jobs/57452290). Of course the values can change over the time.

| Tool                                                |      Time |
| :-------------------------------------------------- | ---------:|
| JLessC                                              |   ~400 ms |
| [less.js][lessJS] via Exec call to nodeJS           |   ~850 ms |
| [Official LESS CSS Compiler for Java][lessOfficial] |  ~4400 ms |
| [LESS Engine][lessEngine]                           | ~10800 ms |

License
----
MIT License

Alternative libraries
----
+ [LessCSS4j](https://github.com/localmatters/lesscss4j) used ANTLR
+ [LESS Engine][lessEngine] using Rhino
+ [Official LESS CSS Compiler for Java][lessOfficial] using Rhino

[lessJS]: https://www.npmjs.com/package/less
[lessEngine]: https://github.com/asual/lesscss-engine "LESS Engine"
[lessOfficial]: https://github.com/marceloverdijk/lesscss-java "Official LESS CSS Compiler for Java"
