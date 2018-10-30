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

import com.autodsl.annotation.AutoDslConstructor
import com.sun.tools.javac.code.Symbol
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class AutoDslAnnotatedClass(val classElement: TypeElement) {

    private val constructor: Element
    val builderClassName = classElement.simpleName.toString().toAutoDslBuilderName()
    val isClassInternalModifier: Boolean

    init {
        val publicConstructors: List<Element> = classElement.enclosedElements.filter {
            it.kind == ElementKind.CONSTRUCTOR && it.modifiers.contains(Modifier.PUBLIC)
        }
        var mainConstructor: Element? = publicConstructors.firstOrNull {
            it.getAnnotation(AutoDslConstructor::class.java) != null
        }
        if (mainConstructor == null) {
            if (publicConstructors.isEmpty()) {
                throw ProcessingException(classElement, "There is no constructor that match required criteria.")
            }
            mainConstructor = publicConstructors.first()
        }
        constructor = mainConstructor
        isClassInternalModifier = classElement.isClassInternal()
    }

    fun formatToBuilderName(classSimpleName: String) = "${classSimpleName}Builder"
    fun getParams(): List<Symbol.VarSymbol> = (constructor as Symbol.MethodSymbol).params
}