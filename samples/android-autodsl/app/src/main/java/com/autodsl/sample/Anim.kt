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
package com.autodsl.sample

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import com.autodsl.annotation.AutoDsl
import com.autodsl.annotation.AutoDslCollection

/**
 * DSL for Animations.
 *
 * @author juan.saravia
 */
interface Anim {
    fun createAnimator(): Animator

    fun runOn(view: View) {
        createAnimator().apply {
            setTarget(view)
            start()
        }
    }
}

@AutoDsl(dslName = "sequence")
class AnimSequence(
    @AutoDslCollection(concreteType = ArrayList::class, inline = true)
    val anim: List<Anim>
) : Anim {

    override fun createAnimator(): Animator {
        return AnimatorSet().apply {
            playSequentially(anim.map { it.createAnimator() })
        }
    }
}

@AutoDsl(dslName = "together")
class AnimTogether(
    @AutoDslCollection(concreteType = ArrayList::class, inline = true)
    val anim: List<Anim>
) : Anim {

    override fun createAnimator(): Animator {
        return AnimatorSet().apply {
            playTogether(anim.map { it.createAnimator() })
        }
    }
}

@AutoDsl
class TranslateX(from: Float, to: Float) : TranslateAnim("translationX", from, to)

class TranslateY(from: Float, to: Float) : TranslateAnim("translationY", from, to)

open class TranslateAnim(
    private val propertyName: String,
    private val from: Float,
    private val to: Float
) : Anim {

    override fun createAnimator(): Animator {
        return ObjectAnimator().apply {
            propertyName = this@TranslateAnim.propertyName
            setFloatValues(from, to)
        }
    }
}