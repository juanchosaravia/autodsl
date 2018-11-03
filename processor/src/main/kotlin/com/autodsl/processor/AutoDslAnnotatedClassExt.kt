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
import com.autodsl.annotation.AutoDslCollection
import com.autodsl.annotation.AutoDslMarker
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.sun.tools.javac.code.Symbol
import org.jetbrains.annotations.Nullable
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
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
    val file = File(generatedSourcesRoot)
    file.mkdir()
    val fileSpec = FileSpec.builder(packageOfClass, builderClassName)

    val classBuilderClassName = processingEnv.getClassName(classElement, builderClassName)
    // create builder class
    val classBuilder = TypeSpec.classBuilder(builderClassName)
        .primaryConstructor(FunSpec.constructorBuilder().build())
        .addAnnotation(AutoDslMarker::class)

    if (this.isClassInternalModifier) {
        classBuilder.addModifiers(KModifier.INTERNAL)
    }

    // create properties from the available constructor
    this.getParams().forEach { param: Symbol.VarSymbol ->
        val paramSimpleName = param.simpleName.toString()
        classBuilder.addProperty(createPropertySpec(paramSimpleName, param).build())

        val paramTypeElement = param.asType().asElement()

        // check param has an associated auto-generated builder and create DSL function
        createFunIfAnnotatedWithAutoDsl(
            paramTypeElement,
            paramSimpleName,
            classBuilderClassName,
            processingEnv
        )?.let {
            fileSpec.addImport(it.packageName, it.name)
            classBuilder.addFunction(it.funSpec)
        }

        try {
            val autoDslCollectionData =
                createFunIfAnnotatedWithCollection(param, paramSimpleName, classBuilderClassName)
            if (autoDslCollectionData != null) {
                classBuilder.addType(autoDslCollectionData.nestedClass)
                classBuilder.addFunction(autoDslCollectionData.collectionFun)
            } else {
                // if not annotated then try to check for default supported collections
                createFunIfSupportedCollectionAndNoAnnotation(param, paramSimpleName, classBuilderClassName)?.let {
                    classBuilder.addType(it.nestedClass)
                    classBuilder.addFunction(it.collectionFun)
                }
            }
        } catch (e: ProcessingException) {
            processingEnv.error(e)
        }

        // creates function for builder to be used in Java: "withVariable(..) = this.apply { .. } "
        val funcBuilder = createWithFun(paramSimpleName, param, classBuilderClassName)
        classBuilder.addFunction(funcBuilder.build())
    }

    val classElementTypeName = classElement.asType().asTypeName()
    // add build function to create real object
    val buildFunSpec = createBuildFun(classElementTypeName)
    classBuilder.addFunction(buildFunSpec)

    // create extension function for DSL
    val extFun = createDslExtFun(classBuilderClassName, classElementTypeName)

    fileSpec.addFunction(extFun.build())
        .addType(classBuilder.build())
        .build().writeTo(file)
}

private fun AutoDslAnnotatedClass.createBuildFun(classElementTypeName: TypeName): FunSpec {
    return FunSpec.builder("build")
        .returns(classElementTypeName)
        // todo improve this loop
        .addStatement("return ${classElement.simpleName}(${getParams().joinToString { it.simpleName }})")
        .build()
}

