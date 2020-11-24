# ARouter-AutowiredTransform

[ ![Download](https://api.bintray.com/packages/zyhang/maven/ARouter-AutowiredPlugin/images/download.svg) ](https://bintray.com/zyhang/maven/ARouter-AutowiredPlugin/_latestVersion)

## 目的
移除`ARouter.getInstance().inject(this);`的使用，减少反射

## 原理
利用`ASM`自动往使用了`@Autowired`的`Activity`或`Fragment`的`onCreate(Bundle)`方法块注入字节码
``` java
@Override
protected void onCreate(Bundle bundle) {
    new MainFragment$$ARouter$$Autowired().inject(this); // 此行由插件自动生成
    super.onCreate(bundle);
}
```

## 使用

本插件已依赖[bytex]()开发，可以直接依赖接入

```groovy
// in root build.gradle
repositories {
    maven {
        url "https://dl.bintray.com/zyhang/maven"
    }
}
dependencies {
    classpath "maven.evilmouth.arouter:autowired-plugin:<latest-version>"
}

// in app build.gradle
apply plugin: 'com.zyhang.arouter.autowired_plugin'
AutowiredExtension {
    enableInDebug true
}
```
