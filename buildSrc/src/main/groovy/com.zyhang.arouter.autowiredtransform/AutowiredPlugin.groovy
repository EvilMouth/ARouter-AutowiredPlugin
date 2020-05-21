package com.zyhang.arouter.autowiredtransform

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AutowiredPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.getByType(BaseExtension)
                .registerTransform(new AutowiredTransform(project))
    }
}