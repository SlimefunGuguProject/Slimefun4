# Slimefun 中文版

Looking for English (or Original) Version? [**Click here**](https://github.com/Slimefun/Slimefun4/)

想要直接下载吗? [**单击此处**](https://github.com/SlimefunGuguProject/Slimefun4/blob/master/README.md#floppy_disk-下载-slimefun4)

欢迎加入 QQ 交流群：807302496

下载 [SlimeGlue(粘液胶)](https://github.com/Xzavier0722/SlimeGlue/) 以保证 Slimefun 与其他保护插件的兼容性

### 订阅计划

⚡ 汉化不易, 欢迎支持[爱发电](https://afdian.net/a/nora1ncity)

加入爱发电订阅计划可抢先获得最新版本的 Slimefun 构建，以及是优先技术支持。
注意：**非年度计划用户**可在距上次公开仓库更新后 15 天获取自动同步的源代码，并非闭源。我们十分遵循 GPLv3 协议 :)

了解订阅计划：[订阅计划](https://builds.guizhanss.com/sf-subscription)

![目前版本](https://img.shields.io/github/v/release/SlimefunGuguProject/Slimefun4?include_prereleases)
![构建状态](https://builds.guizhanss.com/SlimefunGuguProject/Slimefun4/master/badge.svg)
[![Issues](https://img.shields.io/github/issues/SlimefunGuguProject/Slimefun4.svg?style=popout)](https://github.com/SlimefunGuguProject/Slimefun4/issues)
![下载数](https://img.shields.io/github/downloads/SlimefunGuguProject/Slimefun4/total)

使用汉化版之后，**禁止**在官方问题追踪器创建新问题!  
如果你执意要这么做, 请在反馈时使用**官方**开发版并且使用**英语**提交问题。  
否则，请在此处创建问题或加群讨论。

Slimefun 是一个致力于提供模组般体验的插件。它提供了你能想到的一切，不管是喷气背包还是魔法祭坛！

Slimefun 让每个玩家可以自行决定在魔法或科技方面发展。  
从魔杖到核反应堆，我们应有尽有。  
除此之外，我们还有魔法祭坛、能源网络甚至物品运输系统。

该项目始于 2013 年，至今仍在发展。
现在已从独立小型插件到发展成一个拥有数千名参与者和百名贡献者的社区。  
目前 Slimefun 添加了超过 **500 种新物品/合成配方**
([查看关于 Slimefun 的历史](https://slimefun-wiki.guizhanss.cn/Slimefun-in-a-nutshell))。

与此同时，Slimefun 还有种类繁多的附属插件可供选择！<br>
打开[附属插件列表](https://slimefun-wiki.guizhanss.cn/Addons)，寻找你想要的附属插件！

### 导航

* **[下载 Slimefun 4](#floppy_disk-下载-slimefun4)**
* **[Discord 服务器](#discord)**
* **[Bug 反馈](https://github.com/SlimefunGuguProject/Slimefun4/issues)**
* **[官方Wiki](https://github.com/Slimefun/Slimefun4/wiki)**
* **[非官方中文 Wiki](https://slimefun-wiki.guizhanss.cn/)**
* **[FAQ](https://slimefun-wiki.guizhanss.cn/FAQ)**

## :floppy_disk: 下载 Slimefun4

(可以查看: [如何安装 Slimefun](https://slimefun-wiki.guizhanss.cn/Installing-Slimefun))

Slimefun 4 可以在[鬼斩构建站](https://builds.guizhanss.com)页面中**免费下载**。

以下是两种版本的区别:

|                      | 测试版 (最新更新)                                                                                | "稳定版"                                                                                 |
|----------------------|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| **支持的 Minecraft 版本** | :video_game: 1.18.X - 1.20.X                                                              | :video_game: 1.16.X - 1.20.X                                                          |
| **Java 版本**          | :computer: **Java 21+**                                                                   | :computer: **Java 17+**                                                               |
| **自动更新系统**           | :heavy_check_mark:                                                                        | :x:                                                                                   |
| **频繁更新**             | :heavy_check_mark:                                                                        | :x:                                                                                   |
| **享有最新内容**           | :heavy_check_mark:                                                                        | :x:                                                                                   |
| **Discord 支持**       | :heavy_check_mark:                                                                        | :x:                                                                                   |
| **Bug 反馈**           | :heavy_check_mark:                                                                        | :x:                                                                                   |
| **在发布前进行测试**         | :x:                                                                                       | :heavy_check_mark:                                                                    |
| **有无更新日志**           | :x:                                                                                       | :memo: **[更新日志(英文)](https://github.com/Slimefun/Slimefun4/blob/master/CHANGELOG.md)** |
| **下载链接**             | :package: **[下载 最新版](https://builds.guizhanss.com/SlimefunGuguProject/Slimefun4/master)** | :package: **[下载 "稳定版"](https://builds.guizhanss.com/SlimefunGuguProject/Slimefun4/release)** |

**! 建议你使用最新的测试版，可以获得最新的内容更新和 Bug 修复！**

## :computer: (开发) 添加依赖

首先添加我们的私有仓库：

Maven:

```xml
<repository>
    <id>gugu-maven-repo</id>
    <url>https://maven.norain.city/releases</url>
</repository>

```

开发版:
```xml
<repository>
    <id>gugu-maven-repo</id>
    <url>https://maven.norain.city/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

Gradle: 在 `build.gradle` 中添加以下内容:

```groovy
repositories {
    maven {
        url "https://maven.norain.city/releases"
    }
}
```

对于开发版：
```groovy
repositories {
    maven {
        url "https://maven.norain.city/snapshots"
    }
}
```

接下来添加 Slimefun 4 作为依赖:
Maven:

```xml
<dependency>
    <groupId>com.github.SlimefunGuguProject</groupId>
    <artifactId>Slimefun4</artifactId>
    <version>DEV-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Gradle:

```groovy
dependencies {
    compileOnly 'com.github.SlimefunGuguProject:Slimefun4:DEV-SNAPSHOT'
}
```


## :computer: 如何编译
要编译 Slimefun4，你必须先安装 [Git](https://git-scm.com/)

然后运行 `git clone https://github.com/SlimefunGuguProject/Slimefun4.git` 克隆仓库到本地。

最后进行编译：
- 如果你是 Windows 系统: `.\mvnw.cmd package`
- 如果你是类 Unix 系统: `.\mvnw package`

## :framed_picture: 截图

那么，Slimefun 看起来是怎样的呢？<br>
我们让 [Discord 服务器](#discord) 中的一些人发给了我们一些截图：
| 反应堆和电力 | 了不起的自动化工厂 | 魔法实验室 |
| :-------------------------------------------: | :--------------------------------------: | :----------------------------------------: |
| ![](https://raw.githubusercontent.com/Slimefun/Slimefun-Wiki/master/images/showcase1.png) | ![](https://raw.githubusercontent.com/Slimefun/Slimefun-Wiki/master/images/showcase6.png) | ![](https://raw.githubusercontent.com/Slimefun/Slimefun-Wiki/master/images/showcase5.png) |
| *截图由 HamtaBot#0001 提供* | *截图由 Piͭxͪeͤl (mnb)#5049 提供* | *截图由 Kilaruna#4981 提供* |
| ![](https://raw.githubusercontent.com/Slimefun/Slimefun-Wiki/master/images/showcase4.png) | ![](https://raw.githubusercontent.com/Slimefun/Slimefun-Wiki/master/images/showcase3.png) | ![](https://raw.githubusercontent.com/Slimefun/Slimefun-Wiki/master/images/showcase2.png) |
| *截图由 GalaxyKat11#3816 提供* | *截图由 TamThan#7987 提供* | *截图由 Kilaruna#4981 提供* |

## :headphones: 社区

### 汉化版 QQ 群

汉化版交流 QQ 群：807302496 (Slimefun 汉化版交流)
附属交流 QQ 群：205679802 (Slimefun 附属交流，以及汉化组插件更新通知)

### KOOK 交流频道

欢迎加入 KOOK [粘液科技简中社区](https://kook.top/oqZwh8) 服务器

### 官方 Discord

你可以在 Discord 上找到 Slimefun 的社区服务器，并与 **超过 7000** 个来自全世界的用户进行交流。  
单击下面的徽标加入 Discord 服务器反馈问题和提出意见，或者讨论关于此插件的内容。  
Slimefun 官方经常会举办一些社区活动，加入我们了解更多。  
**注意**：Slimefun 官方在 Discord 服务器 **不**
接受任何形式的问题反馈，请使用 [问题追踪器](https://github.com/SlimefunGuguProject/Slimefun4/issues) 反馈问题！

在加入前请先了解官方 Discord 服务器[重要的规则](https://github.com/Slimefun/Slimefun4/wiki/Discord-Rules)。  
不遵守以上规则的人可能会被从服务器中踢出甚至封禁。

<p align="center">
  <a href="https://discord.gg/slimefun">
    <img src="https://discordapp.com/api/guilds/565557184348422174/widget.png?style=banner3" alt="Discord Invite"/>
  </a>
</p>

## :open_book: Wiki

Slimefun 有一个为新玩家准备的 (详细且经常维护的 - *咳咳*) Wiki，
你也可以考虑为 Wiki 的编写献出一份力量。
官方 Wiki：https://github.com/Slimefun/Slimefun4/wiki  
非官方中文 Wiki：https://slimefun-wiki.guizhanss.cn/

#### :star: 有用的文章 (中文)

* [什么是 Slimefun?](https://slimefun-wiki.guizhanss.cn/Slimefun-in-a-nutshell)
* [如何安装 Slimefun](https://slimefun-wiki.guizhanss.cn/Installing-Slimefun)
* [Slimefun 4 扩展列表](https://slimefun-wiki.guizhanss.cn/Addons)
* [Slimefun 4 扩展编写教程](https://slimefun-wiki.guizhanss.cn/Developer-Guide)
* [开始使用](https://slimefun-wiki.guizhanss.cn/Getting-Started)
* [常见问题](https://slimefun-wiki.guizhanss.cn/FAQ)
* [使用中的常见问题](https://slimefun-wiki.guizhanss.cn/Common-Issues)
* [帮助我们扩展 Wiki!](https://slimefun-wiki.guizhanss.cn/Expanding-the-Wiki)
* [帮助我们翻译 Slimefun!](https://slimefun-wiki.guizhanss.cn/Translating-Slimefun)

这个 Wiki 由 @ybw0014 进行维护，如果你发现有文章缺失，请在 Wiki 的 Issues 页面汇报。

## :handshake: 对项目作出贡献

Slimefun 4 是一个以 [GNU GPLv3 协议](https://github.com/Slimefun/Slimefun4/blob/master/LICENSE) 开源的项目。
已经有超过 100 人对这个项目做出了贡献，这些人真是太棒了。  
我们鼓励通过提交 PR 的方式为 Slimefun 4 做出贡献，你的贡献将会使插件保持活力 <3。

## :exclamation: 免责声明

Slimefun4 使用多种系统收集使用数据，以及使用自动更新让你知晓关于这个项目的最新信息。
我们不会收集从你那里获取的任何个人信息，但是有些服务可能仍会收集或下载某种形式的数据。

你可以在任何时候选择退出数据收集和关闭自动更新

<details>
  <summary>自动更新</summary>

Slimefun 汉化版 使用 Github API + GuizhanBuild API 以检测和下载更新。  
这一功能被默认启用，但你可以在 `/plugins/Slimefun/config.yml` 里选择关闭。  
我们强烈推荐你使用自动更新，以确保你能获得最新功能/修复的更新。

</details>

<details>
  <summary>指标与统计</summary>

Slimefun4 使用 [bStats](https://bstats.org/plugin/bukkit/Slimefun/4574) 收集关于插件的匿名信息。  
这些仅是为了统计目的，因为我们对服务器和玩家如何使用插件很感兴趣。  
所有公开的数据均为匿名，我们绝对无法根据上报的数据追溯到某个服务器或是某个玩家。  
所有收集的数据均可公开访问: https://bstats.org/plugin/bukkit/Slimefun/4574

你也可以在 `/plugins/bStats/config.yml` 下关闭数据收集。  
了解更多请查看 [bStats 隐私政策](https://bstats.org/privacy-policy)。

</details>

<details>
  <summary>GitHub 整合</summary>

Slimefun4 使用 [GitHub API](https://api.github.com/) 收集关于此开源项目的使用数据。  
请放心，你的 Minecraft 服务器信息不会被发送到 Github。

这些信息包括但不限于

* 协作者列表，TA 们的用户名和个人主页链接 (来自仓库`Slimefun/Slimefun4`、`Slimefun/Slimefun-Wiki`和`Slimefun/Resourcepack`)
* 仓库中开启的问题的数量
* 仓库中待定合并请求的数量
* 仓库的 Star 数量
* 仓库的分支数量
* 仓库的代码大小
* 仓库中代码上次提交的日期

  </details>

另外，插件还使用了 [textures.minecraft.net](https://www.minecraft.net/en-us) 以获取协作者的 Minecraft 皮肤。  
请注意：Slimefun 与 `Mojang Studios` 或 Minecraft 无关。
