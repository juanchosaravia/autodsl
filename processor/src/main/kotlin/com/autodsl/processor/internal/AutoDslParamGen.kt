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
package com.autodsl.processor.internal

import com.autodsl.annotation.AutoDslMarker
import com.autodsl.processor.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import kotlin.properties.Delegates

/**
 * Generates code for [AutoDslParam].
 */
internal fun ProcessingEnvironment.generateParamCode(
    param: AutoDslParam,
    builderClassName: ClassName
): AutoDslParamSpec {

    val properties = mutableListOf(createPropertySpec(param).build())
    val imports = mutableListOf<AutoDslImportSpec>()
    val functions = mutableListOf<FunSpec>()
    val types = mutableListOf<TypeSpec>()

    // check param has an associated auto-generated builder and create DSL function
    createFunIfAnnotatedWithAutoDsl(
        param,
        builderClassName,
        this
    )?.let {
        imports.add(it.importData)
        functions.add(it.funSpec)
    }

    try {
        val autoDslCollectionData =
            createFunIfAnnotatedWithCollection(param, builderClassName)
        if (autoDslCollectionData != null) {
            autoDslCollectionData.nestedClass?.let {
                types.add(it)
            }
            autoDslCollectionData.propertySpec?.let {
                properties.add(it)
            }
            functions.add(autoDslCollectionData.collectionFun)
        } else {
            // if not annotated then try to check for default supported collections
            createFunIfSupportedCollectionAndNoAnnotation(param, builderClassName)?.let { data ->
                data.propertySpec?.let { properties.add(it) }
                data.nestedClass?.let { types.add(it) }
                functions.add(data.collectionFun)
            }
        }
    } catch (e: ProcessingException) {
        error(e)
    }

    // creates function for builder to be used in Java: "withVariable(..) = this.apply { .. } "
    functions.add(createWithFun(param, builderClassName).build())

    return AutoDslParamSpec(imports, properties, functions, types)
}

private fun createWithFun(
    param: AutoDslParam,
    builderClassName: ClassName
): FunSpec.Builder {
    val paramName = param.name
    return FunSpec.builder("with${paramName.capitalize()}")
        .addParameter(paramName, param.typeName)
        .returns(builderClassName)
        .addStatement("return this.apply { this.$paramName = $paramName}")
}

private fun createPropertySpec(
    param: AutoDslParam
): PropertySpec.Builder {
    val propBuilder = PropertySpec.builder(param.name, param.typeName).mutable()
    if (param.isNullable()) {
        // nullable element
        propBuilder.initializer("null")
    } else {
        // non null element
        propBuilder.delegate("%T.notNull()", Delegates::class)
    }
    return propBuilder
}

private fun createFunIfSupportedCollectionAndNoAnnotation(
    param: AutoDslParam,
    builderClassName: ClassName
): AutoDslCollectionData? {
    val concreteClassName =
        when ((param.typeName as? ParameterizedTypeName)?.rawType?.canonicalName) {
            Constants.LIST_TYPE_NAME -> ArrayList::class.asClassName()
            Constants.MUTABLE_LIST_TYPE_NAME -> ArrayList::class.asClassName()
            Constants.SET_TYPE_NAME -> HashSet::class.asClassName()
            Constants.MUTABLE_SET_TYPE_NAME -> HashSet::class.asClassName()
            else -> {
                return null
            }
        }
    return createCollectionData(param, builderClassName, concreteClassName)
}

private fun createFunIfAnnotatedWithCollection(
    param: AutoDslParam,
    builderClassName: ClassName
): AutoDslCollectionData? {
    val collectionAnnotation = param.getAutoDslCollectionAnnotation()
        ?: return null

    val collectionAnnotationClassName: ClassName = try {
        collectionAnnotation.concreteType.asClassName()
    } catch (e: MirroredTypeException) {
        if (e.typeMirror !is DeclaredType) {
            throw ProcessingException(
                param.element,
                "The given type is not supported by AutoDslCollection. Not able to retrieve type."
            )
        }
        ((e.typeMirror as DeclaredType).asElement() as? TypeElement)?.asClassName()
            ?: throw ProcessingException(
                param.element,
                "The given type is not supported by AutoDslCollection. Type or class not resolved."
            )
    }

    return createCollectionData(param, builderClassName, collectionAnnotationClassName)
}

