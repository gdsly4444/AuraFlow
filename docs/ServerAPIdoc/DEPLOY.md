# AuraAppServer 部署说明（Ubuntu 24.04 · 阿里云 ECS）

> 目标环境：阿里云 ECS，Ubuntu 24.04，单机部署，本机媒体存储  
> 代码仓库：`https://github.com/gdsly4444/AuraAppServer.git`  
> 关联文档：[SERVER_CONFIG.md](./SERVER_CONFIG.md)（鉴权与 Token 详解）| [TECH_DESIGN.md](./TECH_DESIGN.md)

**本机私密配置（IP、SSH、一键部署命令）不放在 Git 里。** 首次克隆后执行：

```bash
bash scripts/init-local-config.sh
vim local/server.env          # 填入公网 IP 等
vim docs/DEPLOY.local.md      # 可选：补充部署备忘
```

模板见 `local/server.env.example`、`docs/DEPLOY.local.example.md`。

## 0. 架构一览

```text
Android App ──HTTP/HTTPS──► 阿里云 ECS (Ubuntu 24.04)
                              ├── aura-api    (uvicorn :8000)
                              ├── aura-worker (LLM 任务队列)
                              ├── PostgreSQL  (本机 :5432，不对公网)
                              └── /data/media (视频/封面/音频)
```

两个进程**都必须运行**：API 接收请求，Worker 调用百炼 LLM 生成场景描述。

---

## 1. 前置条件

| 项 | 要求 |
|----|------|
| OS | **Ubuntu 24.04 LTS** |
| 规格 | 建议 ≥ 2C2G |
| Python | 3.12（系统自带） |
| PostgreSQL | 16（Ubuntu 24.04 默认） |
| ffmpeg | 缩略图生成 |
| 磁盘 | ≥ 40GB；媒体目录 `/data/media` |
| Swap | **2GB**（2G 内存机器必须） |
| 公网 | 有公网 IP 或绑定域名（具体 IP 写在 `local/server.env`，勿提交 Git） |

---

## 2. 阿里云安全组（控制台操作）

