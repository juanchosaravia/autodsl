# AutoDsl in Android

This sample App showcase how to create a DSL around `AnimatorSet` and `Animator` 
to perform animations over Views in a more expressive way:

```kotlin
sequence {
    +together {
        +TranslateX(0f, 150f)
        +TranslateY(0f, -150f)
    }
    +sequence {
        +translateX {
            from = -150f
            to = 150f
        }
        +TranslateY(150f, 0f)
    }
    // ...
    
}.runOn(view)
```

It will concatenate different `Animators` and at the end will execute them in the given order.

## Animation
![Anim not found](https://github.com/juanchosaravia/autodsl/blob/master/samples/android-autodsl/resources/showcase_anim.gif?raw=true)

## License
        
    Copyright 2018 Juan Ignacio Saravia
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.  