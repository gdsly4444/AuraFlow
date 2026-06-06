# AuraAppServer 服务配置文档

> 版本：v1.2  
> 日期：2026-06-07  
> 适用阶段：内测  
> 部署步骤（Ubuntu 24.04 · 阿里云）：[DEPLOY.md](./DEPLOY.md)  
> **Android 接口与传参样例：[API.md](./API.md)**  
> 关联文档：[TECH_DESIGN.md](./TECH_DESIGN.md)

本文档说明服务端全部配置项、**Token / Secret 的生成方法**、**服务器与 App 的配置流程**，以及凭证轮换与验证方法。

---

## 1. 配置总览

```mermaid
flowchart LR
    A["① 生成凭证\nToken + Secret"] --> B["② 写入服务器 .env"]
    B --> C["③ 内置到 Aura App"]
    C --> D["④ 重启服务"]
    D --> E["⑤ 验证请求"]
```

| 步骤 | 操作者 | 产出 |
|------|--------|------|
| ① 生成凭证 | 开发者（你） | `API_TOKEN`、`API_SECRET` |
| ② 配置服务器 | 开发者 | `/opt/aura/.env` |
| ③ 配置 App | 开发者 | App 内嵌 Token + Secret + 包名 |
| ④ 启动服务 | 运维 / 开发者 | API + Worker 运行 |
| ⑤ 验证 | 开发者 | 确认 401/403/200 行为正确 |

---

## 2. Token 是什么？怎么「获取」？

### 2.1 重要说明

内测阶段 **没有** 「App 调接口申请 Token」的公开 API。

Token 是**预共享凭证（Pre-shared Credential）**：

- 由**开发者本地生成**
- **写入服务器** `.env`
- **内置到自有 Aura App**
- 服务端只做**比对**，不对外发放

这样可避免：任意人调用注册接口 → 拿到 Token → 访问你的服务。

### 2.2 凭证组成

| 名称 | 环境变量 | 谁持有 | 是否在网络上传 |
|------|----------|--------|----------------|
| API Token | `API_TOKEN` | 服务器 + App | 是（`Authorization` 头） |
| API Secret | `API_SECRET` | 服务器 + App | **否**（仅用于本地算签名） |
| 合法包名 | `ALLOWED_PACKAGE_NAME` | 服务器 + App | 是（`X-App-Package` 头） |

三者必须**完全一致**，请求才能通过。

### 2.3 与用户登录 Token 的区别

| | 内测 API Token（本文档） | 用户登录 Token（未实现） |
|--|--------------------------|--------------------------|
| 证明对象 | 「是我们的 App」 | 「是哪个用户」 |
| 获取方式 | 开发者预生成 | 用户登录后下发 |
| 配置位置 | `.env` + App 内置 | 运行时动态获取 |
| 用户区分 | 业务字段 `user_id` | JWT / Session |

---

## 3. Token / Secret 生成方法

### 3.1 方法一：项目脚本（推荐）

编码完成后，仓库内提供：

```bash
cd /path/to/AuraAppServer
bash scripts/generate_credentials.sh
```

输出示例：

```
==========================================
AuraAppServer 凭证（请妥善保存，仅显示一次）
==========================================

ALLOWED_PACKAGE_NAME=com.example.aura   ← 请改为你的正式包名

API_TOKEN=K7xR2mN9pQ4vL8wY3zA6bC1dE5fG0hJ
API_SECRET=9sT2uV5wX8yZ1aB4cD7eF0gH3jK6lM9nP2qR5sT8uV1wX4yZ7aB0cD3eF6gH

==========================================
下一步：
  1. 将上述内容写入服务器 .env
  2. 将 Token / Secret 内置到 Android App
  3. 确认 App 的 applicationId 与 ALLOWED_PACKAGE_NAME 一致
==========================================
```

### 3.2 方法二：Python 一行命令

```bash
# 生成 API_TOKEN（32 字节，URL-safe）
python3 -c "import secrets; print('API_TOKEN=' + secrets.token_urlsafe(32))"

# 生成 API_SECRET（48 字节，URL-safe）
python3 -c "import secrets; print('API_SECRET=' + secrets.token_urlsafe(48))"
```

### 3.3 方法三：OpenSSL

