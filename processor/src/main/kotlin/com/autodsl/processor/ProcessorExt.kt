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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.SymbolMetadata
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
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

/**
 * FIXME: Workaround found in this issue:
 * https://github.com/square/kotlinpoet/issues/236
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

fun SymbolMetadata.asKotlinMetadata(): KotlinClassMetadata? {
    val values = this.declarationAttributes[1].values // todo fix
    var kind: Int? = null
    var metadataVersion: IntArray? = null
    var bytecodeVersion: IntArray? = null
    var data1: Array<String>? = null
    var data2: Array<String>? = null

    values.forEach { value ->
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