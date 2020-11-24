package com.zyhang.arouter.autowired_plugin

import com.ss.android.ugc.bytex.common.BaseExtension

/**
 * Created by zyhang on 2020/11/24.09:44
 */
open class AutowiredExtension : BaseExtension() {
    override fun getName(): String = "AutowiredExtension"

    private var consumesFeatureJars = false

    fun consumesFeatureJars(consumesFeatureJars: Boolean) {
        this.consumesFeatureJars = consumesFeatureJars
    }

    fun isConsumesFeatureJars() = consumesFeatureJars
}