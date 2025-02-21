# Slimefun Folia 中文版

原汉化版: https://github.com/SlimefunGuguProject/Slimefun4

本项目仅对Folia进行适配以及加入相关调试工具，不额外增加功能，也不修复bug。**内容相关的修改都应该在上游。**

本项目不提供最终构件，请自行编译。该项目不适合专业开发者之外的用户使用，不为非开发者提供任何支持。

# 技术性说明

1. 现在所有的Slimefun的Block tick均在region tick thread中执行，而不是异步。这可以保证不同地区的粘液刻不会互相影响。主Loop仅做Schedule task。但是为每个tick region建立一个Daemon Async Thread是更好的，未来可能会实现?
2. 单例的Slimefun Profiler完全不适用于Folia，目前的数据完全不正确，请勿使用。
3. 目前全局替换了线程安全容器，但是部分地方是不必要的，并且可能会产生激烈的竞争问题，这需要在未来精确识别。

# 如何编译

由于我没有maven仓库，所以你需要把我修改的Dependency全都publish到local maven repo。

## dough-folia
[https://github.com/KujouMolean/dough-folia](https://github.com/KujouMolean/dough-folia)

`./gradlew build && ./gradlew publishToMavenLocal`

## FoliaAdapter
[https://github.com/KujouMolean/FoliaAdapter](https://github.com/KujouMolean/FoliaAdapter)
`./gradlew build && ./gradlew publishToMavenLocal`

## Slimefun-Folia
`./gradlew build`
