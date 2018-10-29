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

@AutoDsl
data class Address( // can be used in data classes
    val street: String,
    val zipCode: Int,
    val location: Location?
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
    address {
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
so it will provide an extra function to initialize the field directly using the builder. 

#### Auto generated builder
Internally will be generating the builder class and extension function for the annotated class.
Let's take `People` as example, the code will look like this:

```kotlin
fun person(block: PersonBuilder.() -> Unit): Person = PersonBuilder().apply(block).build()

class PersonBuilder() {
    var name: String by Delegates.notNull()

    var age: Int by Delegates.notNull()

    var address: Address by Delegates.notNull()

    var friends: List<Person>? = null
    
    // extra function to inline declaration for fields with classes annotated with AutoDsl
    fun address(block: AddressBuilder.() -> Unit): PersonBuilder = this.apply { this.address = AddressBuilder().apply(block).build() }

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
* Configure DSL function name with `@AutoDsl(name="createPerson")`
* Configurable `@DslMarker`
* Support external builders with new annotation `@ManualDsl(type=MyBuilder)`.

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