private fun createCollectionData(
    param: AutoDslParam,
    builderClassName: ClassName,
    concreteCollectionClassName: ClassName
): AutoDslCollectionData {
    val parameterizedClassName =
        ((param.typeName as? ParameterizedTypeName)?.typeArguments?.get(0) as? ClassName)
            ?: throw ProcessingException(param.element, "Collection has no parameterized value")
    /*
    Review: This could be improved if we detect there is no repeated parameterized type so we can create a list
    directly in the builder and leverage the use of a class only if it's repeated so we can avoid issues with
    two lists having the same parameterized type and both trying to define unaryPLus method.

    example:
    private val __auto_dsl_collection: ArrayList<Person> = ArrayList()
    operator fun Person.unaryPlus() {
        __auto_dsl_collection.add(this)}
     */

    val paramName = param.name
    val collectionFieldName = "_${paramName}AutoDslCollection"

    val unaryPlusFunc = FunSpec.builder("unaryPlus")
        .addModifiers(KModifier.OPERATOR)
        .receiver(parameterizedClassName)
        .addStatement("$collectionFieldName.add(this)")

    val collectionPropertySpecBuilder = PropertySpec.builder(
        collectionFieldName,
        concreteCollectionClassName.plusParameter(parameterizedClassName)
    )
        .initializer("%T()", concreteCollectionClassName)

    if (param.getAutoDslCollectionAnnotation()?.inline == true) {
        return AutoDslCollectionData(
            collectionFun = unaryPlusFunc
                // includes assigment to param
                .addStatement("$paramName = $collectionFieldName")
                .build(),
            propertySpec = collectionPropertySpecBuilder.apply { modifiers.add(KModifier.PRIVATE) }.build()
        )
    }
    val collectionClassNameValue = paramName.toAutoDslCollectionClassName() // todo review this
    val nestedClass = TypeSpec.classBuilder(collectionClassNameValue)
        .primaryConstructor(FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL).build())
        .addAnnotation(AutoDslMarker::class)
        .addProperty(collectionPropertySpecBuilder.apply { modifiers.add(KModifier.INTERNAL) }.build())
        .addFunction(unaryPlusFunc.build())
        .build()

    val collectionFun = FunSpec.builder(paramName)
        .addParameter(
            ParameterSpec.builder(
                BLOCK_FUN_NAME,
                LambdaTypeName.get(
                    receiver = ClassName.bestGuess(collectionClassNameValue),
                    returnType = Unit::class.asTypeName()
                )
            ).build()
        )
        .returns(builderClassName)
        .addStatement("this.$paramName = $collectionClassNameValue().apply { $BLOCK_FUN_NAME() }.$collectionFieldName")
        .addStatement("return this")
        .build()

    return AutoDslCollectionData(collectionFun, nestedClass)
}

private class AutoDslCollectionData(
    val collectionFun: FunSpec,
    val nestedClass: TypeSpec? = null,
    val propertySpec: PropertySpec? = null
)

private fun createFunIfAnnotatedWithAutoDsl(
    param: AutoDslParam,
    builderClassName: ClassName,
    processingEnv: ProcessingEnvironment
): AutoDslFunctionData? {
    val paramType = param.typeInfo
    val paramTypeElementAnnotation = paramType.autoDslAnnotation ?: return null

    // fun address(block: AddressBuilder.() -> Unit): PersonBuilder = this.apply { this.address = AddressBuilder().apply(block).build() }
    val paramBuilderName = paramType.name.toAutoDslBuilderName()
    val paramBuilderClassName = processingEnv.getClassName(paramType.element, paramBuilderName)
    val funSpec = FunSpec.builder(paramTypeElementAnnotation.getDslNameOrDefault(param.name))
        .addParameter(
            ParameterSpec.builder(
                BLOCK_FUN_NAME,
                LambdaTypeName.get(
                    receiver = paramBuilderClassName,
                    returnType = Unit::class.asTypeName()
                )
            ).build()
        )
        .returns(builderClassName)
        .addStatement("this.${param.name} = $paramBuilderName().apply($BLOCK_FUN_NAME).build()")
        .addStatement("return this")
        .build()

    return AutoDslFunctionData(
        funSpec,
        AutoDslImportSpec(
            paramBuilderClassName.packageName,
            paramBuilderClassName.simpleName
        )
    )
}

private class AutoDslFunctionData(
    val funSpec: FunSpec,
    val importData: AutoDslImportSpec
)