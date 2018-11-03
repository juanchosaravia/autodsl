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

import com.autodsl.app.general.Attempts
import com.autodsl.app.general.scores
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PersonTest {

    @Test
    fun builderPersonTest() {
        val me = person {
            name = "Juan"
            age = 34
            createAddress {
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
                    age = 30
                }
                +person {
                    name = "Tiwa"
                    age = 31
                }
            }
            keys = setOf("home_key", "work_key")
        }
        assertEquals(2, me.friends?.size)
    }

    @Test
    fun encapsulatedListOfStrings() {
        val box = box {
            items {
                +"Hello"
                +"World"
            }
        }
        assertEquals(2, box.items.size)
    }

    @Test
    fun definedListOfStamps() {
        val stampRedColor = "Red"
        val box = box {
            items {
                +"Hello"
                +"World"
            }
            stamps {
                +stamp {
                    colors {
                        +"Red"
                        +"Yellow"
                    }
                }
            }
        }
        assertEquals(2, box.items.size)
        assertEquals(stampRedColor, box.stamps?.first()?.colors?.first())
    }

    @Test
    fun validStructureFromDifferentPackages() {
        scores {
            points = 5.0
            rating {
                stars = 5
            }
            attempts = Attempts(1)
        }
    }

    @Test
    fun interopWithJavaClass() {
        val result = scores {
            points = 5.0
            rating {
                stars = 5
            }
            attempts = Attempts(1)
        }

        assertEquals(1, result.attempts?.counter)
    }

    @Test
    fun validStructureWithNoLocationAddress() {
        createAddress {
            street = "Street"
            zipCode = 1000
        }
    }

    @Test(expected = IllegalStateException::class)
    fun invalidAddressStructure() {
        createAddress {
            street = "Street"
            location = Location()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun invalidPersonStructure() {
        person {
            name = "Pepe"
        }
    }
}