# Auto-DSL for Kotlin
Auto-generate DSL for Kotlin classes using annotations.

[![](https://jitpack.io/v/juanchosaravia/autodsl.svg)](https://jitpack.io/#juanchosaravia/autodsl)


```kotlin
@AutoDsl // indicates to create an associated Builder for this class
class Person(
    val name: String,
    val age: Int,
    val address: Address,
    val friends: List<Person>?
)

@AutoDsl("createAddress") // can specify custom name for dsl creation
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

    @AutoDslConstructor // specify the desired constructor for the builder
    constructor(lat: Float, lng: Float) {
        this.lat = lat
        this.lng = lng
    }
}

@AutoDsl
internal class Box // supports internal classes
```

This will allow you to create those classes like this:
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
}
```
For this example, the processor will detect that "Address" is also marked with "@AutoDsl" 
so it will provide an extra function to initialize the field directly using the builder, 
with the custom builder name, if any.

#### Auto generated builder
Internally will be generating the builder class and extension function for the annotated class.
Let's take `People` as example, the code will look like this:

```kotlin
fun person(block: PersonAutoDslBuilder.() -> Unit): Person = PersonAutoDslBuilder().apply(block).build()

class PersonAutoDslBuilder() {
    var name: String by Delegates.notNull()

    var age: Int by Delegates.notNull()

    var address: Address by Delegates.notNull()

    var friends: List<Person>? = null
    
    // extra function to inline declaration for fields with classes annotated with AutoDsl
    fun createAddress(block: AddressAutoDslBuilder.() -> Unit): PersonAutoDslBuilder = this.apply { this.address = AddressAutoDslBuilder().apply(block).build() }

    fun build(): Person = Person(name, age, address, friends)
    
    // continue...
}
```

For required parameters like `name` the build will fail if it is not set indicating exactly which field is missed.
To make it optional just set a default value like `friends`. This value will be null in case it's not set.

## Download

Add repository:
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependencies
```groovy
dependencies {
    implementation 'com.github.juanchosaravia.autodsl:annotation:latest_version'
    kapt 'com.github.juanchosaravia.autodsl:processor:latest_version'
}
```


## Limitations
* There is an [issue](https://github.com/square/kotlinpoet/issues/236) that generates these limitations:
  * Mutable collections are not supported (like MutableList).
  * Support Nullable types inside other types like `List<String?>`
* Does not support private constructors.

## Pending Features
* Configurable `@DslMarker`
* Support external builders with new annotation `@ManualDsl(type=MyBuilder::class)`.

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