# 贡献指南

在对 Slimefun 汉化版进行代码前，必须先阅读此贡献指南。

# 设置环境

我们提供了一个自动化格式检查系统，请使用 `mvn install` 进行初始化。

本项目已经提供 `.editorconfig` 用于控制代码样式。如果你有自己的代码样式风格，请在对本仓库进行贡献前切换为当前仓库的风格配置。

# 设置分支

在开始你的贡献之前，请确认你的代码是基于 `dev` 分支进行的。

# 提交信息规范

本项目**强制使用** [约定式提交](https://www.conventionalcommits.org/zh-hans/v1.0.0/) 的提交信息规范。

> 简单来说, 你的提交信息需要包含以下内容:
> 
> <类型>[可选 范围]: <描述>
> 
> 例如一个添加了新功能的提交应为 feat(item): add new item to Slimefun

如果你提交的代码中解决或处理了 Issue 中的问题，请你在主提交消息外显式声明。

> 如 resolves #114514, fix #114514 等

如果是修复请在主提交消息上声明，不必重复声明。

我们支持的类型前缀正则如下：`(feat(ure)?|fix|docs|style|refactor|ci|chore|perf|build|test|revert|trans)`

另外的, 如果是与翻译相关的提交，类型应为 trans。

# 代码规范

**!! 本项目使用 4 空格缩进 !!**

请不要过度缩减代码长度, 空格少了 Slimefun 也不会因此跑得更快.

我们使用了 Spotless 作为代码格式化工具，在提交前你**必须**使用 `mvn spotless:check spotless:apply` 来自动格式化你的代码，否则将会被格式检查器拦截 PR。

# 提交代码类型

你提交的代码可以是修复、新增内容和 API。

下游代码现在支持提交 API 相关代码，开发者们可以通过 jitpack 依赖汉化版的 Slimefun。