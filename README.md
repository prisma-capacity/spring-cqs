<div align="right"><a target="myNextJob" href="https://www.prisma-capacity.eu/careers#job-offers">
    <img class="inline" src="prisma.png">
</a></div>

# Spring-CQS

Simple abstractions we use to follow the CQS Principle in applications.

![Java CI](https://github.com/prisma-capacity/spring-cqs/workflows/Java%20CI/badge.svg?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/333bfd567a6a447895212994b414f077)](https://app.codacy.com/gh/prisma-capacity/spring-cqs?utm_source=github.com&utm_medium=referral&utm_content=prisma-capacity/spring-cqs&utm_campaign=Badge_Grade_Settings)
[![codecov](https://codecov.io/gh/prisma-capacity/spring-cqs/branch/master/graph/badge.svg)](https://codecov.io/gh/prisma-capacity/spring-cqs)
[![MavenCentral](https://img.shields.io/maven-central/v/eu.prismacapacity/spring-cqs)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22eu.prismacapacity%22)
<a href="https://www.apache.org/licenses/LICENSE-2.0">
    <img class="inline" src="https://img.shields.io/badge/license-ASL2-green.svg?style=flat">
</a>

### Motivation

The [CQS Principle](https://en.wikipedia.org/wiki/Command–query_separation) states that "every method should either be a command that performs an action, or a query that returns data to the caller, but not both." in order to reduce side-effects.

In our projects we use abtractions like Query & QueryHandler as well as Command & CommandHandler to follow this principle. However, there is a bit of fineprint here that makes it worthwile to reuse this in form of a library:

* a command / a query needs to be valid (as in java.validation valid), otherwise a Command/Query-ValidationExcption will be thrown
* a command / a query needs to be valid (determined by an optional message on the handler), otherwise a Command/Query-ValidationExcption will be thrown
* a command / a query needs to be verified by a mandatory method in the handler the is expected to throw a Command/Query-VerificationException 
* when a command / a query is handled, any exception it may throw is to be wrapped in a Command/Query-Handling Exception

In order to accomplish that, this kind of orchestration is done by an aspect, in order to get this out of that way when following the call stack in your IDE.

This has pros and cons and migth be a questionale use of aspects, but we decided that this is the best solution for our context. You know, it depends...

### Usage

This is meant to be used with Spring Boot. In order to get this running just add the dependency to your build system:

#### Maven

````
    <dependency>
      <groupId>eu.prismacapacity</groupId>
      <artifactId>spring-cqs</artifactId>
      <version><!-- put the desired version in here--></version>
    </dependency>
````

#### Configuration

The only thing you might want to configure is how Cqs uses Metrics. See @CqsConfiguration for details.

