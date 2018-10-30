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
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.SymbolMetadata
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

fun ProcessingEnvironment.getGeneratedSourcesRoot(): String {
    return this.options[AutoDslProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        ?: throw ProcessingException(msg = "No source root for generated file")
}

fun Element.asTypeName(): TypeName {
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
    val metadata = this.metadata.asKotlinMetadata()
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

fun SymbolMetadata.asKotlinMetadata(): KotlinClassMetadata? {
    val metadataAttributes = this.declarationAttributes.firstOrNull {
        it.type.asTypeName() is ClassName
                && (it.type.asTypeName() as ClassName).canonicalName == "kotlin.Metadata"
    }
    val metadataClassValues = metadataAttributes?.values ?: return null
    var kind: Int? = null
    var metadataVersion: IntArray? = null
    var bytecodeVersion: IntArray? = null
    var data1: Array<String>? = null
    var data2: Array<String>? = null

    metadataClassValues.forEach { value ->
        when (value.fst.name.toString()) {
            "mv" -> metadataVersion =
                    (value.snd.value as List<Attribute.Constant>).map { it.value.toString().toInt() }.toIntArray()
            "bv" -> bytecodeVersion =
                    (value.snd.value as List<Attribute.Constant>).map { it.value.toString().toInt() }.toIntArray()
            "k" -> kind = value.snd.value.toString().toInt()
            "d1" -> data1 = (value.snd.value as List<Attribute.Constant>).map { it.value.toString() }.toTypedArray()
            "d2" -> data2 = (value.snd.value as List<Attribute.Constant>).map { it.value.toString() }.toTypedArray()
        }
    }

    val header = KotlinClassHeader(kind, metadataVersion, bytecodeVersion, data1, data2, null, null, null)
    return KotlinClassMetadata.read(header)
}

fun String.toAutoDslBuilderName() = "${this}AutoDslBuilder"