```bash
# 生成 API_TOKEN
echo "API_TOKEN=$(openssl rand -base64 32 | tr -d '/+=' | head -c 43)"

# 生成 API_SECRET
echo "API_SECRET=$(openssl rand -base64 48 | tr -d '/+=' | head -c 64)"
```

### 3.4 生成规范

| 项 | 要求 |
|----|------|
| 随机性 | 使用密码学安全随机源（`secrets` / `openssl rand`） |
| 长度 | Token ≥ 32 字节；Secret ≥ 48 字节 |
| 字符集 | 推荐 URL-safe Base64（字母数字 + `-_`） |
| 禁止 | 不要用 `123456`、`aura-token` 等可猜测字符串 |
| 存储 | 生成后立即写入密码管理器或离线记录，**不要进 Git** |

---

## 4. 服务器 `.env` 完整配置

### 4.1 配置文件位置

```text
/opt/aura/AuraAppServer/.env        # 推荐：生产目录
/path/to/AuraAppServer/.env         # 开发 / 测试
```

权限设置：

```bash
chmod 600 .env
chown aura:aura .env    # 如有专用用户
```

### 4.2 配置模板

复制仓库根目录 `.env.example`：

```bash
cp .env.example .env
vim .env
```

完整示例：

```env
# ── 应用 ──────────────────────────────────────
APP_ENV=production
APP_HOST=0.0.0.0
APP_PORT=8000
LOG_LEVEL=info

# ── 客户端鉴权（必填）────────────────────────
# Android applicationId，必须与实际 App 完全一致
ALLOWED_PACKAGE_NAME=com.example.aura

# 内测 Token（generate_credentials.sh 生成）
API_TOKEN=K7xR2mN9pQ4vL8wY3zA6bC1dE5fG0hJ

# 签名密钥（generate_credentials.sh 生成，不在网络传输）
API_SECRET=9sT2uV5wX8yZ1aB4cD7eF0gH3jK6lM9nP2qR5sT8uV1wX4yZ7aB0cD3eF6gH

# ── 数据库 ────────────────────────────────────
DATABASE_URL=postgresql+asyncpg://aura:your_db_password@127.0.0.1:5432/aura_db

# ── 媒体存储 ──────────────────────────────────
MEDIA_ROOT=/data/media
STORAGE_QUOTA_MB=20480
MIN_FREE_GB=10
MAX_UPLOAD_MB=30

# ── LLM（百炼 DashScope）──────────────────────
LLM_API_KEY=sk-your-llm-key
LLM_MODEL=qwen3-omni-flash
LLM_MEDIA_PREFERENCE=VIDEO_FIRST
LLM_MAX_RAW_MEDIA_BYTES=10485760
LLM_MAX_DESCRIPTION_CHARS=560
LLM_READ_TIMEOUT_SEC=120

# ── 安全 ──────────────────────────────────────
TIMESTAMP_TOLERANCE_SEC=300
NONCE_TTL_SEC=600
RATE_LIMIT_PER_MIN=60
```

### 4.3 配置项说明

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `ALLOWED_PACKAGE_NAME` | ✅ | — | 唯一合法 Android 包名 |
| `API_TOKEN` | ✅ | — | 客户端 Bearer Token |
| `API_SECRET` | ✅ | — | HMAC 签名密钥 |
| `DATABASE_URL` | ✅ | — | PostgreSQL 连接串 |
| `MEDIA_ROOT` | ✅ | `/data/media` | 媒体文件根目录 |
| `STORAGE_QUOTA_MB` | ❌ | 按磁盘计算 | 全局媒体配额（MB） |
| `MIN_FREE_GB` | ❌ | `10` | 磁盘剩余低于此值拒绝上传 |
| `MAX_UPLOAD_MB` | ❌ | `30` | 单文件上传上限 |
| `LLM_API_KEY` | ✅ | — | 百炼 DashScope API 密钥 |
| `LLM_MODEL` | ❌ | `qwen3-omni-flash` | 多模态场景描述模型 |
| `TIMESTAMP_TOLERANCE_SEC` | ❌ | `300` | 时间戳允许偏差（秒） |
| `NONCE_TTL_SEC` | ❌ | `600` | nonce 去重缓存时间 |
| `RATE_LIMIT_PER_MIN` | ❌ | `60` | 每 IP 每分钟请求上限 |

