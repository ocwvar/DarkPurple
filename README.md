![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/logo-nv.png) 
![](https://travis-ci.org/ocwvar/DarkPurple.svg?branch=master) 

#### 支持：Android 4.2.2 ~ Android 7.1.2
#### 第二版开发计划
第二版暂时将联网部分功能去除，并去除大量不必要的细节和功能，将原先部分质量较差的代码用kotlin重构。

(17.07.23) 经过考虑，没有必要在一个APP里使用多个音频播放方案，所以只保留EXO音频方案，现已将BASS以及MediaPlayer方案去除。
目前暂不可以使用均衡器。以及为了更好地兼容各种OS，决定将自己实现的一套音频服务改用 MediaBrowserService。 改动较大，时间需要比较久。。

---
- 添加Google EXO Player2基础功能 √
- 新的歌曲选择主界面 √
- 新的播放列表界面 √
- 用户选项界面 √
- EXO频谱动画 √
- 将现有的音频服务改用 MediaBrowserService →
- EXO均衡器功能 X

#### 功能介绍

---
- 可通过耳机按钮和状态栏Notification来控制音乐的播放 
- 拔出耳机自动暂停 , 插入耳机自动恢复播放
- 用户可以通过手动输入路径或从浏览器中限定音乐文件夹或扫描所有音频
- 提供可编辑播放列表功能
- 可根据音频长度进行过滤
- 可根据歌曲名字或歌曲添加时间进行排序
- UI 设计采用 Material Design
- 可在线搜索设置封面数据 (请阅读下方说明)
- 有两种频谱动画 , 您也可以自定义频谱动画的柱状粗细与颜色 (下方有预览图)
- 可在表格样式和列表样式之间进行切换
- 可自定义APP的界面颜色配置
- **可以进行云端音频的上传、下载、删除 WebAPI服务器项目地址：https://github.com/ocwvar/DarkPurpleService**

##### 关于封面获取功能
封面数据源自:coverbox.sinaapp.com , DarkPurple 的封面获取功能就是解析自此网站

CoverBox 的作者为 Henry Hu

封面获取功频繁使用的时候可能会出现无法获取到封面资源的情况,这是因为网址可能左右流量访问限制问题,等待一段时间后即可

---

##### APP截图 (2017/6/11 01:04)
##### *截图无法展示所有应用的细节*

### 主界面<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/musiclist_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/musiclist_2.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/musiclist_3.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/musiclist_4.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/musiclist_5.png)

### 播放列表<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playlist_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playlist_2.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playlist_detail.jpg)

### 播放界面<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playing_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/playing_2.png)

### 基础设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_1.png)
![](https://github.com/ocwvar/DarkPurple/blob/dev/app/showcase/screenshots/setting_2.png)

### 扫描目录设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/screenshots/scan.jpg)
![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/screenshots/scan_2.jpg)

### 封面下载<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/screenshots/cover.jpg)

### 均衡器设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/screenshots/eq.jpg)

### 主题设置功能<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/screenshots/setting_theme.jpg)

### 频谱设置<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/screenshots/setting_sp.jpg)

### 频谱动画 (风格1)<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/sp.gif)

### 频谱动画 (风格2)<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/sp2.gif)

### 频谱动画 (风格3)<p></p>

![](https://github.com/ocwvar/DarkPurple/blob/master/app/showcase/sp3.gif)
---


