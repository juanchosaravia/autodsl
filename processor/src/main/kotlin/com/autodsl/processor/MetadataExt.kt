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
package com.autodsl.processor

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Type
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.TypeParameter
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver

internal fun ProtoBuf.TypeParameter.asTypeName(
    nameResolver: NameResolver,
    getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter,
    resolveAliases: Boolean = false
): TypeVariableName {
    return TypeVariableName(
        name = nameResolver.getString(name),
        bounds = *(upperBoundList.map {
            it.asTypeName(nameResolver, getTypeParameter, resolveAliases)
        }
            .toTypedArray()),
        variance = variance.asKModifier()
    )
}

internal fun ProtoBuf.TypeParameter.Variance.asKModifier(): KModifier? {
    return when (this) {
        ProtoBuf.TypeParameter.Variance.IN -> KModifier.IN
        ProtoBuf.TypeParameter.Variance.OUT -> KModifier.OUT
        ProtoBuf.TypeParameter.Variance.INV -> null
    }
}

/**
 * Returns the TypeName of this typeInfo as it would be seen in the source code, including nullability
 * and generic typeInfo parameters.
 *
 * @param [nameResolver] a [NameResolver] instance from the source proto
 * @param [getTypeParameter] a function that returns the typeInfo parameter for the given index. **Only
 *     called if [ProtoBuf.Type.hasTypeParameter] is true!**
 */
internal fun ProtoBuf.Type.asTypeName(
    nameResolver: NameResolver,
    getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter,
    useAbbreviatedType: Boolean = true
): TypeName {

    val argumentList = when {
        useAbbreviatedType && hasAbbreviatedType() -> abbreviatedType.argumentList
        else -> argumentList
    }

    if (hasFlexibleUpperBound()) {
        return WildcardTypeName.subtypeOf(
            flexibleUpperBound.asTypeName(nameResolver, getTypeParameter, useAbbreviatedType)
        )
            .asNullableIf(nullable)
    } else if (hasOuterType()) {
        return WildcardTypeName.supertypeOf(
            outerType.asTypeName(nameResolver, getTypeParameter, useAbbreviatedType)
        )
            .asNullableIf(nullable)
    }

    val realType = when {
        hasTypeParameter() -> return getTypeParameter(typeParameter)
            .asTypeName(nameResolver, getTypeParameter, useAbbreviatedType)
            .asNullableIf(nullable)
        hasTypeParameterName() -> typeParameterName
        useAbbreviatedType && hasAbbreviatedType() -> abbreviatedType.typeAliasName
        else -> className
    }

    var typeName: TypeName =
        ClassName.bestGuess(
            nameResolver.getString(realType)
                .replace("/", ".")
        )

    if (argumentList.isNotEmpty()) {
        val remappedArgs: Array<TypeName> = argumentList.map { argumentType ->
            val nullableProjection = if (argumentType.hasProjection()) {
                argumentType.projection
            } else null
            if (argumentType.hasType()) {
                argumentType.type.asTypeName(nameResolver, getTypeParameter, useAbbreviatedType)
                    .let { argumentTypeName ->
                        nullableProjection?.let { projection ->
                            when (projection) {
                                ProtoBuf.Type.Argument.Projection.IN -> WildcardTypeName.supertypeOf(argumentTypeName)
                                ProtoBuf.Type.Argument.Projection.OUT -> {
                                    if (argumentTypeName == ANY) {
                                        // This becomes a *, which we actually don't want here.
                                        // List<Any> works with List<*>, but List<*> doesn't work with List<Any>
                                        argumentTypeName
                                    } else {
                                        WildcardTypeName.subtypeOf(argumentTypeName)
                                    }
                                }
                                ProtoBuf.Type.Argument.Projection.STAR -> WildcardTypeName.subtypeOf(ANY)
                                ProtoBuf.Type.Argument.Projection.INV -> TODO("INV projection is unsupported")
                            }
                        } ?: argumentTypeName
                    }
            } else {
                WildcardTypeName.subtypeOf(ANY)
            }
        }.toTypedArray()
        typeName = (typeName as ClassName).parameterizedBy(*remappedArgs)
    }

    return typeName.asNullableIf(nullable)
}