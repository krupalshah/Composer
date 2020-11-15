# Composer 
An easy way to compose chain of interdependent asynchronous tasks.

 ######!!! WORK IN PROGRESS

[![Build Status](https://travis-ci.com/krupalshah/Composer.svg?branch=master)](https://travis-ci.com/krupalshah/Composer)
[![Download](https://api.bintray.com/packages/krupalshah55/Composer/Composer/images/download.svg) ](https://bintray.com/krupalshah55/Composer/Composer/_latestVersion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7dde7d053cac44e88d762519ab50b2b1)](https://app.codacy.com/gh/krupalshah/Composer?utm_source=github.com&utm_medium=referral&utm_content=krupalshah/Composer&utm_campaign=Badge_Grade_Settings)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

<img src="/raw/legocomposer.jpg?raw=true" width="250" height="250"/>

Composer helps you to execute chains of interdependent input/output 
tasks such as webservice calls, database read/writes 
and file i/o with concurrency support using `java.util.concurrent` APIs. 
It is compatible with Java 8 and above.

Most client-side mobile/web applications and backend services communicating with each other 
require a framework in which interdependent asynchronous tasks can be glued together. 
There are many libraries out there which allow doing this very effectively, 
but some of them are either not available for all jvm based platforms or require a steep learning curve. 
The aim of this library is just to provide maximum achievable composability within the constraints of a minimal, easy to use programmable interface.

Here is an example of how you can use composer to execute multiple interdependent asynchronous tasks:
```
Composer.startWith(() -> session.getCurrentUserId(), err -> err.printStackTrace())
        .thenTransform(userId -> userApi.getUserDetails(userId))
        .thenCheckIf(response -> response.status.isOk())
        .thenTransformTogether(response -> postsApi.getPosts(response.username), response -> photosApi.getPhotos(response.username), response -> notesApi.getNotes(response.username), (posts,photos,notes) -> new MergedResult(posts,photos,notes))
        .thenConsumeSynchronously(results -> presentOnUI(results))
        .thenRun(() -> db.trackEvent("user_details"))
        .finish();
``` 
Find more information about usage under [Getting Started](#getting-started) section.
    
### Setup
* Gradle:
```
dependencies {
    implementation 'com.krupalshah:composer:1.0.0'
}
```

* Maven:
```
<dependency>
  <groupId>com.krupalshah</groupId>
  <artifactId>composer</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

* Ivy:
```
<dependency org='com.krupalshah' name='composer' rev='1.0.0'>
  <artifact name='composer' ext='pom' />
</dependency>
```

### Getting Started
TBD
### Sample
TBD
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