---

## 5. 服务器配置流程（ step-by-step ）

### 5.1 准备目录

```bash
sudo mkdir -p /opt/aura /data/media
sudo useradd -r -s /bin/false aura 2>/dev/null || true
sudo chown -R aura:aura /opt/aura /data/media
```

### 5.2 部署代码

```bash
cd /opt/aura
git clone <your-repo-url> AuraAppServer
cd AuraAppServer
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 5.3 生成并写入凭证

```bash
# 生成
bash scripts/generate_credentials.sh

# 创建 .env 并填入（含数据库、LLM 等其余配置）
cp .env.example .env
vim .env

chmod 600 .env
```

### 5.4 初始化数据库

```bash
# 创建 PostgreSQL 数据库和用户（示例）
sudo -u postgres psql <<'SQL'
CREATE USER aura WITH PASSWORD 'your_db_password';
CREATE DATABASE aura_db OWNER aura;
SQL

# 运行迁移
source .venv/bin/activate
alembic upgrade head
```

### 5.5 启动服务

```bash
# 开发调试
uvicorn app.main:app --host 0.0.0.0 --port 8000

# 生产（systemd，详见 DEPLOY.md）
sudo systemctl start aura-api aura-worker
sudo systemctl enable aura-api aura-worker
```

### 5.6 验证服务

```bash
# 健康检查（无需 Token）
curl -s https://your-domain.com/api/v1/health

# 预期：{"status":"ok"}
```

---

## 6. Android App 配置流程

### 6.1 需要内置的值

在 App 中配置与服务器 `.env` **完全相同**的三项：

```kotlin
// 示例：build.gradle.kts 或 local.properties（不要提交 Git）
object AuraApiConfig {
    const val BASE_URL = "https://your-domain.com/api/v1"
    const val PACKAGE_NAME = "com.example.aura"   // = ALLOWED_PACKAGE_NAME
    const val API_TOKEN = "K7xR2mN9pQ4vL8wY3zA6bC1dE5fG0hJ"
    const val API_SECRET = "9sT2uV5wX8yZ1aB4cD7eF0gH3jK6lM9nP2qR5sT8uV1wX4yZ7aB0cD3eF6gH"
}
```

> **注意：** `applicationId` 必须与 `ALLOWED_PACKAGE_NAME` 完全一致。

### 6.2 每次请求的 Header

App 网络层需自动附加：

```http
Authorization: Bearer <API_TOKEN>
X-App-Package: <PACKAGE_NAME>
X-App-Version: 1.0.0
X-Timestamp: <unix_timestamp>
X-Nonce: <random_uuid>
X-Signature: <hmac_sha256_hex>
```

### 6.3 签名计算（App 端）

```
签名字符串 = METHOD + "\n" + PATH + "\n" + Timestamp + "\n" + Nonce + "\n" + SHA256(body)
X-Signature = HMAC-SHA256(API_SECRET, 签名字符串).hex()
```

Kotlin 伪代码：

```kotlin
fun sign(method: String, path: String, timestamp: Long, nonce: String, body: ByteArray): String {
    val bodyHash = MessageDigest.getInstance("SHA-256").digest(body)
        .joinToString("") { "%02x".format(it) }
    val payload = listOf(method, path, timestamp.toString(), nonce, bodyHash).joinToString("\n")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(API_SECRET.toByteArray(), "HmacSHA256"))
    return mac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }
}
```

### 6.4 App 配置检查清单

- [ ] `applicationId` == 服务器 `ALLOWED_PACKAGE_NAME`
- [ ] `API_TOKEN` 与服务器 `.env` 一致
- [ ] `API_SECRET` 与服务器 `.env` 一致
- [ ] 每次请求带 timestamp（与服务器时差 < 5 分钟）
- [ ] 每次请求 nonce 唯一（UUID）
- [ ] **multipart 上传**（`/media/audio`、`/media/video`）签名时 body 为空
- [ ] `/scene/generate` 客户端 timeout ≥ 120 秒
- [ ] `API_SECRET` 不打印到 Logcat

### 6.5 Android 业务接口（新）

凭证配置好后，业务调用见 **[API.md](./API.md)**，推荐流程：

1. `POST /media/audio?user_id=`
2. `POST /media/video?user_id=`
3. `POST /scene/generate`（JSON，同步等待）
4. `GET /records?user_id=`

---

## 7. 验证鉴权是否生效

### 7.1 无 Token → 401

```bash
curl -s -o /dev/null -w "%{http_code}" https://your-domain.com/api/v1/records
# 预期：401
```

### 7.2 错误包名 → 403

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer <正确Token>" \
  -H "X-App-Package: com.fake.app" \
  https://your-domain.com/api/v1/records
# 预期：403
```

