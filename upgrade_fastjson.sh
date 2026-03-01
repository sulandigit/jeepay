#!/bin/bash

# Jeepay FastJSON 1.x 到 2.x 批量升级脚本

echo "开始批量更新 FastJSON import 语句..."

# 定义需要处理的目录
MODULES=(
    "jeepay-manager"
    "jeepay-merchant" 
    "jeepay-payment"
    "jeepay-service"
    "jeepay-z-codegen"
)

# 统计更新的文件数量
TOTAL_FILES=0

for module in "${MODULES[@]}"; do
    if [ -d "/data/workspace/jeepay/$module" ]; then
        echo "处理模块: $module"
        
        # 统计该模块中的相关文件
        FILES=$(find "/data/workspace/jeepay/$module" -name "*.java" -exec grep -l "import com\.alibaba\.fastjson\." {} \;)
        FILE_COUNT=$(echo "$FILES" | wc -l)
        
        if [ -n "$FILES" ] && [ "$FILE_COUNT" -gt 0 ]; then
            echo "  发现 $FILE_COUNT 个文件需要更新"
            TOTAL_FILES=$((TOTAL_FILES + FILE_COUNT))
            
            # 批量替换 import 语句
            find "/data/workspace/jeepay/$module" -name "*.java" -exec sed -i 's/import com\.alibaba\.fastjson\./import com.alibaba.fastjson2./g' {} \;
            
            echo "  ✓ 已更新 $module 模块的 FastJSON import 语句"
        else
            echo "  - 该模块没有需要更新的文件"
        fi
    else
        echo "  ⚠ 模块目录不存在: $module"
    fi
done

echo ""
echo "FastJSON 升级完成!"
echo "总共更新了 $TOTAL_FILES 个文件"
echo ""

# 验证更新结果
echo "验证更新结果..."
REMAINING_OLD=$(find "/data/workspace/jeepay" -name "*.java" -exec grep -l "import com\.alibaba\.fastjson\." {} \; | wc -l)
NEW_IMPORTS=$(find "/data/workspace/jeepay" -name "*.java" -exec grep -l "import com\.alibaba\.fastjson2\." {} \; | wc -l)

echo "剩余旧版本 import: $REMAINING_OLD"
echo "新版本 import 文件数: $NEW_IMPORTS"

if [ "$REMAINING_OLD" -eq 0 ]; then
    echo "✅ 所有 FastJSON import 已成功升级到 2.x 版本"
else
    echo "⚠️  仍有 $REMAINING_OLD 个文件使用旧版本 import，需要手动检查"
    find "/data/workspace/jeepay" -name "*.java" -exec grep -l "import com\.alibaba\.fastjson\." {} \;
fi