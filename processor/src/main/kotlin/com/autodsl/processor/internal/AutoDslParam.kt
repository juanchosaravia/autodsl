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
package com.autodsl.processor.internal

import com.autodsl.annotation.AutoDsl
import com.autodsl.annotation.AutoDslCollection
import com.autodsl.processor.asTypeName
import com.sun.tools.javac.code.Symbol
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver

internal class AutoDslParam(
    val name: String,
    param: TargetParameter,
    protoClass: ProtoBuf.Class,
    nameResolver: NameResolver
) {
    val element = param.element
    val typeInfo = AutoDslParamType(element as Symbol.VarSymbol)
    val typeName = param.proto.type.asTypeName(nameResolver, protoClass::getTypeParameter)

    fun isNullable() = typeName.nullable
    fun getAutoDslCollectionAnnotation(): AutoDslCollection? = element.getAnnotation(AutoDslCollection::class.java)
}

class AutoDslParamType(
    param: Symbol.VarSymbol
) {
    val element: Symbol.TypeSymbol = param.asType().asElement()
    val name = element.simpleName.toString()
    val autoDslAnnotation: AutoDsl? = element.getAnnotation(AutoDsl::class.java)
}