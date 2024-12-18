<div align="right"><a target="myNextJob" href="https://www.prisma-capacity.eu/careers#job-offers">
    <img class="inline" src="prisma.png">
</a></div>

# Spring-CQS

Simple abstractions we use to follow the CQS Principle in applications.

![Java CI](https://github.com/prisma-capacity/spring-cqs/workflows/Java%20CI/badge.svg?branch=main)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/333bfd567a6a447895212994b414f077)](https://app.codacy.com/gh/prisma-capacity/spring-cqs?utm_source=github.com&utm_medium=referral&utm_content=prisma-capacity/spring-cqs&utm_campaign=Badge_Grade_Settings)
[![codecov](https://codecov.io/gh/prisma-capacity/spring-cqs/branch/main/graph/badge.svg)](https://codecov.io/gh/prisma-capacity/spring-cqs)
[![MavenCentral](https://img.shields.io/maven-central/v/eu.prismacapacity/spring-cqs)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22eu.prismacapacity%22)
<a href="https://www.apache.org/licenses/LICENSE-2.0">
<img class="inline" src="https://img.shields.io/badge/license-ASL2-green.svg?style=flat">
</a>

### Motivation

The [CQS Principle](https://en.wikipedia.org/wiki/Command–query_separation) states that "every method should either be a
command that performs an action, or a query that returns data to the caller, but not both." in order to reduce
side-effects.

In our projects we use abstractions like Query & QueryHandler as well as Command & CommandHandler to follow this
principle. However, there is a bit of fineprint here that makes it worthwhile to reuse this in form of a library:

* a command / a query needs to be valid (as in java.validation valid), otherwise a Command/Query-ValidationExcption will
  be thrown
* a command / a query needs to be valid (determined by an optional message on the handler), otherwise a
  Command/Query-ValidationExcption will be thrown
* a command / a query needs to be verified by a mandatory method in the handler the is expected to throw a
  Command/Query-VerificationException
* when a command / a query is handled, any exception it may throw is to be wrapped in a Command/Query-Handling Exception

In order to accomplish that, this kind of orchestration is done by an aspect, in order to get this out of that way when
following the call stack in your IDE.

This has pros and cons and might be a debatable use of aspects, but we decided that this is the best solution for our
context. You know, it depends...

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

#### Spring Boot Compatibility

| Library version | Spring Boot version |
|-----------------|---------------------|
| 2.x.x           | 2.7+                |
| 3.x.x           | 3.1+                |   


#### Configuration

The only thing you might want to configure is how Cqs uses Metrics. See @CqsConfiguration for details.

#### Example

Let's say, you have a Foo Entity and a corresponding repository. What we do with this lib is to encapsulate use-cases in
a UI-agnosic manner.

```java
class FooEntity {
}

class FooQuery implements Query {
    @NotNull
    UUID idToLookFor;

    @NotNull
    Long userIdOfRequestingUser;
}

class FooHandler implements QueryHandler<FooQuery, List<FooEntity>> {

    @Override
    public void verify(@NonNull FooQuery query) throws QueryVerificationException {
        // check if the preconditions for the query to be executed are met.
        // we know userIdOfRequestingUser is not null (otherwise it would not have passed validation)
        // but maybe we need to check if the user is assigned to the right organisation or something...
    }

    @Override
    public List<FooEntity> handle(@NonNull FooQuery query) throws QueryHandlingException, QueryTimeoutException {
        return myFooRepository.findById(query.idToLookFor);
    }
}
```

The idea here is (beyond javax.validation), you can quickly see the ins and outs of a use-case, may it be Query or
Command, including checking for instance security constraints in a programmatic and technology agnostic way. Also this
creates a nice seam between UI/Rest Layer and Domain Model or persistence model in case this is the same for you. If
you're interested in checking and maintaing those bounds, have a look at for
instance [Archunit](https://www.archunit.org/).

#### Configure a retry behaviour for Command and Query handlers

If you want you can configure a retry behaviour for your handlers by adding `@RetryConfiguration` to the handler class.
By default, it will retry 3 times in intervals of 20ms for every exception that is not of
type `QueryValidationException` or `CommandValidationException`. You can also configure an exponential backoff if
desired.
Please have a look
at [RetryConfiguration.java](src/main/java/eu/prismacapacity/spring/cqs/retry/RetryConfiguration.java) for all available
options.

#### Mandatory Logging of Command Execution (since version 3.1)

Commands have the potential to alter the state of the system (in contrast to queries, which should not). This is why 
it makes sense to log attempted command executions (regardless of their outcome, may it be success or any kind of 
failure). The aspect will take care of that automatically.

When logging command executions, an extra attribute is added to the Log-Event with the name of 'cqs.command' and the value 
is the so-called LogString of the command. By default this is generated reflectively as a best-effort similar to a 
toString implementation. As it makes sense to exclude certain fields from logging (for GDPR reasons for example)
you can annotate fields of your Commands using '@LogExclude' in order to skip those when rendering a command for logging.

Also, if your using returning Command handlers, the result will be added as 'cqs.result' with the same rules as 
for the command.

Failures will go to WARN loglevel while success go to INFO.

#### Tips for use

* Create one handler per use-case.
* Do not call other handlers inside of yours. If that leads to code duplication, consider refactoring common code into a service that will be used by the two handlers.


## Migration

#### 1.0 -> 1.1:

In 1.0 `CommandHandler` returned a `CommandTokenResponse`. While this is useful in some cases, the majority of uses
could go with a `void` return. For this reason, we have a breaking change in 1.1, where `CommandHandler` was renamed
to `TokenCommandHandler`, and the new `CommandHandler` now return void.

So please make your CommandHandlers extend `TokenCommandHandler` as a minimal change. If you don't use the token, you
may want to just return void instead.
