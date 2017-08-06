# 版本 1.3
- 采用系统组件 MediaBrowserServiceCompat 作为音频服务，提高应用兼容性
- 采用系统组件 MediaStyle 的 Notification
- 可通过媒体按钮来唤醒媒体服务
- 在媒体服务重新启动后恢复上一次的媒体数据
- 多处逻辑异常修正
# 版本 1.2
- 修改所有封面加载逻辑，提高封面加载效率
- 歌曲数据已全部基于 MediaMetadata ，为之后的服务更改做准备
- 播放界面背景优化
- 歌曲检索优化，提高歌曲检索效率
- 移除大量冗余代码
- 修改自定义封面加载逻辑 (目前还有些问题，详见Issues：https://github.com/ocwvar/DarkPurple/issues/22)
# 版本 1.1
- 添加 录音权限 请求动作
- 移除BASS、MediaPlayer播放方案
- 移除所有相关资源
