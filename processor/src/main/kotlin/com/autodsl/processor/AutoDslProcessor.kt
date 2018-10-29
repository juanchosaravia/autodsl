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
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol
import kotlinx.metadata.*
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
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
            val values = (classElement as Symbol.ClassSymbol).metadata.declarationAttributes[1].values
            var kind: Int? = null
            var metadataVersion: IntArray? = null
            var bytecodeVersion: IntArray? = null
            var data1: Array<String>? = null
            var data2: Array<String>? = null

            values.forEach { value ->
                when(value.fst.name.toString()) {
                    "mv" -> metadataVersion = (value.snd.value as List<Attribute.Constant>).map { it.value.toString().toInt() }.toIntArray()
                    "bv" -> bytecodeVersion = (value.snd.value as List<Attribute.Constant>).map { it.value.toString().toInt() }.toIntArray()
                    "k" -> kind = value.snd.value.toString().toInt()
                    "d1" -> data1 = (value.snd.value as List<Attribute.Constant>).map { it.value.toString() }.toTypedArray()
                    "d2" -> data2 = (value.snd.value as List<Attribute.Constant>).map { it.value.toString() }.toTypedArray()
                }
            }

            val header = KotlinClassHeader(kind, metadataVersion, bytecodeVersion, data1, data2, null, null, null)
            val metadata = KotlinClassMetadata.read(header)

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