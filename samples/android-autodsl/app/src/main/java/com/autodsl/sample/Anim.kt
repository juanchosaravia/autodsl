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