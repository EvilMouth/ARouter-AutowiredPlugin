//package com.zyhang.arouter.autowired_plugin
//
//import com.android.build.gradle.AppExtension
//import com.ss.android.ugc.bytex.common.CommonPlugin
//import org.gradle.api.Project
//
///**
// * Created by zyhang on 2020/11/23.16:35
// */
//class AutowiredPlugin : CommonPlugin<AutowiredExtension, AutowiredContext>() {
//    override fun getContext(
//        project: Project?,
//        android: AppExtension?,
//        extension: AutowiredExtension?
//    ): AutowiredContext = AutowiredContext(project, android, extension)
//}