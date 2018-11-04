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
package com.autodsl.processor.model

import com.autodsl.annotation.AutoDsl
import com.autodsl.annotation.AutoDslMarker
import com.autodsl.processor.*
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.ProcessingEnvironment

/**
 * Generates code for [AutoDslClass].
 */
fun ProcessingEnvironment.generateClass(autoDslClass: AutoDslClass) {
    val classElement = autoDslClass.classElement
    val packageOfClass = this.elementUtils.getPackageOf(classElement).toString()
    val builderClassName = autoDslClass.builderClassName

    val generatedSourcesRoot: String = getGeneratedSourcesRoot()
    if (generatedSourcesRoot.isEmpty()) {
        throw ProcessingException(
            classElement,
            "Can't find the target directory for generated Kotlin files."
        )
    }
    val file = File(generatedSourcesRoot)
    file.mkdir()
    val fileSpec = FileSpec.builder(packageOfClass, builderClassName)

    val classBuilderClassName = getClassName(classElement, builderClassName)
    // create builder class
    val classBuilder = TypeSpec.classBuilder(builderClassName)
        .primaryConstructor(FunSpec.constructorBuilder().build())
        .addAnnotation(AutoDslMarker::class)

    if (autoDslClass.isClassInternalModifier) {
        classBuilder.addModifiers(KModifier.INTERNAL)
    }

    // setup properties from the available constructor
    autoDslClass.parameters.forEach { param ->
        val builder = generateParamCode(param, classBuilderClassName)
        builder.imports.forEach { fileSpec.addImport(it.packageName, it.name) }
        builder.properties.forEach { classBuilder.addProperty(it) }
        builder.functions.forEach { classBuilder.addFunction(it) }
        builder.types.forEach { classBuilder.addType(it) }
    }

    val classElementTypeName = classElement.asType().asTypeName()
    // add build function to create real object
    val buildFunSpec = autoDslClass.createBuildFun(classElementTypeName)
    classBuilder.addFunction(buildFunSpec)

    // create extension function for DSL
    val extFun = autoDslClass.createDslExtFun(classBuilderClassName, classElementTypeName)

    fileSpec.addFunction(extFun.build())
        .addType(classBuilder.build())
        .build().writeTo(file)
}

private fun AutoDslClass.createBuildFun(classElementTypeName: TypeName): FunSpec {
    return FunSpec.builder("build")
        .returns(classElementTypeName)
        // todo loop could be improved
        .addStatement("return ${classElement.simpleName}(${parameters.joinToString { it.name }})")
        .build()
}

private fun AutoDslClass.createDslExtFun(
    classBuilderClassName: ClassName,
    classElementTypeName: TypeName
): FunSpec.Builder {
    val extensionFunParams = ParameterSpec.builder(
        BLOCK_FUN_NAME,
        LambdaTypeName.get(
            receiver = classBuilderClassName,
            returnType = Unit::class.asTypeName()
        )
    ).build()

    val classElementAnnotation = classElement.getAnnotation(AutoDsl::class.java)
    val extFunName = classElementAnnotation.getDslNameOrDefault(classElement.simpleName.toString().decapitalize())
    val extFun = FunSpec.builder(extFunName)
        .addParameter(extensionFunParams)
        .returns(classElementTypeName)
        .addStatement("return $builderClassName().apply($BLOCK_FUN_NAME).build()")

    if (this.isClassInternalModifier) {
        extFun.addModifiers(KModifier.INTERNAL)
    }
    return extFun
}