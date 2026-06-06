# LLM 场景描述（Scene Description）

> 状态：已实现  
> 来源：移植 AuraFlow `data/scenedescription/` 模块  
> **Android 对接见 [API.md](./API.md)**

## 1. 目标

服务端调用阿里云百炼 `qwen3-omni-flash`，将**视频 + 环境音**与元数据（位置、正在播放的音乐等）组合为 Prompt，生成：

| 输出 | 写入字段 | App 使用 |
|------|----------|----------|
| 主题色 Hex | `records.color` | 列表卡片背景色 |
| 中文场景描述（252～560 字） | `records.summary` | 列表/详情文案 |

API Key 仅存在于服务端 `.env`（`LLM_API_KEY`），App 不直连 DashScope。

## 2. 调用路径

### 2.1 推荐：同步 API（新 App）

```
POST /media/audio  → audio_media_id
POST /media/video  → video_media_id
POST /scene/generate
    → SceneGenerateService.generate()
    → SceneDescriptionService.generate_color_scene()
    → 返回 color + description（HTTP 200，同步，≤120s）
```

App 在步骤 ③ 等待 HTTP 响应即可，**无需轮询** Worker。

### 2.2 兼容：异步 Worker（旧 App）

```
POST /records
POST /records/{id}/upload?finalize=true
    → tasks 表 → Worker → SummaryService
    → App 轮询 GET /records/{id} 直到 status=completed
```

旧流程不返回 `color`（除非后续扩展）。

## 3. 客户端需提交的元数据

与 AuraFlow `SceneCapturePayload` 对齐，在 `POST /scene/generate` JSON 中传递：

| 字段 | 说明 |
|------|------|
| `captured_at_ms` | 采集时间 Unix 毫秒 |
| `location` / `location_encrypted` | GPS（生产须加密） |
| `now_playing` | 当前播放音乐 |
| `video_meta` / `audio_meta` | 录制时长、是否成功 |
| `capture_errors` | 非致命采集错误 |

完整 JSON 样例见 [API.md §5.4](./API.md#54-步骤--生成场景核心)。

## 4. 服务端处理链

```
读取 media_assets 本地文件
  → 复制到 records/{record_id}/
  → ffmpeg 缩略图
  → SceneMediaEncoder（VIDEO_FIRST，音视频 Base64）
  → ScenePromptBuilder（system + user 多模态 content）
  → DashScopeOmniClient（SSE 流式）
  → 解析 JSON：{ "color": "#...", "description": "..." }
  → UPDATE records SET color, summary, status=completed
```

失败时：`status=failed`，HTTP 502 `SCENE_GENERATION_FAILED`。

## 5. 配置（.env）

| 变量 | 默认 |
|------|------|
| `LLM_MODEL` | `qwen3-omni-flash` |
| `LLM_MEDIA_PREFERENCE` | `VIDEO_FIRST` |
| `LLM_MAX_RAW_MEDIA_BYTES` | `10485760` |
| `LLM_MAX_DESCRIPTION_CHARS` | `560` |
| `LLM_POSTER_MAX_EDGE_PX` | `1024` |
| `LLM_READ_TIMEOUT_SEC` | `120` |

## 6. 模块映射

| AuraFlow (Kotlin) | AuraAppServer (Python) |
|-------------------|------------------------|
| `SceneDescriptionPromptBuilder` | `scene_prompt_builder.py` |
| `SceneMediaEncoder` | `scene_media_encoder.py` |
| `DashScopeSceneDescriptionRemote` | `dashscope_omni_client.py` |
| `SceneDescriptionRepository` | `scene_description_service.py` |

## 7. 相关文档

- [API.md](./API.md) — Android 三步流程与样例
- [TECH_DESIGN.md](./TECH_DESIGN.md) — 架构
- [SERVER_CONFIG.md](./SERVER_CONFIG.md) — `LLM_API_KEY` 配置
