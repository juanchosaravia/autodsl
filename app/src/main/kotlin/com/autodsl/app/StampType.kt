package com.autodsl.app

import com.autodsl.annotation.AutoDsl

internal sealed class StampType
@AutoDsl
internal class GoldStamp(val price: Double): StampType()
internal object MetalStamp : StampType()
internal object BronzeStamp : StampType()