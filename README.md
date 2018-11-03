# Auto-DSL for Kotlin
Auto-generates [DSL (Domain Specific Language)](https://en.wikipedia.org/wiki/Domain-specific_language) for your Kotlin classes using annotations.

[ ![Download](https://api.bintray.com/packages/juanchosaravia/autodsl/com.juanchosaravia.autodsl%3Aprocessor/images/download.svg) ](https://bintray.com/juanchosaravia/autodsl/com.juanchosaravia.autodsl%3Aprocessor/_latestVersion)

No more boilerplate code to create your own DSL. 

Create expressive DSL like this:
```kotlin
person {
    name = "Juan"
    age = 34
    createAddress {
        street = "200 Celebration Bv"
        zipCode = 34747
        location {
            lat = 100f
            lng = 100f
        }
    }
    friends {
        +person {
            name = "Arturo"
            age = 30
        }
        +person {
            name = "Tiwa"
            age = 30
        }
    }
}
```

To generate the previous DSL, you just need these classes and the AutoDsl annotations:
```kotlin
@AutoDsl
class Person(
    val name: String,
    val age: Int,
    val address: Address?,
    val friends: List<Person>?, // List and Set have default concrete types
    @AutoDslCollection(concreteType = TreeSet::class) // specify custom concrete type
    val keys: Set<String>?
)

@AutoDsl("createAddress") // set custom name for DSL
data class Address( // can be used in data classes
    val street: String,
    val zipCode: Int,
    internal val location: Location? // supports internal fields
)

@AutoDsl
class Location {
    val lat: Float
    val lng: Float

    constructor() {
        lat = 0F
        lng = 0F
    }

    // specify the desired constructor in multiple constructors
    @AutoDslConstructor
    constructor(lat: Float, lng: Float) {
        this.lat = lat
        this.lng = lng
    }
}
```

For this example, the processor will detect that `Address` is also marked with `@AutoDsl` 
so it will provide an extra function to initialize the field directly using the builder, 
with the custom builder name, if any.

Internally will be generating a builder class and extension function for the annotated class. 

For required parameters like `name` the DSL will throw an exception on runtime indicating exactly which field is missed.
To make it optional just set the property as nullable with the question mark like `friends` or `keys`. The value will be null in case it's not set.

#### Examples
- Annotation examples: [Person.kt](app/src/main/kotlin/com/autodsl/app/Person.kt)
- DSL Usage: [PersonTest.kt](app/src/test/kotlin/com/autodsl/app/PersonTest.kt)

## Download

Add repository:
```groovy
repositories {
    maven { url "https://dl.bintray.com/juanchosaravia/autodsl" }
    maven { url "https://kotlin.bintray.com/kotlinx/" }
}
```
*Note: kotlinx is only available from bintray so we are forced to 
add this repo for now and is going to be used just for the processor.* 

##### Add the dependencies
```groovy
dependencies {
    api "com.juanchosaravia.autodsl:annotation:latest_version"
    kapt "com.juanchosaravia.autodsl:processor:latest_version"
}
```

## Limitations
* Does not support private constructors.
* There is an [issue](https://github.com/square/kotlinpoet/issues/236) that generates these limitations:
  * Mutable collections are not supported as type in the constructor parameter (like MutableList). 
  Still you can use concrete types or non-mutable interfaces like List, Set, etc. See examples for more options.
  * Support Nullable types inside other types like `List<String?>`

## Pending Features
* Use the default values set in the class definition.
* Support external builders with new annotation `@ManualDsl(type=MyBuilder::class)`.
* Custom names for builders to improve integration with Java.

## Debug
If you want to debug the processor do the following steps:

1. Run this command:
    ```text
    ./gradlew clean :app:build --no-daemon -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy="in-process" -Dkotlin.daemon.jvm.options="-Xdebug,-Xrunjdwp:transport=dt_socket\,address=5005\,server=y\,suspend=n"
    ```
2. In IntelliJ Idea go to Tools > Edit Configurations > press "+" icon in the left top corner.
Add a new "Remote". Set a Name and check the "Single instance only" flag to true.
3. Press "Debug" button to run the newly created "Remote" configuration.

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