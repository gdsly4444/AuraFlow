# 部署私密备忘（模板）

> 复制为 `docs/DEPLOY.local.md` 并填写真实 IP。**`DEPLOY.local.md` 已在 .gitignore。**  
> 通用步骤见 [DEPLOY.md](./DEPLOY.md)。

```bash
cp docs/DEPLOY.local.example.md docs/DEPLOY.local.md
vim docs/DEPLOY.local.md
```

---

## 服务器信息

| 项 | 值 |
|----|-----|
| 公网 IP | `<PUBLIC_IP>` |
| SSH | `ssh <SSH_USER>@<PUBLIC_IP>` |
| API Base URL | `http://<PUBLIC_IP>:8000/api/v1` |
| 部署路径 | `/opt/aura/AuraAppServer` |

## 健康检查

```bash
curl -s http://<PUBLIC_IP>:8000/api/v1/health
```

## Android App

```kotlin
const val BASE_URL = "http://<PUBLIC_IP>:8000/api/v1"
```

## 一键部署

（将 `<PUBLIC_IP>`、密码等替换后粘贴到服务器执行；完整命令可写在下方。）

## 私密备注

- 数据库密码：
- LLM Key：
- 其他：
