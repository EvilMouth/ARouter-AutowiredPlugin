package com.zyhang.arouter.autowiredtransform

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class AutowiredPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def isApp = project.plugins.hasPlugin(AppPlugin)
        if (isApp) {
            def android = project.extensions.getByType(AppExtension)
            android.registerTransform(new AutowiredTransform(android.defaultConfig.applicationId))
        }
    }
}