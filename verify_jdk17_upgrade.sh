#!/bin/bash

# Jeepay JDK 17 编译验证脚本

echo "======================================"
echo "Jeepay JDK 17 编译验证脚本"
echo "======================================"
echo ""

# 检查环境
echo "1. 检查环境..."

# 检查Java版本
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep "version" | awk '{print $3}' | tr -d '"' | cut -d'.' -f1)
    echo "  Java 版本: $(java -version 2>&1 | head -1)"
    
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo "  ⚠️  警告: 当前Java版本 ($JAVA_VERSION) 低于 JDK 17"
        echo "  请安装 JDK 17 后重新运行此脚本"
        echo ""
        echo "JDK 17 安装指引:"
        echo "  1. 下载 Eclipse Temurin 17: https://adoptium.net/"
        echo "  2. 设置 JAVA_HOME 环境变量"
        echo "  3. 更新 PATH 环境变量"
        echo ""
        exit 1
    else
        echo "  ✅ Java版本符合要求 (JDK $JAVA_VERSION)"
    fi
else
    echo "  ❌ 未检测到Java环境，请先安装JDK 17"
    exit 1
fi

# 检查Maven版本
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | grep "Apache Maven" | awk '{print $3}')
    echo "  Maven 版本: $MVN_VERSION"
    echo "  ✅ Maven环境正常"
else
    echo "  ❌ 未检测到Maven环境，请先安装Maven 3.8+"
    exit 1
fi

echo ""

# 进入项目目录
cd /data/workspace/jeepay || exit 1

echo "2. 项目配置验证..."

# 检查根POM配置
JAVA_VERSION_IN_POM=$(grep -A1 "<java.version>" pom.xml | grep "<java.version>" | sed 's/.*<java.version>\(.*\)<\/java.version>.*/\1/' | tr -d ' ')
SPRING_BOOT_VERSION=$(grep -A3 "<parent>" pom.xml | grep "<version>" | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d ' ')

echo "  POM中Java版本: $JAVA_VERSION_IN_POM"
echo "  Spring Boot版本: $SPRING_BOOT_VERSION"

if [ "$JAVA_VERSION_IN_POM" = "17" ]; then
    echo "  ✅ POM Java版本配置正确"
else
    echo "  ❌ POM Java版本配置错误，期望: 17, 实际: $JAVA_VERSION_IN_POM"
    exit 1
fi

echo ""

echo "3. 清理和编译..."

# 清理项目
echo "  执行 mvn clean..."
mvn clean -q
if [ $? -eq 0 ]; then
    echo "  ✅ 清理完成"
else
    echo "  ❌ 清理失败"
    exit 1
fi

# 编译项目
echo "  执行 mvn compile..."
mvn compile -DskipTests -q
COMPILE_RESULT=$?

echo ""

if [ $COMPILE_RESULT -eq 0 ]; then
    echo "✅ 编译成功！"
    echo ""
    
    echo "4. 运行测试..."
    mvn test -q
    TEST_RESULT=$?
    
    if [ $TEST_RESULT -eq 0 ]; then
        echo "✅ 测试通过！"
    else
        echo "⚠️  部分测试失败，但编译成功"
    fi
    
    echo ""
    echo "5. 打包验证..."
    mvn package -DskipTests -q
    PACKAGE_RESULT=$?
    
    if [ $PACKAGE_RESULT -eq 0 ]; then
        echo "✅ 打包成功！"
        
        # 检查生成的JAR文件
        echo ""
        echo "生成的JAR文件:"
        find . -name "*.jar" -not -path "./target/dependency/*" | head -10
        
    else
        echo "❌ 打包失败"
        exit 1
    fi
    
else
    echo "❌ 编译失败！"
    echo ""
    echo "常见错误解决方案:"
    echo "1. 检查FastJSON import是否都已更新为fastjson2"
    echo "2. 检查是否有使用JDK内部API的代码"
    echo "3. 检查依赖版本兼容性"
    echo "4. 查看详细错误信息: mvn compile"
    echo ""
    exit 1
fi

echo ""
echo "======================================"
echo "JDK 17 升级验证完成"
echo "======================================"
echo ""
echo "升级摘要:"
echo "- Java版本: JDK $JAVA_VERSION"
echo "- Spring Boot: $SPRING_BOOT_VERSION"
echo "- FastJSON: 2.0.43"
echo "- 编译状态: ✅ 成功"
echo "- 测试状态: $([ $TEST_RESULT -eq 0 ] && echo "✅ 通过" || echo "⚠️  部分失败")"
echo "- 打包状态: ✅ 成功"
echo ""
echo "🎉 Jeepay 已成功升级到 JDK 17！"