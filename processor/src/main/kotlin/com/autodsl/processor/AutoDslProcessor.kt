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

import com.autodsl.annotation.AutoDsl
import com.google.auto.service.AutoService
import com.sun.tools.javac.code.Symbol
import kotlinx.metadata.ClassName
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic


/**
 * AutoDsl Processor
 */
@AutoService(Processor::class)
@SupportedOptions(AutoDslProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AutoDslProcessor : AbstractProcessor() {

    private var messager: Messager? = null

    override fun init(env: ProcessingEnvironment?) {
        super.init(env)
        messager = env?.messager
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(AutoDsl::class.java).forEach { classElement ->
            if (classElement.kind != ElementKind.CLASS) {
                error(classElement, "Only classes can be annotated with %s.", AutoDsl::class.java.simpleName)
                return true
            }
            classElement as TypeElement

            var isInternal = false
            val metadata = (classElement as Symbol.ClassSymbol).metadata.asKotlinMetadata()

            if (metadata is KotlinClassMetadata.Class) {
                metadata.accept(object : KmClassVisitor() {
                    override fun visit(flags: Flags, name: ClassName) {
                        if (Flag.IS_INTERNAL(flags)) {
                            isInternal = true
                        }
                        super.visit(flags, name)
                    }
                })
            }

            val modifiers = classElement.modifiers
            // check class is public and not abstract
            if (!modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.ABSTRACT)) {
                error(
                    classElement,
                    "The class %s is not public or is abstract.",
                    classElement.qualifiedName.toString()
                )
                return true
            }

            try {
                val annotatedClass = AutoDslAnnotatedClass(classElement)
                annotatedClass.generateClass(processingEnv, isInternal)
            } catch (e: Throwable) {
                error(classElement, e.message.orEmpty())
                return true
            }
        }
        return false // false=continue; true=exit process
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(AutoDsl::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun error(e: Element, msg: String, vararg args: String) {
        messager?.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e)
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}