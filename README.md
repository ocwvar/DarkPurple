![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/logo.png) 
 
# 应用下载 1.4.2 1708241650 ![](https://travis-ci.org/ocwvar/DarkPurple.svg?branch=dev)
[<img src="https://github.com/ocwvar/Project-common-image-resources/blob/master/Github%20Badge/Github_badge_350.png">](https://github.com/ocwvar/DarkPurple/releases/download/1.4.2/app-release.apk)
[<img src="https://github.com/ocwvar/Project-common-image-resources/blob/master/Google%20Badge/Google_badge_350.png">](https://play.google.com/store/apps/details?id=com.ocwvar.darkpurple)

更新日志：https://github.com/ocwvar/DarkPurple/blob/dev/changelog.md

隐私条例：https://github.com/ocwvar/DarkPurple/blob/dev/Privacy%20Policy.md

# 介绍
#### 系统支持：Android 5.0 ~ Android 7.1.2
#### 音频媒体支持：MP3、WAV、FLAC、OGG


这是一款基于Google EXO引擎独立开发的音乐播放器。

- 可通过媒体按钮和 Notification 来控制媒体播放
- 停止播放后，可通过滑动移除 Notification 来关闭应用
- 拔出耳机自动暂停 , 插入耳机自动恢复播放
- 用户可以通过手动输入路径或从浏览器中限定音乐文件夹或扫描所有音频
- 提供可编辑播放列表功能
- 可根据音频长度进行过滤
- 可根据歌曲名字或歌曲添加时间进行排序
- 在应用被销毁后，可以通过耳机线控来重新启动
- 可自定义均衡器配置
- UI 设计采用 Material Design
- 可在线搜索设置封面数据 (请阅读下方说明)
- 有两种频谱动画 , 您也可以自定义频谱动画的柱状粗细与颜色 (下方有预览图) 频谱元素有柱状、点状、线状，可以单独开启或关闭
- **可以进行云端音频的上传、下载、删除 WebAPI服务器项目地址：https://github.com/ocwvar/DarkPurpleService**

## 关于封面获取功能
封面数据源自:coverbox.sinaapp.com , DarkPurple 的封面获取功能就是解析自此网站

CoverBox 的作者为 Henry Hu

封面获取功频繁使用的时候可能会出现无法获取到封面资源的情况,这是因为网址可能左右流量访问限制问题,等待一段时间后即可

## 关于均衡器调节
本APP使用的是系统的均衡器API，经测试发现在不同设备下可以调节的 Band(音频带宽) 范围数量有所不同。大多数设备为 5 个，部分三星设备为 13 个。

## 关于Android 8.0 支持
在虚拟机下的 Android 8.0 版本中，Google v4兼容包(v26.0.0-alpha1)中的 MediaSessionCompat 会产生异常。
在新版本发布后再进行相应的适配工作。

## 开发计划
- √ 优化播放界面和主框架页面头部布局严重 Overdraw 的问题
- → 将逻辑复杂的页面改为MVP设计结构：播放界面√  主框架页面√  主播放列表页面 →
- x  将Picasso更换为Glide，以节省内存和提升性能
- x  修正Stop相关逻辑
- x  文件系统歌曲扫描完善，扫描子目录并提供按目录选择歌曲，包括Cover.jpg的支持

# 截图展示 (2017/8/24 16：42)
##### *截图无法展示所有应用的细节*

### 播放界面<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playing_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playing_2.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playing_3.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playing_4.png)

### 主界面<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/main_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/main_2.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/main_3.png)

### 播放列表<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playlist_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playlist_2.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playlist_3.png)

## Notification 样式(Android 7.1.2)<p></p>
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/notification_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/notification_2.png)

### 设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_2.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_3.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_4.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_5.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_6.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_7.png)

### 扫描目录设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/folder_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/folder_2.png)

### 在线浏览下载<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/cloud_list.jpg)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/cloud_download.jpg)

### 封面下载<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/cover.jpg)

### 均衡器设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/eq.png)

### 频谱动画 (风格1)<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/sp1.gif)

### 频谱动画 (风格2)<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/sp2.gif)

---


