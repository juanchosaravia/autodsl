#[DEPRECATED]
Unfortunately, since Kotlin 1.4.x this library doesn't work anymore. It has a strong dependency on me.eugeniomarletti.kotlin.metadata:kotlin-metadata 
lib which doesn't work anymore in 1.4.x to extract Kotlin Metadata.

If you think this feature should be back into 1.4.x then please let me know in the Issue section, 
probably we could invest some time trying to achieve the same with the new Kotlin Backend.

# AutoDsl for Kotlin
Auto-generates [DSL (Domain Specific Language)](https://en.wikipedia.org/wiki/Domain-specific_language) 
for your Kotlin projects using annotations.

[![CircleCI](https://circleci.com/gh/juanchosaravia/autodsl.svg?style=svg)](https://circleci.com/gh/juanchosaravia/autodsl)
[ ![Download](https://api.bintray.com/packages/juanchosaravia/autodsl/io.github.juanchosaravia.autodsl%3Aprocessor/images/download.svg) ](https://bintray.com/juanchosaravia/autodsl/io.github.juanchosaravia.autodsl%3Aprocessor/_latestVersion)

## Table of Contents
1. [Documentation](#documentation)
2. [Download](#download)


Create expressive, immutable and type-safe DSL **without boilerplate code**:
```kotlin
person {
    name = "Juan"
    age = 34
    newAddress {
        street = "200 Celebration Bv"
        zipCode = 34747
        location {
            lat = 100.0
            lng = 100.0
        }
    }
    friends {
        +person {
            name = "Arturo"
            age = 28
        }
        +person {
            name = "Tiwa"
            age = 30
        }
    }
}
```

To generate the previous DSL you just need to provide your desired classes with `@AutoDsl`:
```kotlin
@AutoDsl
class Person(
    val name: String,
    val age: Int,
    val address: Address?,
    val friends: List<Person>?
)

@AutoDsl("newAddress") // set custom name for DSL
data class Address(      // can be used in data classes
    val street: String,
    val zipCode: Int,
    val location: Location?
)

@AutoDsl
class Location {
    val lat: Double
    val lng: Double

    constructor() {
        lat = 0.0
        lng = 0.0
    }

    // in multiple constructors you can specify which one to use.
    @AutoDslConstructor
    constructor(lat: Double, lng: Double) {
        this.lat = lat
        this.lng = lng
    }
}
```

AutoDsl will be generating a builder class and extension function for 
the annotated class providing this super expressive DSL. 

For required parameters like `name` the DSL will throw an exception at 
runtime indicating exactly which field is missed.
To make it optional just set the property as nullable with the 
question mark like `friends`. The value will be null in 
case it's not set.

> Note: Default parameters in constructor is not currently supported 
as there is no way to get that value in the process to generate the code.
There is a workaround that you can use explained in the 
[wiki page](https://github.com/juanchosaravia/autodsl/wiki#default-parameters).

## Documentation
Visit the Wiki for a full list of features and more details: [AutoDsl-Wiki](https://github.com/juanchosaravia/autodsl/wiki)

For more Examples
- Annotation examples: [Person.kt](app/src/main/kotlin/com/autodsl/app/Person.kt)
- DSL examples usage: [AutoDslTest.kt](app/src/test/kotlin/com/autodsl/app/AutoDslTest.kt)
- [Android Sample](samples/android-autodsl/README.md)

## Download

##### Add JCenter repository:
```groovy
repositories {
    jcenter()
}
```

##### Add the dependencies
[ ![Download](https://api.bintray.com/packages/juanchosaravia/autodsl/io.github.juanchosaravia.autodsl%3Aprocessor/images/download.svg) ](https://bintray.com/juanchosaravia/autodsl/io.github.juanchosaravia.autodsl%3Aprocessor/_latestVersion)
```groovy
dependencies {
    api "io.github.juanchosaravia.autodsl:annotation:latest_version"
    kapt "io.github.juanchosaravia.autodsl:processor:latest_version"
}
```

## Debug
If you want to debug the processor do the following steps:

1. Run this command:
    ```text
    ./gradlew clean :app:build --no-daemon -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy="in-process" -Dkotlin.daemon.jvm.options="-Xdebug,-Xrunjdwp:transport=dt_socket\,address=5005\,server=y\,suspend=n"
    ```
2. In IntelliJ Idea go to Tools > Edit Configurations > press "+" icon in the left top corner.
Add a new "Remote". Set a Name and check the "Single instance only" flag to true.
3. Press "Debug" button to run the newly created "Remote" configuration.

## Publish
* Update version in release-brintray.gradle file:
```text 
libraryVersion = 'x.y.z'
```
* Setup bintray user and pass in local.properties:
```text
bintray.user=username
bintray.apikey=apikey
```
* Run:
```
./gradlew :annotation:bintrayUpload
./gradlew :processor:bintrayUpload
```


## License
        
    Copyright 2018 Juan Ignacio Saravia
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.  
