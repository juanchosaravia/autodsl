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

import com.autodsl.annotation.AutoDslConstructor
import com.autodsl.processor.ProcessingException
import com.sun.tools.javac.code.Symbol
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Constructor
import me.eugeniomarletti.kotlin.metadata.visibility
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.util.Elements

internal data class TargetConstructor(
    val element: ExecutableElement,
    val proto: Constructor,
    val parameters: Map<String, TargetParameter>
) {
    companion object {
        fun targetConstructor(metadata: KotlinClassMetadata, elements: Elements): TargetConstructor {
            val (nameResolver, classProto) = metadata.data

            val constructorElements = classProto.fqName
                .let(nameResolver::getString)
                .replace('/', '.')
                .let(elements::getTypeElement)
                .enclosedElements
                .mapNotNull { element ->
                    element.takeIf { it.kind == ElementKind.CONSTRUCTOR }?.let { it as ExecutableElement }
                }

            var proto: ProtoBuf.Constructor = classProto.constructorList.first()
            var element: ExecutableElement = constructorElements.first()
            for ((index, constructorElement) in constructorElements.withIndex()) {
                if (constructorElement.isAutoDslCollection()) {
                    proto = classProto.constructorList.getOrNull(index) ?: proto
                    element = constructorElement
                    break
                }
            }

            if (proto.visibility != ProtoBuf.Visibility.INTERNAL && proto.visibility != ProtoBuf.Visibility.PUBLIC) {
                throw ProcessingException(
                    element, "@AutoDsl can't be applied to $element: " +
                            "constructor is not internal or public"
                )
            }

            val parameters = mutableMapOf<String, TargetParameter>()
            for (parameter in proto.valueParameterList) {
                val name = nameResolver.getString(parameter.name)
                val index = proto.valueParameterList.indexOf(parameter)
                parameters[name] = TargetParameter(name, parameter, index, element.parameters[index])
            }

            return TargetConstructor(element, proto, parameters)
        }

        // FIXME review how to accomplish this as getAnnotation was not returning it
        private fun ExecutableElement.isAutoDslCollection(): Boolean {
            (this as? Symbol.MethodSymbol)?.annotationMirrors?.forEach {
                if (it.value.type.asElement().simpleName.toString() == AutoDslConstructor::class.java.simpleName)
                    return true
            }
            return false
        }
    }
}
