# Composer 
An easy way to build resilient chains of interdependent asynchronous tasks.

> WORK IN PROGRESS

[![Build Status](https://travis-ci.com/krupalshah/Composer.svg?branch=master)](https://travis-ci.com/krupalshah/Composer)
[![Download](https://api.bintray.com/packages/krupalshah55/Composer/Composer/images/download.svg) ](https://bintray.com/krupalshah55/Composer/Composer/_latestVersion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7dde7d053cac44e88d762519ab50b2b1)](https://app.codacy.com/gh/krupalshah/Composer?utm_source=github.com&utm_medium=referral&utm_content=krupalshah/Composer&utm_campaign=Badge_Grade_Settings)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<img src="/raw/legocomposer.jpg?raw=true" width="250" height="250"/>

Composer helps you to organize and execute multiple interdependent input/output 
tasks such as webservice calls, database read/writes 
and file i/o together with concurrency support using `java.util.concurrent` APIs.<br/>
It is compatible with Java 8 & above on all JVM based platforms.

Most client-side mobile/web applications and backend services communicating with each other 
require a framework in which interdependent asynchronous tasks can be glued together. 
There are many libraries out there which allow doing this very effectively. 
However, many of them are either not available for all platforms or require a steep learning curve. 
Composer does not aim to provide an extensible API for managing asynchronous tasks. Instead, it aims to provide a minimal, easy to use API which can be useful for the scenarios where interdependency between such tasks forces you to write boilerplate code for managing state, checking conditions or handling errors.

Here is an example of how you can use Composer to create a chain of tasks. Consider a scenario where you want to get an associated twitter account details for your app user, fetch different kinds of twitter data for that user, show them on app UI and then track the event in your analytics database. All of these tasks are asynchronous and dependent on each other.

```java
Composer.startWith(currentUser.getUserId(), err -> logger.log(err))
        .thenTransform(userId -> accountService.getTwitterAccountDetails(userId))
        .thenContinueIf(response -> response.status.isOk())
        .thenTransformTogether(
            response -> twitterService.getTweets(response.username), 
            response -> twitterService.getMedia(response.username), 
            response -> twitterService.getFollowers(response.username), 
            CollectedResults::new
        )
        .thenConsumeSynchronously(results -> refreshUI(results))
        .thenRun(() -> analyticsDb.trackEvent("get_twitter_details"))
        .thenFinish();
``` 
For detailed usage information, please refer [Getting Started](#getting-started) section.
    
### Adding Dependency
- Gradle:
```groovy
dependencies {
    implementation 'com.krupalshah:composer:1.0.3'
}
```

- Maven:
```xml
<dependency>
  <groupId>com.krupalshah</groupId>
  <artifactId>composer</artifactId>
  <version>1.0.3</version>
  <type>pom</type>
</dependency>
```

- Ivy:
```xml
<dependency org='com.krupalshah' name='composer' rev='1.0.3'>
  <artifact name='composer' ext='pom' />
</dependency>
```
Development snapshots are available in [JFrog artifactory](https://oss.jfrog.org/artifactory/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/com/krupalshah/composer). 

### Getting Started

#### Overview
The API consists of an interface `Composable` and its implementation `Composer`. The implementation serves as an entrypoint and returns `Composable` at each step of execution until chaining is discontinued.

Use `startWith()` to create your first `Composable` like below:

```java
Composer.startWith(someInputOrTask, err -> err.printStackTrace())
```
The first param requires something as an input, or a task which produces the same.<br/>
The second param `ErrorStream` receives any error during execution.<br/>

Use `thenFinish()` to discontinue further chaining and return last awaiting task result. Between `startWith` and `thenFinish`, chain your tasks according to their dependencies.<br/>

#### Chaining Tasks
A `Task` can be anything to be run. It may take something as an input and/or return some output. It can be synchronous or asynchronous. Following methods can be used to chain one or more tasks:
    
- Use `thenRun...` methods for the task which takes no input and returns no output.
- Use `thenConsume...` methods for the task which takes an input but returns no output. 
- Use `thenProduce...` methods for the task which takes no input but returns an output. 
- Use `thenTransform...` methods for the task which takes an input and converts it into output.

For example, consider a very straightforward scenario in which some independent data is to be fetched from remote datasource via webservice, converted into csv format, written to a file, and a message is to be printed to the console when all of this is done.<br/>

Given this information, a chain can be as written as below:

```java
Composer.startWith(() -> service.fetchData(), err -> err.printStackTrace())
        .thenTransform(response -> converter.convertToCsv(response.data))
        .thenConsume(csv -> writer.writeCsvFile(csv))
        .thenRun(() -> logger.log("DONE"))
        .thenFinish();
```

Each step returns `Composable`, which can be detached and glued wherever required:

```java
Composable<String> myComposable = Composer.startWith(() -> "", err -> err.printStackTrace())
        .thenTransform(response -> converter.convertToCsv(response.data));

doSomething();
doSomethingMore();

String csv = myComposable.thenConsume(csv -> writer.writeCsvFile(csv))
        .thenRun(() -> logger.log("DONE"))
        .thenFinish();
```

Please note that chained tasks are executed asynchronously by default. Hence, in above example there is no guarantee that `doSomething()` will be run after the data is converted to csv. If you want to execute something synchronously in-between, chain it as specified under [Executing Synchronous Task](#executing-synchronous-task) section.

#### Executing Multiple Tasks Concurrently
Different variants of above methods have been provided to execute multiple tasks concurrently. All you have to do is to specify a set of tasks to be executed concurrently. The order of execution is never guaranteed.<br/>
    
For example, consider a slight modification in above scenario where converted csv is persisted in the database along with a file.<br/> 

In that case, both tasks can be executed concurrently using `then...Together()` variants like below:

```java
Composer.startWith(() -> service.fetchData(), err -> err.printStackTrace())
        .thenTransform(response -> converter.convertToCsv(response.data))
        .thenConsumeTogether(() -> {
            Set<ConsumingTask> tasks = new LinkedHashSet<>();
            tasks.add(csv -> writer.writeCsvFile(csv));
            tasks.add(csv -> db.storeCsv(csv));
            return tasks;  //both will be executed concurrently
        })
        .thenRun(() -> logger.log("DONE"))
        .thenFinish();
```

In the cases where a task produces an output, concurrent variants can execute any number of tasks with same type of output, or maximum three tasks with different types of output.<br/> 
    
Such tasks will require a `Collector` to be passed as last param to collect outputs produced. A `Collector` collects results from multiple output producing tasks and returns something which can hold those results.<br/>

For example, consider a slight modification in the first scenario where data is to be converted into multiple formats such as csv, xml and yaml. In that case, we can use concurrent variants of above methods and collect results like below:

```java
Composer.startWith(() -> service.fetchData(), err -> err.printStackTrace())
        .thenTransformTogether(
                response -> converter.convertToCsv(response.data),
                response -> converter.convertToXml(response.data),
                response -> converter.convertToYaml(response.data),
                (csv, xml, yaml) -> new ConvertedData(csv, xml, yaml) //ConvertedData is a pojo returned from collector to hold outputs from concurrently executing tasks
        )
        .thenConsumeTogether(() -> {
            Set<ConsumingTask> tasks = new LinkedHashSet<>();
            tasks.add(convertedData -> writer.writeCsvFile(convertedData.csv));
            tasks.add(convertedData -> writer.writeXmlFile(convertedData.xml));
            tasks.add(convertedData -> writer.writeYamlFile(convertedData.yaml));
            return tasks;
        })
        .thenRun(() -> logger.log("DONE"))
        .thenFinish();
```

#### Validating Task Output
A task output must be `non-null`. Any task in a chain that receives `null` as an input will discontinue further execution.
    
Use `thenContinueIf()` to validate the task output before it is used as an input of dependent tasks. If condition specified returns false, you will receive a `ComposerException` on the `ErrorStream` provided. Further execution will be discontinued and `thenFinish` will return `null` as a final result.
    
For example, in the first scenario, consider that you want to check the status and size of the data in response before converting to csv:

```java
Composer.startWith(() -> service.fetchData(), err -> err.printStackTrace())
        .thenContinueIf(response -> response.status.isOk() && !response.data.isEmpty()) //this will discontinue further execution if the specified condition returns false.
        .thenTransform(data -> converter.convertToCsv(data))
        .thenConsume(csv -> writer.writeCsvFile(csv))
        .thenRun(() -> logger.log("DONE"))
        .thenFinish();
```    
     
#### Executing Synchronous Task
By default, all tasks will be executed asynchronously. If you want to execute something synchronously on the same thread the method has been called (in most cases - the application main thread), synchronous variants of above methods `then...Synchronously()` can be used like below:

```java
Composer.startWith(() -> produceSomething(), err -> err.printStackTrace())
        .thenConsumeSynchronously(data -> showOnUI(data))
        .thenFinish();
```

#### Providing Custom ExecutorService
Finally, Composer uses an `ExecutorService` that creates a cached thread pool internally. If you want to provide your custom executor service, pass it as a third param of `startWith()` like below (not recommended unless required):

```java
Composer.startWith(() -> produceSomething(), err -> err.printStackTrace(), customExecutorService)
```

### Sample [TBD]
- Standalone:
    - Standalone demo is available under module `:sample`.
- Web:
    - Spring Boot demo is available under module `:sample-web`.
- Mobile:
    - Android demos are available in 
        - Java under module `:sample-mobile-java`.
        - Kotlin under module `:sample-mobile-kotlin`.
    
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
