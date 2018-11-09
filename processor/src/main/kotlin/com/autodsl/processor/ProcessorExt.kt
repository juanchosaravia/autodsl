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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

const val BLOCK_FUN_NAME = "block"

fun ProcessingEnvironment.getClassName(element: Element, className: String): ClassName {
    return ClassName(elementUtils.getPackageOf(element).toString(), className)
}

fun ProcessingEnvironment.error(e: ProcessingException) {
    this.error(e.element, e.message ?: "There was an error processing this element.")
}

fun ProcessingEnvironment.error(e: Element, msg: String, vararg args: String) {
    messager?.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e)
}

fun ProcessingEnvironment.getGeneratedSourcesRoot(): String {
    return this.options[AutoDslProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        ?: throw IllegalStateException("No source root for generated file")
}

fun Element.asKotlinTypeName(): TypeName {
    val annotation = this.getAnnotation(Nullable::class.java)
    val typeName = this.javaToKotlinType()
    return if (annotation != null) typeName.asNullable() else typeName
}

fun Element.javaToKotlinType(): TypeName =
    asType().asTypeName().javaToKotlinType()

fun AutoDsl?.getDslNameOrDefault(defaultString: String): String {
    return if (this == null || dslName.isEmpty()) {
        defaultString
    } else {
        dslName
    }
}

/**
 * FIXME: Workaround found in this issue:
 * https://github.com/square/kotlinpoet/issues/236
 * // TODO could we use Kotlin metadata to solve this?
 */
fun TypeName.javaToKotlinType(): TypeName {
    return if (this is ParameterizedTypeName) {
        (rawType.javaToKotlinType() as ClassName).parameterizedBy(*typeArguments.map { it.javaToKotlinType() }.toTypedArray())
    } else {
        val className =
            JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))
                ?.asSingleFqName()?.asString()
        return if (className == null) {
            this
        } else {
            ClassName.bestGuess(className)
        }
    }
}

fun TypeElement.isClassInternal(): Boolean {
    if (this !is Symbol.ClassSymbol) return false

    var isInternal = false
    val metadata = getKotlinMetadata()
    if (metadata is KotlinClassMetadata.Class) {
        metadata.accept(object : KmClassVisitor() {
            override fun visit(flags: Flags, name: kotlinx.metadata.ClassName) {
                if (Flag.IS_INTERNAL(flags)) {
                    isInternal = true
                }
                super.visit(flags, name)
            }
        })
    }
    return isInternal
}

fun TypeElement.getKotlinMetadata(): KotlinClassMetadata? {
    val kotlinMetadata = this.getAnnotation(kotlin.Metadata::class.java) ?: return null
    val header = KotlinClassHeader(
        kotlinMetadata.kind,
        kotlinMetadata.metadataVersion,
        kotlinMetadata.bytecodeVersion,
        kotlinMetadata.data1,
        kotlinMetadata.data2,
        kotlinMetadata.extraString,
        kotlinMetadata.packageName,
        kotlinMetadata.extraInt
    )
    return KotlinClassMetadata.read(header)
}

fun String.toAutoDslBuilderName() = "${this}AutoDslBuilder"
fun String.toAutoDslCollectionClassName() = "${this.capitalize()}AutoDslCollection"