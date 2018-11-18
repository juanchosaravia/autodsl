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

import com.autodsl.processor.asKModifier
import com.autodsl.processor.asTypeName
import com.squareup.kotlinpoet.*
import me.eugeniomarletti.kotlin.metadata.*
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Class
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Modality.ABSTRACT
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.TypeParameter
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Visibility.INTERNAL
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Visibility.LOCAL
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic.Kind.ERROR

internal data class TargetType(
    val proto: Class,
    val element: TypeElement,
    val constructor: TargetConstructor,
    val typeVariables: List<TypeVariableName>
) {
    val name = element.className.simpleName
    val builderName = element.simpleName.toString().toAutoDslBuilderName()
    val isInternal = proto.visibility == INTERNAL

    companion object {
        fun get(messager: Messager, elements: Elements, element: Element): TargetType? {
            val typeMetadata: KotlinMetadata? = element.kotlinMetadata
            if (element !is TypeElement || typeMetadata !is KotlinClassMetadata) {
                messager.printMessage(
                    ERROR, "@AutoDsl can't be applied to $element: must be a Kotlin class", element
                )
                return null
            }

            val proto = typeMetadata.data.classProto
            when {
                proto.classKind == Class.Kind.ENUM_CLASS -> {
                    messager.printMessage(
                        ERROR,
                        "@AutoDsl with 'generateAdapter = \"true\"' can't be applied to $element: code gen for enums is not supported or necessary",
                        element
                    )
                    return null
                }
                proto.classKind != Class.Kind.CLASS -> {
                    messager.printMessage(
                        ERROR, "@AutoDsl can't be applied to $element: must be a Kotlin class", element
                    )
                    return null
                }
                proto.isInnerClass -> {
                    messager.printMessage(
                        ERROR, "@AutoDsl can't be applied to $element: must not be an inner class", element
                    )
                    return null
                }
                proto.modality == ABSTRACT -> {
                    messager.printMessage(
                        ERROR, "@AutoDsl can't be applied to $element: must not be abstract", element
                    )
                    return null
                }
                proto.visibility == LOCAL -> {
                    messager.printMessage(
                        ERROR, "@AutoDsl can't be applied to $element: must not be local", element
                    )
                    return null
                }
            }

            val typeVariables = genericTypeNames(proto, typeMetadata.data.nameResolver)

            val constructor = TargetConstructor.targetConstructor(typeMetadata, elements)
            return TargetType(proto, element, constructor, typeVariables)
        }

        private val Element.className: ClassName
            get() {
                val typeName = asType().asTypeName()
                return when (typeName) {
                    is ClassName -> typeName
                    is ParameterizedTypeName -> typeName.rawType
                    else -> throw IllegalStateException("unexpected TypeName: ${typeName::class}")
                }
            }

        private fun genericTypeNames(proto: Class, nameResolver: NameResolver): List<TypeVariableName> {
            return proto.typeParameterList.map {
                val possibleBounds = it.upperBoundList
                    .map { it.asTypeName(nameResolver, proto::getTypeParameter, false) }
                val typeVar = if (possibleBounds.isEmpty()) {
                    TypeVariableName(
                        name = nameResolver.getString(it.name),
                        variance = it.varianceModifier
                    )
                } else {
                    TypeVariableName(
                        name = nameResolver.getString(it.name),
                        bounds = *possibleBounds.toTypedArray(),
                        variance = it.varianceModifier
                    )
                }
                return@map typeVar.reified(it.reified)
            }
        }

        private val TypeParameter.varianceModifier: KModifier?
            get() {
                return variance.asKModifier().let {
                    // We don't redeclare out variance here
                    if (it == KModifier.OUT) {
                        null
                    } else {
                        it
                    }
                }
            }
    }
}
