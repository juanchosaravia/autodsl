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

import com.autodsl.app.general.scores
import org.junit.Test

class PersonTest {

    @Test
    fun builderTest() {
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
            friends = listOf(
                person {
                    name = "Arturo"
                    age = 30
                    address {
                        street = "1600 Latta Dr"
                        zipCode = 34747
                    }
                })
        }
        scores {
            points = 2.3
            rating {
                stars = 5
            }
        }
    }

    @Test
    fun validStructureFromDifferentPackages() {
        scores {
            points = 5.0
            rating {
                stars = 5
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun invalidPersonStructure() {
        person {
            name = "Pepe"
        }
    }
}