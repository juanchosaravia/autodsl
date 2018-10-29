/*
 * Copyright 2018 Juan Ignacio Saravia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.autodsl.app

import com.autodsl.annotation.AutoDsl
import com.autodsl.annotation.AutoDslConstructor

@AutoDsl // indicates to create an associated Builder for this class
class Person(
    val name: String,
    val age: Int,
    val address: Address,
    val friends: List<Person>?
)

@AutoDsl("createAddress")
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