### 7.3 完整签名请求 → 200/201

内测阶段建议直接在 App 里验证；手动 curl 需正确计算 HMAC，较繁琐。  
编码完成后，`tests/` 目录会提供签名测试用例供参考。

---

## 8. Token 轮换（泄露或定期更换）

### 8.1 轮换流程

```mermaid
flowchart TD
    A["生成新 Token + Secret"] --> B["更新服务器 .env"]
    B --> C["重启 aura-api / aura-worker"]
    C --> D["发布新版 App（含新凭证）"]
    D --> E["确认旧 App 版本无法访问"]
```

### 8.2 操作命令

```bash
# 1. 生成新凭证
bash scripts/generate_credentials.sh

# 2. 更新 .env 中的 API_TOKEN 和 API_SECRET
vim /opt/aura/AuraAppServer/.env

# 3. 重启服务
sudo systemctl restart aura-api aura-worker

# 4. 发布新版 App
```

### 8.3 注意事项

- 轮换后**旧 Token 立即失效**，需同步发版 App
- 建议在低峰期操作
- 不要将新旧 Token 同时长期有效（内测阶段不支持双 Token）

---

## 9. 安全规范

| 规则 | 说明 |
|------|------|
| 不进 Git | `.env`、真实 Token/Secret 不得提交仓库 |
| 最小权限 | 服务器 `.env` 权限 `600`，仅运行用户可读 |
| 分环境 | 开发 / 内测 / 生产使用**不同** Token |
| 禁止 Log | 服务端和 App 均不打印 Token / Secret |
| 泄露即换 | 怀疑泄露后立刻轮换，不要犹豫 |
| 包名绑定 | 即使 Token 泄露，错误包名仍会被 403 拒绝 |

---

## 10. 常见问题

### Q1：App 报 401，怎么办？

1. 检查 `Authorization: Bearer ` 前缀是否正确
2. 对比 App 内 `API_TOKEN` 与服务器 `.env` 是否完全一致（无多余空格）
3. 确认服务器已重启并加载最新 `.env`

### Q2：App 报 403 PACKAGE_MISMATCH？

1. 检查 App 的 `applicationId`
2. 对比服务器 `ALLOWED_PACKAGE_NAME`
3. 两者必须**字符级完全一致**（区分大小写）

### Q3：App 报 403 SIGNATURE_INVALID？

1. 检查 `API_SECRET` 两端是否一致
2. 确认签名字符串拼接顺序：METHOD / PATH / Timestamp / Nonce / SHA256(body)
3. 确认 GET 请求 body 为空时 SHA256 为空字符串的哈希
4. 检查 PATH 是否包含 `/api/v1` 前缀（与服务端验签规则一致）
5. **multipart 上传**时 body 必须按**空字节**计算 SHA256（见 [API.md](./API.md) §3.2）

### Q4：App 报 403 REPLAY_DETECTED？

- 同一 `nonce` 被使用了两次；确保每次请求生成新 UUID

### Q5：能否给测试同学单独发 Token？

- 内测阶段**不建议**多人多 Token
- 所有人用同一套 App 包 + 同一 Token 即可
- 若必须区分，需改架构（不在当前设计范围内）

---

## 11. 相关文件

| 文件 | 说明 |
|------|------|
| [API.md](./API.md) | **Android 接口文档（主）** |
| [TECH_DESIGN.md](./TECH_DESIGN.md) | 整体技术设计 |
| [DEPLOY.md](./DEPLOY.md) | 部署说明（Ubuntu 24.04 · 阿里云 ECS） |
| `/.env.example` | 环境变量模板 |
| `/scripts/generate_credentials.sh` | 凭证生成脚本 |

---

*文档结束*
