# 🚀 Jeepay MySQL 8.0.35 升级快速指南

## 一键升级命令

```bash
# 1. 环境准备
bash scripts/setup-upgrade-env.sh

# 2. 升级前检查
bash scripts/pre-upgrade-check.sh

# 3. 执行升级
bash scripts/mysql-upgrade.sh

# 4. 验证升级
bash scripts/verify-upgrade.sh
```

## 应急回滚

```bash
# 查看备份目录
ls -la backup/

# 执行回滚（替换为实际备份目录）
bash scripts/rollback.sh backup/mysql_upgrade_YYYYMMDD_HHMMSS
```

## 升级内容

- ✅ MySQL: `8.0.25` → `8.0.35`
- ✅ MySQL Connector/J: `8.0.28` → `8.0.35`
- ✅ 性能配置优化
- ✅ 安全性增强
- ✅ 完整备份策略
- ✅ 自动化验证

## 升级后检查清单

- [ ] MySQL 版本确认: `docker exec jeepay-mysql mysql --version`
- [ ] 应用服务状态: `docker ps | grep jeepay`
- [ ] 管理后台访问: http://localhost:9227
- [ ] 商户后台访问: http://localhost:9228
- [ ] 支付网关访问: http://localhost:9226
- [ ] 连接池监控: http://localhost:9217/druid/

## 文件说明

| 文件 | 说明 |
|------|------|
| `MYSQL_UPGRADE_GUIDE.md` | 完整升级指南文档 |
| `mysql-upgrade-plan.md` | 详细技术实施计划 |
| `scripts/pre-upgrade-check.sh` | 升级前环境检查 |
| `scripts/backup-data.sh` | 数据备份脚本 |
| `scripts/mysql-upgrade.sh` | 主升级执行脚本 |
| `scripts/verify-upgrade.sh` | 升级后验证测试 |
| `scripts/rollback.sh` | 回滚脚本 |

---
💡 **重要提醒**: 生产环境请先在测试环境验证完整流程！