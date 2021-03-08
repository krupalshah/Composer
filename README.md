# Composer
Asynchrony made simple

[![Build Status](https://travis-ci.com/krupalshah/Composer.svg?branch=master)](https://travis-ci.com/krupalshah/Composer)
[![Download](https://api.bintray.com/packages/krupalshah55/Composer/Composer/images/download.svg) ](https://bintray.com/krupalshah55/Composer/Composer/_latestVersion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7dde7d053cac44e88d762519ab50b2b1)](https://app.codacy.com/gh/krupalshah/Composer?utm_source=github.com&utm_medium=referral&utm_content=krupalshah/Composer&utm_campaign=Badge_Grade_Settings)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<img src="/raw/legocomposer.jpg?raw=true" width="250" height="250"/>

Composer helps you to organize and execute multiple interdependent asynchronous input/output 
tasks such as webservice calls, database read/writes 
and file i/o together with concurrency support using `java.util.concurrent` APIs.<br/>
It is compatible with Java 8 & above on all JVM based platforms including Android.

Here is an example of how you can use Composer to create a chain of tasks. Consider a scenario where you want to get an associated Twitter account details for your app user, fetch different kinds of twitter data for that user, show them on app UI and then track the event in your analytics database. All of these tasks are asynchronous (except refreshing the UI) and dependent on each other.

```java
Composer.startWith(currentUser.getUserId(), err -> logger.error("Error executing tasks", err))
        .thenPlay(userId -> { return accountService.getTwitterAccountDetails(userId); })
        .thenContinueIf(response -> response.status.isOk())
        .thenPlayTogether(
            Results::new,
            response -> twitterService.getTweets(response.username), 
            response -> twitterService.getMedia(response.username), 
            response -> twitterService.getFollowers(response.username) 
        )
        .thenWaitFor(results -> { refreshUI(results); })
        .thenPlay(() -> { analyticsDb.trackEvent("get_twitter_details"); })
        .thenFinish();
``` 

Please note that Composer does not aim to provide an extensible API for managing asynchronous tasks. Instead, it aims to provide a minimal, easy to use API which can be useful for the scenarios where interdependency between such tasks forces you to write boilerplate code for managing state, validating conditions or handling errors. Most client-side mobile/web applications and backend services communicating with each other require an extensible framework in which interdependent asynchronous tasks can be glued together. Composer serves only specific use cases and it may not be a good fit all the use cases, especially when having an extensibe asynchronous framework is critical to the application.

For detailed usage information, please refer [Getting Started](#getting-started) section.
    
### Table of Contents
- [Setup](#setup)
- [Getting Started](#getting-started)
    - [Overview](#overview)
    - [Chaining tasks](#chaining-tasks)
    - [Executing tasks concurrently](#executing-tasks-concurrently)
    - [Validating task output](#validating-task-output)
    - [Executing task synchronously](#executing-task-synchronously)
    - [Providing custom executor service](#providing-custom-executor-service)
- [Changelog](#changelog)

### Setup
- Gradle:
```groovy
repositories {
    jcenter()
}

dependencies {
    implementation 'com.krupalshah:composer:2.0.0'
}
```

- Maven:
```xml
<dependency>
  <groupId>com.krupalshah</groupId>
  <artifactId>composer</artifactId>
  <version>2.0.0</version>
  <type>pom</type>
</dependency>
```

- Ivy:
```xml
<dependency org='com.krupalshah' name='composer' rev='2.0.0'>
  <artifact name='composer' ext='pom' />
</dependency>
```
Development snapshots are available in [JFrog artifactory](https://oss.jfrog.org/artifactory/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/com/krupalshah/composer). 

### Getting Started

#### Overview
The API consists of an interface `Composable` and its implementation `Composer`. The implementation serves as an entrypoint and returns `Composable` at each step of execution until chaining is discontinued.

Use `startWith()` to create your first `Composable` like below:

```java
Composer.startWith(someInputOrTask, err -> logger.error("Error executing tasks", err))
```
The first param is only required if you want to pass some pre-known value as an input, or a task that may produce the same.<br/>
The second param `ErrorStream` receives all errors during execution.<br/>

If you don't have any pre-known input or task, you can simply create your first `Composable` by just providing an `ErrorStream` like below:

```java
Composer.startWith(err -> logger.error("Error executing tasks", err))
```

Use `thenFinish()` to discontinue further chaining and complete the awaiting task execution. Between `startWith` and `thenFinish`, chain your tasks according to their dependencies.<br/>

#### Chaining tasks
A `Task` can be of type
- `SimpleTask` if it takes no input and returns no output.
- `ConsumingTask<Input>` if it takes an input but returns no output. 
- `ProducingTask<Output>` if it takes no input but returns an output. 
- `TransformingTask<Input,Output>` if it takes an input and converts it into output.

For example, consider a very straightforward scenario in which some independent data is to be fetched from remote data source via webservice, converted into csv format, written to a file, and an email is to triggered when all of this is done.<br/>

Given this information, a chain can be as written as below:

```java
Composer.startWith(() -> service.fetchData(), err -> logger.error("Error executing tasks", err))
        .thenPlay(response -> { return converter.convertToCsv(response.data); })
        .thenPlay(csv -> { writer.writeCsvFile(csv); })
        .thenPlay(() -> { mailer.sendEmail("All Tasks Completed"); })
        .thenFinish();
```

Each step returns `Composable`, which can be detached and glued wherever required:

```java
Composable<String> myComposable = Composer.startWith(() -> service.fetchData(), err -> logger.error("Error executing tasks", err))
        .thenPlay(response -> { return converter.convertToCsv(response.data); })

doSomething();
doSomethingMore();

String csv = myComposable.thenPlay(csv -> { writer.writeCsvFile(csv); })
        .thenPlay(() -> { mailer.sendEmail("All Tasks Completed"); })
        .thenFinish();
```

Please note that chained tasks are executed asynchronously by default. Hence, in the above example there is no guarantee that `doSomething()` will be run after the data is converted to csv. If something needs to be executed synchronously in-between, chain it as specified under [Executing task synchronously](#executing-task-synchronously) section.

#### Executing tasks concurrently
Different method variants have been provided to execute multiple tasks concurrently. All you have to do is to specify a collection of tasks to be executed in parallel. The order of execution is never guaranteed.<br/>

- ##### Executing multiple tasks
 
Consider a slight modification in the previous scenario where converted csv is persisted in the database along with a file.<br/> 

In that case, both tasks can be executed concurrently using `thenPlayTogether()` variants like below:

```java
Composer.startWith(() -> service.fetchData(), err -> logger.error("Error executing tasks", err))
        .thenPlay(response -> { return converter.convertToCsv(response.data); })
        .thenPlayTogether( 
            csv -> writer.writeCsvFile(csv),
            db.storeCsv(csv) //both tasks will be executed concurrently
        )
        .thenPlay(() -> { mailer.sendEmail("All Tasks Completed"); })
        .thenFinish();
```
- ##### Collecting output from multiple tasks

In the cases where a task produces an output, concurrent variants can execute any number of tasks with the same type of output, or maximum three tasks with different types of output.<br/> 
    
Such tasks will require a `Collector` as an additional parameter. A `Collector` collects results from multiple producer tasks and returns something which can hold those results.<br/>

Consider a modification in the first scenario where data is to be converted into multiple formats such as csv, xml and yaml. In that case, we can use concurrent method variants and collect results like below:

```java
Composer.startWith(() -> service.fetchData(), err -> logger.error("Error executing tasks", err))
        .thenPlayTogether(
                (response, csv, xml, yaml) -> new ConvertedData(csv, xml, yaml), //ConvertedData is a pojo returned from collector to hold outputs from concurrently executing tasks
                response -> converter.convertToCsv(response.data),
                response -> converter.convertToXml(response.data),
                response -> converter.convertToYaml(response.data)
        )
        .thenPlayTogether(
            convertedData -> writer.writeCsvFile(convertedData.csv),
            convertedData -> writer.writeXmlFile(convertedData.xml),
            convertedData -> writer.writeYamlFile(convertedData.yaml)
        )
        .thenPlay(() -> mailer.sendEmail("All Tasks Completed"))
        .thenFinish();
```
- ##### Iterating over upstream results

In the cases where an upstream output contains a collection, and you want to execute a task concurrently for each value in that collection, use `thenPlayForEachTogether()` variants.<br/>

Consider a scenario where you need to fetch some posts from a service and then fetch comments for each post in the response. In that case, you will need to expand the upstream response to a collection of posts, provide the task to be executed concurrently for each post and finally collect the comments grouped by posts like below:

```java
Composer.startWith(() -> service.fetchPosts(), err -> logger.error("Error executing tasks", err))
        .thenPlayTogether(
                response -> response.getPosts(), //provide a collection to iterate over
                post -> service.fetchComments(post), //this task will be applied for each post in the list
                (response, postAndComments) -> new GroupedData(postAndComments) //collector will receive results as pairs of <Post,List<Comment>> assuming that the service is retuning the list of comments for a specific post
        )
        .thenPlay(data -> { db.insertPostsAndComments(data); })
        .thenPlay(() -> { mailer.sendEmail("All Tasks Completed"); })
        .thenFinish();
```

#### Validating task output
A task output must be `non-null`. Any task in a chain that receives `null` as an input will discontinue further execution.
    
Use `thenContinueIf()` to validate the task output before it is used as an input of dependent tasks. If the condition specified returns false, you will receive a `ComposerException` on the `ErrorStream` provided. Further execution will be discontinued and downstream consuming tasks will receive `null` as a result.
    
For example, in the first scenario, consider that you want to check the status and size of the data in response before converting to csv:

```java
Composer.startWith(() -> service.fetchData(), err -> logger.error("Error executing tasks", err))
        .thenContinueIf(response -> response.status.isOk() && !response.data.isEmpty()) //this will discontinue further execution if the specified condition returns false.
        .thenPlay(response -> { return converter.convertToCsv(response.data); })
        .thenPlay(csv -> { writer.writeCsvFile(csv); })
        .thenPlay(() -> { mailer.sendEmail("All Tasks Completed"); })
        .thenFinish();
```    
     
#### Executing task synchronously
By default, all tasks will be executed asynchronously. If you want to execute something synchronously on the same thread the method has been called (in most cases - the application main thread), `thenWaitFor` variants can be used like below:

```java
Composer.startWith(() -> produceSomething(), err -> logger.error("Error executing tasks", err))
        .thenWaitFor(data -> { showOnUI(data); })
        .thenFinish();
```

#### Providing custom executor service
Finally, Composer uses an `ExecutorService` that creates a cached thread pool internally. If you want to provide your custom executor service, pass it as a third param of `startWith()` like below (not recommended unless required):

```java
Composer.startWith(() -> produceSomething(), err -> logger.error("Error executing tasks", err), customExecutorService)
```

### Changelog
- #### 2.0.0
  - This release contains breaking changes. Major API refactorings include renaming all methods to reduce verbosity.
  - Collection parameters in `then..Together` variants have been replaced with varargs.
- #### 1.0.1
  - Fixed a bug where an `ErrorStream` was not transmitting errors synchronously.


### Licence
```
Copyright 2020 Krupal Shah

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