登录 [阿里云 ECS 控制台](https://ecs.console.aliyun.com/) → 实例 → 安全组 → **入方向**：

| 端口 | 协议 | 来源 | 用途 |
|------|------|------|------|
| 22 | TCP | 你的 IP 或 0.0.0.0/0 | SSH |
| 8000 | TCP | 0.0.0.0/0 | API（**无域名、用 IP 访问时**） |
| 443 | TCP | 0.0.0.0/0 | HTTPS（**有域名时**，见 §9） |

**禁止**对公网开放 **5432**（PostgreSQL）。

---

## 3. SSH 登录服务器

在本地电脑：

```bash
ssh root@<PUBLIC_IP>
```

将 IP 换成你的公网地址。以下命令均在服务器上执行。

---

## 4. 安装系统依赖

```bash
apt update
apt upgrade -y
apt install -y \
  python3 \
  python3-venv \
  python3-pip \
  git \
  postgresql \
  postgresql-contrib \
  ffmpeg \
  curl \
  vim

# 2GB Swap（2C2G 机器强烈建议）
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab

# 验证
python3 --version    # 应 ≥ 3.12
ffmpeg -version
systemctl status postgresql
```

---

## 5. 初始化 PostgreSQL

将 `你的数据库密码` 替换为强密码（与后续 `.env` 中一致）：

```bash
systemctl enable postgresql
systemctl start postgresql

sudo -u postgres psql <<'SQL'
CREATE USER aura WITH PASSWORD '你的数据库密码';
CREATE DATABASE aura_db OWNER aura;
GRANT ALL PRIVILEGES ON DATABASE aura_db TO aura;
SQL
```

验证：

```bash
sudo -u postgres psql -c "\l" | grep aura_db
```

---

## 6. 创建目录并拉取代码

```bash
mkdir -p /opt/aura /data/media /var/log/aura

cd /opt/aura
git clone https://github.com/gdsly4444/AuraAppServer.git
cd AuraAppServer
```

---

## 7. 生成凭证

```bash
bash scripts/generate_credentials.sh
```

**立即保存输出**中的 `API_TOKEN` 和 `API_SECRET`，后续写入 `.env` 和 Android App。

将 `ALLOWED_PACKAGE_NAME` 改为你的 Android `applicationId`（默认示例为 `com.example.aura`）。

---

## 8. 配置 `.env`

```bash
cp .env.example .env
vim .env
chmod 600 .env
```

### 8.1 生产环境完整示例

```env
# ── 应用 ──────────────────────────────────────
APP_ENV=production
APP_HOST=0.0.0.0
APP_PORT=8000
LOG_LEVEL=info
LOG_FILE=/var/log/aura/app.log
LOG_MAX_BYTES=10485760
LOG_BACKUP_COUNT=5
LOG_FORMAT=text
TRUST_PROXY_HEADERS=true

# ── 客户端鉴权（必填，与 Android App 完全一致）──
ALLOWED_PACKAGE_NAME=com.example.aura
API_TOKEN=上一步 generate_credentials.sh 生成的值
API_SECRET=上一步 generate_credentials.sh 生成的值

# ── 数据库 ────────────────────────────────────
DATABASE_URL=postgresql+asyncpg://aura:你的数据库密码@127.0.0.1:5432/aura_db

# ── 媒体存储 ──────────────────────────────────
MEDIA_ROOT=/data/media
STORAGE_QUOTA_MB=20480
MIN_FREE_GB=10
MAX_UPLOAD_MB=30

# ── LLM（百炼 DashScope）──────────────────────
LLM_API_KEY=sk-你的百炼Key
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

### 8.2 配置项说明

| 变量 | 必填 | 说明 |
|------|------|------|
| `ALLOWED_PACKAGE_NAME` | ✅ | Android `applicationId`，字符级一致 |
| `API_TOKEN` | ✅ | App 请求头 `Authorization: Bearer ...` |
| `API_SECRET` | ✅ | 仅用于 HMAC 签名，不在网络传输 |
| `DATABASE_URL` | ✅ | PostgreSQL 连接串 |
| `MEDIA_ROOT` | ✅ | 媒体文件根目录，默认 `/data/media` |
| `LLM_API_KEY` | ✅ | 百炼 API Key；Worker 调用 LLM 必需 |
| `LLM_MODEL` | ❌ | 默认 `qwen3-omni-flash` |
| `MAX_UPLOAD_MB` | ❌ | 单文件上限，默认 30MB |
| `MIN_FREE_GB` | ❌ | 磁盘剩余低于此值拒绝上传，默认 10GB |

鉴权与签名算法详见 [SERVER_CONFIG.md](./SERVER_CONFIG.md)。

---

## 9. 安装 Python 依赖与数据库迁移

```bash
cd /opt/aura/AuraAppServer
bash scripts/deploy.sh
```

脚本会：创建 `.venv` → 安装 `requirements.txt` → 执行 `alembic upgrade head`。

成功输出：`Deploy preparation complete.`

手动等价命令：

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
alembic upgrade head
```

---

## 10. 启动服务（systemd）

提供两种方式，**二选一**。

### 方式 A：root 运行（简单，适合个人内测）

```bash
cd /opt/aura/AuraAppServer

cp deploy/aura-api.root.service /etc/systemd/system/aura-api.service
cp deploy/aura-worker.root.service /etc/systemd/system/aura-worker.service

systemctl daemon-reload
systemctl enable aura-api aura-worker
systemctl start aura-api aura-worker
systemctl status aura-api aura-worker
```

### 方式 B：专用 aura 用户运行（推荐生产）

```bash
useradd -r -s /usr/sbin/nologin aura 2>/dev/null || true
chown -R aura:aura /opt/aura /data/media /var/log/aura

cd /opt/aura/AuraAppServer
cp deploy/aura-api.service /etc/systemd/system/
cp deploy/aura-worker.service /etc/systemd/system/

systemctl daemon-reload
systemctl enable aura-api aura-worker
systemctl start aura-api aura-worker
systemctl status aura-api aura-worker
```

两个服务均应为 `active (running)`：

| 服务 | 作用 |
|------|------|
| `aura-api` | FastAPI，监听 `0.0.0.0:8000` |
| `aura-worker` | 轮询任务队列，调用 LLM 写总结 |

---

## 11. 验证部署

### 11.1 服务器本机

```bash
curl -s http://127.0.0.1:8000/api/v1/health
```

预期：

```json
{"status":"ok","env":"production"}
```

### 11.2 公网（IP 直接访问）

```bash
curl -s http://<PUBLIC_IP>:8000/api/v1/health
```

若本机通、公网不通 → 检查阿里云安全组是否放行 **8000**。

### 11.3 查看日志

```bash
journalctl -u aura-api -f
journalctl -u aura-worker -f
tail -f /var/log/aura/app.log
```

---

## 12. Android App 配置

> **接口流程、传参、请求/响应样例见 [API.md](./API.md)。** 本节仅列凭证与 Header。

App 内置值必须与服务器 `.env` **完全一致**：

```kotlin
object AuraApiConfig {
    // 无域名时用 IP（内测）
    const val BASE_URL = "http://<PUBLIC_IP>:8000/api/v1"

    // 有域名 + HTTPS 后改为：
    // const val BASE_URL = "https://your-domain.com/api/v1"

    const val PACKAGE_NAME = "com.example.aura"   // = ALLOWED_PACKAGE_NAME
    const val API_TOKEN = "与 .env 中 API_TOKEN 相同"
    const val API_SECRET = "与 .env 中 API_SECRET 相同"
}
```

每次请求需附加 Header（签名算法见 [SERVER_CONFIG.md](./SERVER_CONFIG.md)，multipart 注意 body 为空）：

```http
Authorization: Bearer <API_TOKEN>
X-App-Package: <PACKAGE_NAME>
X-App-Version: 1.0.0
X-Timestamp: <unix_timestamp>
X-Nonce: <random_uuid>
X-Signature: <hmac_sha256_hex>
```

---

## 13. 可选：绑定域名 + HTTPS（Caddy）

有域名且 DNS A 记录指向服务器 IP 时，可用 Caddy 自动申请证书，App 改用 HTTPS。

### 13.1 安装 Caddy

```bash
apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
apt update
apt install -y caddy
```

### 13.2 配置 Caddyfile

```bash
vim /etc/caddy/Caddyfile
```

内容（替换域名和邮箱）：

```caddy
{
  email admin@example.com
}

your-domain.com {
  encode gzip
  reverse_proxy 127.0.0.1:8000
}
```

```bash
systemctl enable caddy
systemctl restart caddy
curl -s https://your-domain.com/api/v1/health
```

### 13.3 安全组调整

有 HTTPS 后可关闭公网 **8000**，仅保留 **443** 和 **22**。

---

## 14. 业务 API 速查

> 完整说明（含 Android 传参样例）：**[API.md](./API.md)**

Base URL：`http://<PUBLIC_IP>:8000/api/v1`（或 HTTPS 域名；IP 见 `local/server.env`）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| GET | `/health` | 否 | 健康检查 |
| POST | `/media/audio` | 是 | 上传音频 → `media_id` |
| POST | `/media/video` | 是 | 上传视频 → `media_id` |
| POST | `/scene/generate` | 是 | **同步** LLM → `color` + `description` |
| GET | `/records` | 是 | 历史列表（含 `color`） |
| GET | `/records/{id}` | 是 | 详情 |
| GET | `/records/{id}/media` | 是 | 下载视频 |
| GET | `/records/{id}/audio` | 是 | 下载音频 |
| GET | `/records/{id}/thumbnail` | 是 | 下载缩略图 |
| POST | `/records` | 是 | 创建记录（旧流程） |
| POST | `/records/{id}/upload` | 是 | 上传 + Worker（旧流程） |

上传接口 Query：`user_id`（必填）。旧流程另有 `finalize`（默认 false）。

**部署后必做：** `git pull` 后执行 `systemctl restart aura-api aura-worker`，否则新路由不会生效。

---

## 15. 常用运维命令

```bash
# 重启
systemctl restart aura-api aura-worker

# 停止
systemctl stop aura-api aura-worker

# 查看状态
systemctl status aura-api aura-worker

# 更新代码
cd /opt/aura/AuraAppServer
git pull
bash scripts/deploy.sh
systemctl restart aura-api aura-worker

# 数据库备份
bash scripts/backup_db.sh

# 媒体备份
bash scripts/backup_media.sh

# 磁盘使用
df -h /data/media
du -sh /data/media/*
```

---

## 16. 故障排查

| 现象 | 排查步骤 |
|------|----------|
| 本机 curl 通，公网不通 | 阿里云安全组是否放行 8000（或 443） |
| `deploy.sh` 数据库报错 | 检查 `DATABASE_URL` 密码；`systemctl status postgresql` |
| `type "record_status" already exists` | 见下方 **迁移失败修复** |
| 401 AUTH_INVALID | 对比 App 与 `.env` 的 `API_TOKEN`（无多余空格） |
| 403 PACKAGE_MISMATCH | App `applicationId` ≠ `ALLOWED_PACKAGE_NAME` |
| 403 SIGNATURE_INVALID | `API_SECRET` 不一致；PATH 须含 `/api/v1` 前缀 |
| 403 REPLAY_DETECTED | 同一 `nonce` 重复使用，每次请求用新 UUID |
| 507 DISK_FULL / QUOTA | `df -h`；清理旧媒体或扩容 |
| 502 SCENE_GENERATION_FAILED | 检查 `LLM_API_KEY`；API 日志 |
| Worker 不处理任务 | 仅旧流程；新流程走 `/scene/generate` 同步，不经过 Worker |
| git pull 后新接口 404 | 必须 `systemctl restart aura-api` |
| ffmpeg 失败 | `ffmpeg -version`；`apt install ffmpeg` |
| 服务启动失败 | `journalctl -u aura-api -n 50`；确认 `/opt/aura/AuraAppServer/.env` 存在 |

手动检查任务队列：

```bash
sudo -u postgres psql -d aura_db -c "SELECT id, record_id, status, error_message FROM tasks ORDER BY created_at DESC LIMIT 10;"
```

### 迁移失败修复（`record_status already exists`）

上次迁移跑到一半时，ENUM 类型可能已创建但表未建完。在服务器上执行：

```bash
cd /opt/aura/AuraAppServer
git pull

# 查看当前状态
sudo -u postgres psql -d aura_db -c "\dt"
sudo -u postgres psql -d aura_db -c "SELECT * FROM alembic_version;"
```

**若没有 records 表**（只有残留的 ENUM 类型），清理后重跑：

```bash
sudo -u postgres psql -d aura_db <<'SQL'
DROP TYPE IF EXISTS record_status CASCADE;
DROP TYPE IF EXISTS task_status CASCADE;
DROP TYPE IF EXISTS task_type CASCADE;
SQL

source .venv/bin/activate
alembic upgrade head
```

**若 records 表已存在**，标记版本后继续：

```bash
source .venv/bin/activate
alembic stamp 001
alembic upgrade head
```

成功时应显示 `alembic current` → `003 (head)`。

---

## 17. 资源建议（2C2G）

- uvicorn `--workers 1`（已在 systemd 中配置）
- Worker 单进程，单任务并发
- 上传限制 30MB，并发上传建议 ≤ 2
- 必须配置 2GB Swap

---

## 18. 一键命令清单

含真实 IP、SSH 账号的一键脚本见 **`docs/DEPLOY.local.md`**（由 `docs/DEPLOY.local.example.md` 复制，**仅保留在本机，勿提交 Git**）。

---

*文档版本：Ubuntu 24.04 · 2026-06-05*
