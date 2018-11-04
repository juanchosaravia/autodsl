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
package com.autodsl.processor.model

import com.autodsl.annotation.AutoDsl
import com.autodsl.annotation.AutoDslCollection
import com.autodsl.processor.asKotlinTypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import org.jetbrains.annotations.Nullable

class AutoDslParam(
    val element: Symbol.VarSymbol
) {
    val name = element.simpleName.toString()
    val type = AutoDslParamType(element)
    val jvmTypeName = element.asType().asTypeName()
    val kotlinTypeName = element.asKotlinTypeName()

    fun isNullable() = element.getAnnotation(Nullable::class.java) != null
    fun getAutoDslCollectionAnnotation(): AutoDslCollection? = element.getAnnotation(AutoDslCollection::class.java)
}

class AutoDslParamType(
    param: Symbol.VarSymbol
) {
    val element: Symbol.TypeSymbol = param.asType().asElement()
    val name = element.simpleName.toString()
    val autoDslAnnotation: AutoDsl? = element.getAnnotation(AutoDsl::class.java)
}