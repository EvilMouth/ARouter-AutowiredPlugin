# ARouter-AutowiredTransform

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
