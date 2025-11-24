#!/bin/bash

# Jeepay MySQL 升级环境初始化脚本
# 用途：设置脚本权限和环境准备
# 执行方式：bash setup-upgrade-env.sh

echo "======================================"
echo "Jeepay MySQL 升级环境初始化"
echo "======================================"

# 检查当前目录是否为 Jeepay 项目根目录
if [ ! -f "docker-compose.yml" ] || [ ! -f "pom.xml" ]; then
    echo "错误: 请在 Jeepay 项目根目录下执行此脚本"
    exit 1
fi

# 创建 scripts 目录（如果不存在）
mkdir -p scripts

# 设置脚本执行权限
echo "设置脚本执行权限..."
chmod +x scripts/*.sh 2>/dev/null || true

# 检查必要的工具
echo "检查必要工具..."

check_tool() {
    if command -v $1 >/dev/null 2>&1; then
        echo "✓ $1 已安装"
    else
        echo "✗ $1 未安装或不在 PATH 中"
        MISSING_TOOLS=true
    fi
}

MISSING_TOOLS=false

check_tool "docker"
check_tool "docker-compose"
check_tool "bc"  # 用于计算
check_tool "curl"  # 用于服务检查

if [ "$MISSING_TOOLS" = true ]; then
    echo ""
    echo "请安装缺失的工具后重新运行此脚本"
    exit 1
fi

# 检查 Maven（可选）
if command -v mvn >/dev/null 2>&1; then
    echo "✓ Maven 已安装"
else
    echo "⚠ Maven 未安装，升级过程中将跳过编译步骤"
fi

echo ""
echo "环境检查完成！"
echo ""
echo "📋 可用的升级脚本："
echo "  1. bash scripts/pre-upgrade-check.sh   - 升级前环境检查"
echo "  2. bash scripts/backup-data.sh         - 数据备份"
echo "  3. bash scripts/mysql-upgrade.sh       - 主升级流程"
echo "  4. bash scripts/verify-upgrade.sh      - 升级后验证"
echo "  5. bash scripts/rollback.sh [备份目录] - 回滚操作"
echo ""
echo "🚀 推荐执行顺序："
echo "  bash scripts/pre-upgrade-check.sh && bash scripts/mysql-upgrade.sh && bash scripts/verify-upgrade.sh"
echo ""
echo "📖 详细说明请查看: MYSQL_UPGRADE_GUIDE.md"