# 智能问答 AI 系统

基于企业私有知识的智能问答平台，支持自然语言交互，能够精准回答技术问题、运维操作、产品配置等咨询。

## 技术栈

- **后端框架**：Spring Boot 3.x、Spring AI、LangChain4j
- **模型推理**：Ollama + vLLM（DeepSeek-Coder / Qwen）
- **缓存**：Redis Cluster、Caffeine
- **消息队列**：RocketMQ
- **实时计算**：Flink
- **容器化**：Docker、Kubernetes

## 核心功能

- 自然语言问答：支持员工通过自然语言提问，系统返回精准答案
- RAG 检索增强：从企业私有知识库中检索相关文档片段作为上下文
- 多轮对话：保留最近 5 轮交互历史，提升复杂问题的回答连贯性
- 流式输出：支持 SSE 流式返回，降低首字延迟
- 热点统计：实时统计热点问题与 Token 消耗，为缓存优化提供数据支撑

## 快速开始

### 环境要求

- JDK 17+
- Docker & Docker Compose
- Ollama（需提前拉取 DeepSeek-Coder / Qwen 模型）

### 本地运行

```bash
# 进入你的项目目录（假设项目在本地）
cd /path/to/your/Intelligent-Q-A-AI-System

# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 首次提交
git commit -m "Initial commit: 智能问答 AI 系统"

# 添加远程仓库（替换为你的 GitHub 用户名）
git remote add origin https://github.com/DayiSiki/Intelligent-Q-A-AI-System.git

# 推送代码
git branch -M main
git push -u origin main
