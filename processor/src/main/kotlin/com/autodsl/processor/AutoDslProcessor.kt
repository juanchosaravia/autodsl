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
import com.autodsl.processor.internal.TargetType
import com.autodsl.processor.internal.generateClass
import com.google.auto.service.AutoService
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.KotlinMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement


/**
 * AutoDsl Processor
 */
@AutoService(Processor::class)
@SupportedOptions(AutoDslProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AutoDslProcessor : AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(AutoDsl::class.java)
            .asSequence()
            .map { it as TypeElement }
            .forEach { classElement ->
                val typeMetadata: KotlinMetadata? = classElement.kotlinMetadata
                if (typeMetadata !is KotlinClassMetadata) {
                    processingEnv.error(
                        classElement,
                        "@AutoDsl must be used in a Kotlin class, cannot be used in $classElement"
                    )
                    return@forEach
                }

                try {
                    val targetType = TargetType.get(
                        processingEnv.messager,
                        processingEnv.elementUtils,
                        classElement
                    ) ?: return@forEach

                    processingEnv.generateClass(targetType)
                } catch (pe: ProcessingException) {
                    processingEnv.error(pe)
                    return@forEach
                } catch (e: Throwable) {
                    processingEnv.error(
                        classElement,
                        "There was an error while processing your annotated classes. error = ${e.message.orEmpty()}"
                    )
                    return@forEach
                }
            }
        return true // false=continue; true=exit process
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(AutoDsl::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}