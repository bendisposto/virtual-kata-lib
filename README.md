# Virtual Kata - Compiler and Testrunner Library

[![Build Status](https://travis-ci.org/bendisposto/virtual-kata-lib.svg?branch=master)](https://travis-ci.org/bendisposto/virtual-kata-lib)

The library is used to compile Strings containing Java source code and run   JUnit 4.12 tests.

## Maven
    <dependency>
       <groupId>de.hhu.stups</groupId>
       <artifactId>virtual-kata-lib</artifactId>
       <version>1.0.1</version>
    </dependency>

## Building

- Library: ```gradle build```
- JavaDoc: ```gradle javadoc```

## Changes

- Version 1.0.1 (25/06/16)
Improved error message when used with a plain JRE instead of a JDK.
