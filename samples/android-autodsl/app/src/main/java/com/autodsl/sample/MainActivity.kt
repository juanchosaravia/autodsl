package com.autodsl.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView.setOnClickListener {
            sequence {
                // up right
                +together {
                    +TranslateX(0f, 150f)
                    +TranslateY(0f, -150f)
                }
                // left
                +TranslateX(150f, -150f)
                // down right
                +together {
                    +translateX {
                        from = -150f
                        to = 150f
                    }
                    +TranslateY(-150f, 150f)
                }
                // left
                +TranslateX(150f, -150f)
                // center
                +together {
                    +TranslateX(-150f, 0f)
                    +TranslateY(150f, 0f)
                }
            }.runOn(it)
        }
    }
}