private fun AutoDslAnnotatedClass.createDslExtFun(
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

private fun createWithFun(
    paramSimpleName: String,
    param: Symbol.VarSymbol,
    classBuilderClassName: ClassName
): FunSpec.Builder {
    return FunSpec.builder("with${paramSimpleName.capitalize()}")
        .addParameter(paramSimpleName, param.asTypeName())
        .returns(classBuilderClassName)
        .addStatement("return this.apply { this.$paramSimpleName = $paramSimpleName }")
}

private fun createPropertySpec(
    paramSimpleName: String,
    param: Symbol.VarSymbol
): PropertySpec.Builder {
    val propBuilder = PropertySpec.builder(paramSimpleName, param.asTypeName()).mutable()
    if (param.getAnnotation(Nullable::class.java) != null) {
        // nullable element
        propBuilder.initializer("null")
    } else {
        // non null element
        propBuilder.delegate("%T.notNull()", Delegates::class)
    }
    return propBuilder
}

private fun createFunIfSupportedCollectionAndNoAnnotation(
    param: Symbol.VarSymbol,
    paramSimpleName: String,
    classBuilderClassName: ClassName
): AutoDslCollectionData? {
    val concreteClassName =
        when ((param.asTypeName().javaToKotlinType() as? ParameterizedTypeName)?.rawType?.canonicalName) {
            Constants.LIST_TYPE_NAME -> ArrayList::class.asClassName()
            Constants.SET_TYPE_NAME -> HashSet::class.asClassName()
            else -> {
                return null
            }
        }
    return createCollectionData(param, paramSimpleName, classBuilderClassName, concreteClassName)
}

private fun createFunIfAnnotatedWithCollection(
    param: Symbol.VarSymbol,
    paramSimpleName: String,
    classBuilderClassName: ClassName
): AutoDslCollectionData? {
    val collectionAnnotation = param.getAnnotation(AutoDslCollection::class.java)
        ?: return null

    val collectionAnnotationClassName: ClassName = try {
        collectionAnnotation.concreteType.asTypeName().javaToKotlinType() as ClassName
    } catch (e: MirroredTypeException) {
        if (e.typeMirror !is DeclaredType) {
            throw ProcessingException(
                param,
                "The given type is not supported by AutoDslCollection. Make sure the selected class is a DeclaredType."
            )
        }
        (e.typeMirror as DeclaredType).asTypeName().javaToKotlinType() as ClassName
    }

    return createCollectionData(param, paramSimpleName, classBuilderClassName, collectionAnnotationClassName)
}

private fun createCollectionData(
    param: Symbol.VarSymbol,
    paramSimpleName: String,
    classBuilderClassName: ClassName,
    concreteCollectionClassName: ClassName
): AutoDslCollectionData {
    val parameterizedClassName =
        ((param.asType().asTypeName() as? ParameterizedTypeName)?.typeArguments?.get(0)?.javaToKotlinType() as? ClassName)
            ?: throw ProcessingException(param, "Collection has no parameterized value")
    /*
    Review: This could be improved if we detect there is no repeated parameterized type so we can create a list
    directly in the builder and leverage the use of a class only if it's repeated so we can avoid issues with
    two lists having the same parameterized type and both trying to define unaryPLus method.

    example:
    private val __auto_dsl_collection: ArrayList<Person> = ArrayList()
    operator fun Person.unaryPlus() {
        __auto_dsl_collection.add(this)}
     */

    val collectionFieldName = "collection"
    val collectionClassNameValue = paramSimpleName.toAutoDslCollectionClassName()
    val nestedClass = TypeSpec.classBuilder(collectionClassNameValue)
        .primaryConstructor(FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL).build())
        .addAnnotation(AutoDslMarker::class)
        .addProperty(
            PropertySpec.builder(
                collectionFieldName,
                concreteCollectionClassName.plusParameter(parameterizedClassName),
                KModifier.INTERNAL
            )
                .initializer("%T()", concreteCollectionClassName)
                .build()
        )
        .addFunction(
            FunSpec.builder("unaryPlus")
                .addModifiers(KModifier.OPERATOR)
                .receiver(parameterizedClassName)
                .addCode("$collectionFieldName.add(this)")
                .build()
        )
        .build()

    val collectionFun = FunSpec.builder(paramSimpleName)
        .addParameter(
            ParameterSpec.builder(
                BLOCK_FUN_NAME,
                LambdaTypeName.get(
                    receiver = ClassName.bestGuess(collectionClassNameValue),
                    returnType = Unit::class.asTypeName()
                )
            ).build()
        )
        .returns(classBuilderClassName)
        .addStatement("return this.apply { this.$paramSimpleName = $collectionClassNameValue().apply { $BLOCK_FUN_NAME() }.$collectionFieldName }")
        .build()

    return AutoDslCollectionData(collectionFun, nestedClass)
}

private class AutoDslCollectionData(
    val collectionFun: FunSpec,
    val nestedClass: TypeSpec
)

private fun createFunIfAnnotatedWithAutoDsl(
    paramTypeElement: Symbol.TypeSymbol,
    paramSimpleName: String,
    classBuilderClassName: ClassName,
    processingEnv: ProcessingEnvironment
): AutoDslFunctionData? {
    val paramTypeElementAnnotation = paramTypeElement.getAnnotation(AutoDsl::class.java) ?: return null

    // fun address(block: AddressBuilder.() -> Unit): PersonBuilder = this.apply { this.address = AddressBuilder().apply(block).build() }
    val paramTypeName = paramTypeElement.simpleName.toString()
    val paramBuilderName = paramTypeName.toAutoDslBuilderName()
    val paramBuilderClassName = processingEnv.getClassName(paramTypeElement, paramBuilderName)
    val funSpec = FunSpec.builder(paramTypeElementAnnotation.getDslNameOrDefault(paramSimpleName))
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

    return AutoDslFunctionData(funSpec, paramBuilderClassName.packageName, paramBuilderClassName.simpleName)
}

private class AutoDslFunctionData(
    val funSpec: FunSpec,
    val packageName: String,
    val name: String
)

private fun ProcessingEnvironment.getClassName(element: Element, className: String): ClassName {
    return ClassName(elementUtils.getPackageOf(element).toString(), className)
}