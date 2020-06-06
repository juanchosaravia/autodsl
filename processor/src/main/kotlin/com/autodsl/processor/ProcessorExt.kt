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
import com.squareup.kotlinpoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.Diagnostic

internal fun ProcessingEnvironment.getClassName(element: Element, className: String): ClassName {
    return ClassName(elementUtils.getPackageOf(element).toString(), className)
}

internal fun ProcessingEnvironment.error(e: ProcessingException) {
    this.error(e.element, e.message ?: "There was an error processing this element.")
}

internal fun ProcessingEnvironment.error(e: Element, msg: String, vararg args: String) {
    messager?.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e)
}

internal fun ProcessingEnvironment.getGeneratedSourcesRoot(): String {
    return this.options[AutoDslProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        ?: throw IllegalStateException("No source root for generated file")
}

internal fun AutoDsl?.getDslNameOrDefault(defaultString: String): String {
    return if (this == null || dslName.isEmpty()) {
        defaultString
    } else {
        dslName
    }
}

internal fun TypeName.asNullableIf(condition: Boolean): TypeName {
    return if (condition) copy(nullable = true) else this
}