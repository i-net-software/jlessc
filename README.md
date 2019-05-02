JLessC
======

[![Build Status](https://travis-ci.org/i-net-software/jlessc.svg)](https://travis-ci.org/i-net-software/jlessc)
[![License](https://img.shields.io/github/license/i-net-software/jlessc.svg)](https://github.com/i-net-software/jlessc/blob/master/license.txt)
[![Coverage Status](https://coveralls.io/repos/i-net-software/jlessc/badge.svg?branch=master&service=github)](https://coveralls.io/github/i-net-software/jlessc?branch=master)
[![Download](https://api.bintray.com/packages/bintray/jcenter/de.inetsoftware%3Ajlessc/images/download.svg)](https://bintray.com/bintray/jcenter/de.inetsoftware%3Ajlessc/_latestVersion)
[![Maven](https://img.shields.io/maven-central/v/de.inetsoftware/jlessc.svg)](https://mvnrepository.com/artifact/de.inetsoftware/jlessc)

JLessC is a [Less CSS compiler](http://lesscss.org) written completely in Java (pure Java). It does not need any additional libraries at runtime (no JavaScript, no ANTLR). The main target of the project is performance and compatibility to Bootstrap.

Required Java Version
----
JLessC requires Java SE 8 or higher. It is tested with Java 8, 9, 10 and 11 on [travis-ci.org](https://travis-ci.org/i-net-software/jlessc). If you need support for Java 7 then you can use the old version 1.5.


Add Dependency
----
Gradle users should add the library using the following dependency:

    dependencies {
        compile 'de.inetsoftware:jlessc:+'
    }

If you want to test a snapshot version with the latest bug fixes, then add the snapshot repository:

    repositories {
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
        jcenter() // or any other repository that you use
    }

Maven users should add the library using the following dependency:

    <dependency>
      <groupId>de.inetsoftware</groupId>
      <artifactId>jlessc</artifactId>
      <version>RELEASE</version>
    </dependency>

Usage
----
Check out the sources or download the binary and add it to your Java project. The following code sequence shows a sample usage:

    import com.inet.lib.less.Less;
    ...
    
    // Compile Less data to CSS output
    String css = Less.compile( null, "@bgcol: red; #row { background-color: @bgcol; }", true );

API details can be found in the source of the [Less](https://github.com/i-net-software/jlessc/blob/master/src/com/inet/lib/less/Less.java) class. 

Benchmark
----
JLessC runs a benchmark test on the Travis build system using different less compilers. We always use the latest version of every compiler. We measure the compile time of the Bootstrap sample in our test suite. The table shows [a result for Java SE 8](https://travis-ci.org/i-net-software/jlessc/jobs/57452290). Of course the values can change over the time.

| Tool                                                |      Time |
| :-------------------------------------------------- | ---------:|
| JLessC                                              |   ~400 ms |
| [less.js][lessJS] via Exec call to nodeJS           |   ~850 ms |
| [Official LESS CSS Compiler for Java][lessOfficial] |  ~4400 ms |
| [LESS Engine][lessEngine]                           | ~10800 ms |


Alternative libraries
----
+ [LessCSS4j](https://github.com/localmatters/lesscss4j) used ANTLR
+ [LESS Engine][lessEngine] using Rhino
+ [Official LESS CSS Compiler for Java][lessOfficial] using Rhino


Reference usage
----
We use JLessC in our reporting software [i-net Clear Reports](https://www.inetsoftware.de/products/clear-reports) and PDF file comparer [i-net PDFC](https://www.inetsoftware.de/products/pdf-content-comparer) for user customized themes. 


[lessJS]: https://www.npmjs.com/package/less
[lessEngine]: https://github.com/asual/lesscss-engine "LESS Engine"
[lessOfficial]: https://github.com/marceloverdijk/lesscss-java "Official LESS CSS Compiler for Java"
