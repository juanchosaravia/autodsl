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
import com.squareup.kotlinpoet.*
import com.sun.tools.javac.code.Symbol
import org.jetbrains.annotations.Nullable
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import kotlin.properties.Delegates

private const val BLOCK_FUN_NAME = "block"

/**
 * Generates a Builder class with the current [AutoDslAnnotatedClass].
 */
fun AutoDslAnnotatedClass.generateClass(processingEnv: ProcessingEnvironment) {
    val packageOfClass = processingEnv.elementUtils.getPackageOf(classElement).toString()

    val generatedSourcesRoot: String = processingEnv.getGeneratedSourcesRoot()
    if (generatedSourcesRoot.isEmpty()) {
        throw ProcessingException(this.classElement, "Can't find the target directory for generated Kotlin files.")
    }

    val classBuilderClassName = processingEnv.getClassName(classElement, builderClassName)
    // create builder class
    val classBuilder = TypeSpec.classBuilder(builderClassName)
        .primaryConstructor(FunSpec.constructorBuilder().build())

    if (this.isClassInternalModifier) {
        classBuilder.addModifiers(KModifier.INTERNAL)
    }

    // add each constructor param as property in builder class
    this.getParams().forEach { param: Symbol.VarSymbol ->
        val paramSimpleName = param.simpleName.toString()
        val propBuilder = PropertySpec.builder(paramSimpleName, param.asTypeName()).mutable()
        if (param.getAnnotation(Nullable::class.java) != null) {
            // nullable element
            propBuilder.initializer("null")
        } else {
            // non null element
            propBuilder.delegate("%T.notNull()", Delegates::class)
        }
        classBuilder.addProperty(propBuilder.build())

        // check param has an associated auto-generated builder and create DSL function
        val paramTypeElement = param.type.asElement()
        if (paramTypeElement.getAnnotation(AutoDsl::class.java) != null) {
            // fun address(block: AddressBuilder.() -> Unit): PersonBuilder = this.apply { this.address = AddressBuilder().apply(block).build() }
            val paramTypeName = paramTypeElement.simpleName.toString()
            val paramBuilderName = formatName(paramTypeName)
            val paramBuilderClassName = processingEnv.getClassName(param, paramBuilderName)
            classBuilder.addFunction(
                FunSpec.builder(paramSimpleName)
                    .addParameter(
                        ParameterSpec.builder(
                            BLOCK_FUN_NAME,
                            LambdaTypeName.get(
                                receiver = paramBuilderClassName,
                                returnType = Unit::class.asTypeName()
                            )
                        ).build()
                    )
                    .returns(classBuilderClassName)
                    .addStatement("return this.apply { this.$paramSimpleName = $paramBuilderName().apply($BLOCK_FUN_NAME).build() }")
                    .build()
            )
        }

        // creates function for builder to be used in Java: "withVariable(..) = this.apply { .. } "
        val funcBuilder = FunSpec.builder("with${paramSimpleName.capitalize()}")
            .addParameter(paramSimpleName, param.asTypeName())
            .returns(classBuilderClassName)
            .addStatement("return this.apply { this.$paramSimpleName = $paramSimpleName }")

        classBuilder.addFunction(funcBuilder.build())
    }

    val classElementTypeName = classElement.asType().asTypeName()
    // add build function to create real object
    classBuilder.addFunction(
        FunSpec.builder("build")
            .returns(classElementTypeName)
            // todo improve this
            .addStatement("return ${classElement.simpleName}(${this.getParams().joinToString { it.simpleName }})")
            .build()
    )

    // create extension function for DSL
    val extensionFunParams = ParameterSpec.builder(
        BLOCK_FUN_NAME,
        LambdaTypeName.get(
            receiver = classBuilderClassName,
            returnType = Unit::class.asTypeName()
        )
    ).build()

    val extFun = FunSpec.builder(classElement.simpleName.toString().decapitalize())
        .addParameter(extensionFunParams)
        .returns(classElementTypeName)
        .addStatement("return $builderClassName().apply($BLOCK_FUN_NAME).build()")

    if (this.isClassInternalModifier) {
        extFun.addModifiers(KModifier.INTERNAL)
    }

    val file = File(generatedSourcesRoot)
    file.mkdir()
    FileSpec.builder(packageOfClass, builderClassName)
        .addFunction(extFun.build())
        .addType(classBuilder.build())
        .build().writeTo(file)
}

private fun ProcessingEnvironment.getClassName(element: Element, className: String): ClassName {
    return ClassName(elementUtils.getPackageOf(element).toString(), className)
}