#!/bin/bash
# SparkLink 生产一键部署（部署目录入口）
# 等价于: ./scripts/deploy-prod.sh "$@"
exec "$(cd "$(dirname "$0")/.." && pwd)/scripts/deploy-prod.sh" "$@"
