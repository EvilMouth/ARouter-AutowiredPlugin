package com.zyhang.arouter.autowired_plugin

import com.android.build.gradle.AppExtension
import com.ss.android.ugc.bytex.common.BaseContext
import org.gradle.api.Project

/**
 * Created by zyhang on 2020/11/24.09:44
 */
class AutowiredContext(project: Project?, android: AppExtension?, extension: AutowiredExtension?) :
    BaseContext<AutowiredExtension>(project, android, extension)