# LangGraph4j 与 LangChain4j 集成方案

## 📖 核心概念

### LangChain4j - LLM 应用开发框架
**作用**：简化与大语言模型（LLM）的集成
- 🔌 统一的 LLM 接口（OpenAI、通义千问、文心一言等）
- 💬 对话记忆管理（ChatMemory）
- 🛠️ 函数调用/工具集成（Tools）
- 📝 提示词模板（Prompt Templates）
- 🔗 链式调用（Chains）
- 🧠 向量数据库集成（Embedding Stores）

### LangGraph4j - 状态机编排框架
**作用**：构建复杂的、有状态的 AI Agent 工作流
- 🔄 状态图管理（StateGraph）
- 🎯 节点和边的定义（Nodes & Edges）
- 🔀 条件路由（Conditional Edges）
- 🔁 循环和分支控制
- 📊 可视化工作流
- 🔄 多 Agent 协同

---

## 🎯 为什么要集成？

### 单独使用 LangChain4j 的局限
```
用户输入 → LangChain4j → LLM → 响应
```
- ❌ 缺少复杂的状态管理
- ❌ 难以处理多步骤工作流
- ❌ 条件分支逻辑复杂
- ❌ 多轮对话状态难以维护

### 单独使用 LangGraph4j 的局限
- ❌ 需要手动处理 LLM 调用
- ❌ 没有现成的 LLM 工具集成
- ❌ 提示词管理不方便
- ❌ 对话记忆需要自己实现

### 集成后的优势 ✅
```
用户输入 → LangGraph4j 状态机 → LangChain4j 工具 → LLM → 状态更新 → 响应
```
- ✅ **LangGraph4j** 管理复杂的状态流转
- ✅ **LangChain4j** 提供 LLM 调用能力
- ✅ 两者结合：构建企业级 Agent 应用

---

## 🏗️ 集成架构设计

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         用户请求层                                │
└────────────────────────┬────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────┐
│                  LangGraph4j 状态机层                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │              StateGraph (状态图)                       │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │                                                       │     │
│  │  [START] → [GREETING] → [COLLECTING] → [ANALYZING]   │     │
│  │                ↓            ↓            ↓            │     │
│  │           [RECOMMENDING] → [COMPLETED]               │     │
│  │                ↓            ↓                         │     │
│  │           [条件路由] ← [循环控制]                      │     │
│  │                                                       │     │
│  └───────────────────────────────────────────────────────┘     │
│                         │                                      │
│                         │ 调用节点处理器                         │
│                         ▼                                      │
│  ┌───────────────────────────────────────────────────────┐     │
│  │              Node Handlers (节点处理器)                │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │  • GreetingNode      - 欢迎节点                       │     │
│  │  • CollectingNode    - 信息收集节点                   │     │
│  │  • AnalyzingNode     - 信息分析节点                   │     │
│  │  • RecommendingNode  - 推荐生成节点                   │     │
│  └───────────────────────────────────────────────────────┘     │
│                         │                                      │
└─────────────────────────┼──────────────────────────────────────┘
                          │ 集成点 - 调用 LangChain4j
┌─────────────────────────▼──────────────────────────────────────┐
│                  LangChain4j 工具层                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │           AI Services (AI 服务接口)                    │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │  • MedicalAssistant (医疗助手服务)                     │     │
│  │  • InfoExtractor (信息提取服务)                        │     │
│  │  • RecommendationEngine (推荐引擎)                     │     │
│  └───────────────────────────────────────────────────────┘     │
│                         │                                      │
│  ┌───────────────────────────────────────────────────────┐     │
│  │              Chat Memory (对话记忆)                     │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │  • MessageWindowChatMemory                            │     │
│  │  • TokenWindowChatMemory                              │     │
│  └───────────────────────────────────────────────────────┘     │
│                         │                                      │
│  ┌───────────────────────────────────────────────────────┐     │
│  │                Tools (工具函数)                         │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │  • @Tool extractPatientInfo()                         │     │
│  │  • @Tool recommendDepartment()                        │     │
│  │  • @Tool searchDoctors()                              │     │
│  │  • @Tool checkCompleteness()                          │     │
│  └───────────────────────────────────────────────────────┘     │
│                         │                                      │
│  ┌───────────────────────────────────────────────────────┐     │
│  │         ChatLanguageModel (语言模型)                    │     │
│  ├───────────────────────────────────────────────────────┤     │
│  │  • OpenAI GPT-4 / GPT-3.5                             │     │
│  │  • 通义千问 Qwen                                        │     │
│  │  • 文心一言 ERNIE                                       │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼──────────────────────────────────────┐
│                    数据持久化层                                  │
├─────────────────────────────────────────────────────────────────┤
│  • StateStore (状态存储 - Redis/Memory)                         │
│  • VectorStore (向量数据库 - Pinecone/Milvus)                   │
│  • DataRepository (业务数据 - MySQL/JSON)                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 核心集成思路

### 1️⃣ 状态图设计（LangGraph4j）

**定义状态类**：

```java
@Data
@Builder
public class AgentState {
    // 基础信息
    private String sessionId;
    private String currentNode;
    
    // 对话上下文（传递给 LangChain4j）
    private List<ChatMessage> messages;
    private PatientInfo patientInfo;
    
    // 状态标记
    private boolean isComplete;
    private Map<String, Object> metadata;
    
    // LangChain4j 集成点
    private ChatMemory chatMemory;
    private String lastAiResponse;
}
```

**定义状态图**：

```java
@Configuration
public class GraphConfig {
    
    @Bean
    public StateGraph<AgentState> medicalAssistantGraph(
        GreetingNode greetingNode,
        CollectingNode collectingNode,
        AnalyzingNode analyzingNode,
        RecommendingNode recommendingNode
    ) {
        return StateGraph.<AgentState>builder()
            // 定义节点
            .addNode("greeting", greetingNode)
            .addNode("collecting", collectingNode)
            .addNode("analyzing", analyzingNode)
            .addNode("recommending", recommendingNode)
            
            // 定义边和路由
            .addEdge("START", "greeting")
            .addConditionalEdge("greeting", this::routeAfterGreeting)
            .addConditionalEdge("collecting", this::routeAfterCollecting)
            .addEdge("analyzing", "recommending")
            .addEdge("recommending", "END")
            
            // 设置入口点
            .setEntryPoint("greeting")
            .build();
    }
    
    // 条件路由：根据状态决定下一步
    private String routeAfterCollecting(AgentState state) {
        if (state.getPatientInfo().isComplete()) {
            return "analyzing";
        } else {
            return "collecting"; // 继续收集
        }
    }
    
    private String routeAfterGreeting(AgentState state) {
        return "collecting";
    }
}
```

### 2️⃣ 节点实现（集成 LangChain4j）

**核心节点示例 - CollectingNode**：

```java
@Component
@RequiredArgsConstructor
public class CollectingNode implements Node<AgentState> {
    
    // 注入 LangChain4j 服务
    private final MedicalAssistantService aiService;
    private final InfoExtractionService extractionService;
    private final ChatMemoryProvider chatMemoryProvider;
    
    @Override
    public AgentState process(AgentState state) {
        String sessionId = state.getSessionId();
        
        // 1. 获取对话记忆（LangChain4j）
        ChatMemory chatMemory = chatMemoryProvider.get(sessionId);
        state.setChatMemory(chatMemory);
        
        // 2. 使用 LangChain4j 提取信息
        String userMessage = getLastUserMessage(state);
        PatientInfo extractedInfo = extractionService.extractInfo(
            userMessage, 
            state.getPatientInfo()
        );
        state.setPatientInfo(extractedInfo);
        
        // 3. 使用 LangChain4j 生成回复
        String aiResponse;
        if (extractedInfo.isComplete()) {
            aiResponse = "好的，我已经了解了您的情况。正在为您分析...";
            state.setComplete(true);
        } else {
            // 调用 AI 生成追问
            aiResponse = aiService.generateFollowUpQuestion(
                extractedInfo,
                chatMemory
            );
            state.setComplete(false);
        }
        
        // 4. 更新对话历史
        chatMemory.add(UserMessage.from(userMessage));
        chatMemory.add(AiMessage.from(aiResponse));
        
        state.setLastAiResponse(aiResponse);
        state.setCurrentNode("collecting");
        
        return state;
    }
    
    private String getLastUserMessage(AgentState state) {
        List<ChatMessage> messages = state.getMessages();
        return messages.get(messages.size() - 1).text();
    }
}
```

**分析节点 - AnalyzingNode**：

```java
@Component
@RequiredArgsConstructor
public class AnalyzingNode implements Node<AgentState> {
    
    private final MedicalTools medicalTools;
    private final ChatLanguageModel chatModel;
    
    @Override
    public AgentState process(AgentState state) {
        PatientInfo patientInfo = state.getPatientInfo();
        
        // 使用 LangChain4j Tools 分析
        String department = medicalTools.recommendDepartment(
            patientInfo.getMainSymptom()
        );
        
        List<Doctor> doctors = medicalTools.searchDoctors(
            department,
            patientInfo.getMainSymptom()
        );
        
        // 保存分析结果到状态
        state.getMetadata().put("department", department);
        state.getMetadata().put("doctors", doctors);
        state.setCurrentNode("analyzing");
        
        return state;
    }
}
```

**推荐节点 - RecommendingNode**：

```java
@Component
@RequiredArgsConstructor
public class RecommendingNode implements Node<AgentState> {
    
    private final MedicalAssistant aiService;
    
    @Override
    public AgentState process(AgentState state) {
        PatientInfo patientInfo = state.getPatientInfo();
        List<Doctor> doctors = (List<Doctor>) state.getMetadata().get("doctors");
        
        // 使用 LangChain4j AiService 生成推荐
        String recommendation = aiService.recommendDoctor(
            patientInfo,
            doctors
        );
        
        String medicineAdvice = aiService.recommendMedicine(
            patientInfo
        );
        
        String finalResponse = String.format("""
            %s
            
            %s
            
            ⚠️ 免责声明：本建议仅供参考，不能替代专业医疗诊断。
            """, recommendation, medicineAdvice);
        
        state.setLastAiResponse(finalResponse);
        state.setCurrentNode("recommending");
        state.setComplete(true);
        
        return state;
    }
}
```

### 3️⃣ LangChain4j 服务定义

**AI Service 接口**：

```java
@Service
public interface MedicalAssistant {
    
    @SystemMessage("""
        你是一个专业的医疗助手。
        根据患者信息，生成友好的追问以收集更多必要信息。
        必须询问的信息：年龄、性别、主要症状、持续时间。
        """)
    String generateFollowUpQuestion(
        @V("patientInfo") PatientInfo patientInfo,
        @MemoryId ChatMemory chatMemory
    );
    
    @SystemMessage("""
        根据患者信息和可选医生列表，推荐最合适的医生。
        请以友好的方式解释推荐理由。
        """)
    String recommendDoctor(
        @V("patientInfo") PatientInfo patientInfo,
        @V("doctors") List<Doctor> doctors
    );
    
    @SystemMessage("""
        根据症状推荐非处方药物。
        包含：药物名称、用法用量、注意事项。
        """)
    String recommendMedicine(
        @V("patientInfo") PatientInfo patientInfo
    );
}
```

**工具函数定义**：

```java
@Component
public class MedicalTools {
    
    @Tool("从对话中提取患者信息，返回 PatientInfo 对象")
    public PatientInfo extractPatientInfo(
        @P("用户消息") String message,
        @P("已有信息") PatientInfo existing
    ) {
        // 使用 AI 提取
        // 实际实现可能需要调用另一个 AI Service
        return PatientInfo.builder()
            .age(extractAge(message, existing))
            .gender(extractGender(message, existing))
            .mainSymptom(extractSymptom(message, existing))
            .duration(extractDuration(message, existing))
            .build();
    }
    
    @Tool("根据症状推荐科室")
    public String recommendDepartment(@P("症状") String symptom) {
        Map<String, String> mapping = Map.of(
            "头痛", "神经内科",
            "发烧", "内科",
            "咳嗽", "呼吸科",
            "胃痛", "消化内科"
        );
        return mapping.getOrDefault(symptom, "内科");
    }
    
    @Tool("搜索医生")
    public List<Doctor> searchDoctors(
        @P("科室") String department,
        @P("专长") String specialty
    ) {
        // 从数据库或 JSON 文件查询
        return doctorRepository.findByDepartmentAndSpecialty(
            department, 
            specialty
        );
    }
    
    @Tool("检查信息完整度")
    public boolean checkCompleteness(@P("患者信息") PatientInfo info) {
        return info.getAge() != null &&
               info.getGender() != null &&
               info.getMainSymptom() != null &&
               info.getDuration() != null;
    }
}
```

### 4️⃣ 配置 LangChain4j

```java
@Configuration
public class LangChainConfig {
    
    @Value("${langchain.openai.api-key}")
    private String apiKey;
    
    // 配置 ChatLanguageModel
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4")
            .temperature(0.7)
            .timeout(Duration.ofSeconds(30))
            .build();
    }
    
    // 配置 ChatMemory Provider
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return sessionId -> MessageWindowChatMemory.builder()
            .id(sessionId)
            .maxMessages(20)
            .build();
    }
    
    // 配置 AI Service
    @Bean
    public MedicalAssistant medicalAssistant(
        ChatLanguageModel chatModel,
        ChatMemoryProvider memoryProvider,
        MedicalTools tools
    ) {
        return AiServices.builder(MedicalAssistant.class)
            .chatLanguageModel(chatModel)
            .chatMemoryProvider(memoryProvider)
            .tools(tools)
            .build();
    }
    
    // 配置信息提取服务
    @Bean
    public InfoExtractionService infoExtractionService(
        ChatLanguageModel chatModel
    ) {
        return AiServices.builder(InfoExtractionService.class)
            .chatLanguageModel(chatModel)
            .build();
    }
}
```

### 5️⃣ 执行状态图

**GraphExecutor 服务**：

```java
@Service
@RequiredArgsConstructor
public class GraphExecutor {
    
    private final StateGraph<AgentState> medicalAssistantGraph;
    private final StateStore stateStore;
    
    public String processMessage(String sessionId, String userMessage) {
        // 1. 获取或创建状态
        AgentState state = stateStore.getOrCreate(sessionId);
        
        // 2. 添加用户消息到状态
        state.getMessages().add(new UserMessage(userMessage));
        
        // 3. 执行状态图
        AgentState resultState = medicalAssistantGraph.invoke(state);
        
        // 4. 保存状态
        stateStore.save(sessionId, resultState);
        
        // 5. 返回 AI 响应
        return resultState.getLastAiResponse();
    }
    
    public Stream<String> processMessageStream(String sessionId, String userMessage) {
        // 流式处理
        AgentState state = stateStore.getOrCreate(sessionId);
        state.getMessages().add(new UserMessage(userMessage));
        
        return medicalAssistantGraph.stream(state)
            .map(AgentState::getLastAiResponse);
    }
}
```

---

## 📦 Maven 依赖配置

```xml
<properties>
    <java.version>17</java.version>
    <spring.boot.version>3.2.0</spring.boot.version>
    <langchain4j.version>0.35.0</langchain4j.version>
    <langgraph4j.version>1.0.0</langgraph4j.version>
</properties>

<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- LangChain4j 核心 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangChain4j Spring Boot Starter -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangChain4j OpenAI -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-open-ai</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangGraph4j 核心 -->
    <dependency>
        <groupId>io.github.bsorrentino</groupId>
        <artifactId>langgraph4j-core</artifactId>
        <version>${langgraph4j.version}</version>
    </dependency>
    
    <!-- LangGraph4j 与 LangChain4j 集成 -->
    <dependency>
        <groupId>io.github.bsorrentino</groupId>
        <artifactId>langgraph4j-langchain4j</artifactId>
        <version>${langgraph4j.version}</version>
    </dependency>
    
    <!-- Redis (状态持久化) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 🎯 集成最佳实践

### 1. 分层设计原则

```
┌──────────────────────────────────────┐
│  LangGraph4j Layer (状态编排)         │  ← 负责工作流控制
├──────────────────────────────────────┤
│  LangChain4j Layer (LLM 能力)         │  ← 负责 AI 能力
├──────────────────────────────────────┤
│  Business Logic Layer (业务逻辑)      │  ← 负责业务规则
└──────────────────────────────────────┘
```

**关键点**：
- LangGraph4j 不应包含业务逻辑，只负责状态流转
- LangChain4j 提供原子化的 AI 能力
- 业务逻辑在 Service 层实现

### 2. 状态设计原则

```java
// ❌ 错误：状态过于复杂
public class BadState {
    private String session;
    private List<Message> messages;
    private PatientInfo info;
    private List<Doctor> doctors;
    private List<Medicine> medicines;
    private Map<String, Object> cache;
    // ... 太多字段
}

// ✅ 正确：状态简洁，职责明确
public class GoodState {
    private String sessionId;          // 会话标识
    private String currentNode;        // 当前节点
    private PatientInfo patientInfo;   // 核心业务数据
    private ChatMemory chatMemory;     // 对话记忆（引用）
    private Map<String, Object> metadata; // 扩展数据
}
```

### 3. 节点设计原则

**单一职责**：每个节点只做一件事

```java
// ✅ 好的设计
class CollectingNode implements Node<AgentState> {
    // 只负责信息收集
}

class AnalyzingNode implements Node<AgentState> {
    // 只负责信息分析
}

class RecommendingNode implements Node<AgentState> {
    // 只负责生成推荐
}
```

### 4. 错误处理

```java
@Component
public class RobustNode implements Node<AgentState> {
    
    @Override
    public AgentState process(AgentState state) {
        try {
            // 正常处理逻辑
            return processNormally(state);
        } catch (LLMException e) {
            // LLM 调用失败，降级处理
            return fallbackResponse(state, e);
        } catch (Exception e) {
            // 其他异常，记录日志
            log.error("Node processing failed", e);
            return errorResponse(state, e);
        }
    }
    
    private AgentState fallbackResponse(AgentState state, Exception e) {
        state.setLastAiResponse("抱歉，我暂时无法处理您的请求，请稍后重试。");
        return state;
    }
}
```

### 5. 性能优化

**并行处理**：

```java
@Component
public class ParallelRecommendingNode implements Node<AgentState> {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    
    @Override
    public AgentState process(AgentState state) {
        PatientInfo info = state.getPatientInfo();
        
        // 并行执行多个任务
        CompletableFuture<String> doctorFuture = CompletableFuture.supplyAsync(
            () -> aiService.recommendDoctor(info), executor
        );
        
        CompletableFuture<String> medicineFuture = CompletableFuture.supplyAsync(
            () -> aiService.recommendMedicine(info), executor
        );
        
        CompletableFuture<String> adviceFuture = CompletableFuture.supplyAsync(
            () -> aiService.generateHealthAdvice(info), executor
        );
        
        // 等待所有任务完成
        CompletableFuture.allOf(doctorFuture, medicineFuture, adviceFuture).join();
        
        String finalResponse = String.format("""
            %s
            
            %s
            
            %s
            """, 
            doctorFuture.join(),
            medicineFuture.join(),
            adviceFuture.join()
        );
        
        state.setLastAiResponse(finalResponse);
        return state;
    }
}
```

---

## 🔄 完整流程示例

### 用户对话流程

```
用户: "我头痛，已经三天了"
  ↓
[LangGraph4j] 接收消息 → 进入 CollectingNode
  ↓
[LangChain4j] extractPatientInfo() → 提取 {symptom: "头痛", duration: "三天"}
  ↓
[LangChain4j] checkCompleteness() → 返回 false（缺少年龄、性别）
  ↓
[LangChain4j] generateFollowUpQuestion() → "请问您的年龄和性别是？"
  ↓
[LangGraph4j] 更新状态 → 保持在 CollectingNode
  ↓
返回给用户: "请问您的年龄和性别是？"

---

用户: "我今年30岁，男性"
  ↓
[LangGraph4j] 接收消息 → 仍在 CollectingNode
  ↓
[LangChain4j] extractPatientInfo() → 更新 {age: 30, gender: "男", symptom: "头痛", duration: "三天"}
  ↓
[LangChain4j] checkCompleteness() → 返回 true
  ↓
[LangGraph4j] 条件路由 → 转到 AnalyzingNode
  ↓
[LangChain4j Tools] recommendDepartment("头痛") → "神经内科"
[LangChain4j Tools] searchDoctors("神经内科") → [Doctor1, Doctor2]
  ↓
[LangGraph4j] 转到 RecommendingNode
  ↓
[LangChain4j] recommendDoctor() → 生成推荐文本
[LangChain4j] recommendMedicine() → 推荐非处方药
  ↓
[LangGraph4j] 转到 END 状态
  ↓
返回给用户: 完整的推荐信息
```

---

## 📊 项目结构（集成版）

```
healthCare/
├── src/main/java/com/yihu/agent/
│   ├── HealthCareApplication.java
│   │
│   ├── config/
│   │   ├── LangChainConfig.java       # LangChain4j 配置
│   │   ├── LangGraphConfig.java       # LangGraph4j 配置
│   │   └── WebSocketConfig.java
│   │
│   ├── graph/                         # LangGraph4j 层
│   │   ├── GraphConfig.java           # 状态图定义
│   │   ├── GraphExecutor.java         # 状态图执行器
│   │   │
│   │   ├── state/
│   │   │   ├── AgentState.java        # 状态类
│   │   │   └── StateStore.java        # 状态持久化
│   │   │
│   │   └── nodes/                     # 节点实现
│   │       ├── GreetingNode.java
│   │       ├── CollectingNode.java
│   │       ├── AnalyzingNode.java
│   │       └── RecommendingNode.java
│   │
│   ├── langchain/                     # LangChain4j 层
│   │   ├── services/
│   │   │   ├── MedicalAssistant.java  # AI Service 接口
│   │   │   ├── InfoExtractionService.java
│   │   │   └── RecommendationService.java
│   │   │
│   │   ├── tools/
│   │   │   └── MedicalTools.java      # @Tool 方法集合
│   │   │
│   │   └── memory/
│   │       └── ChatMemoryService.java # 对话记忆管理
│   │
│   ├── service/                       # 业务服务层
│   │   ├── ConversationService.java   # 对话协调服务
│   │   ├── DoctorService.java
│   │   └── MedicineService.java
│   │
│   ├── model/
│   │   ├── dto/
│   │   │   ├── PatientInfo.java
│   │   │   ├── ChatMessage.java
│   │   │   └── RecommendationResult.java
│   │   └── entity/
│   │       ├── Doctor.java
│   │       └── Medicine.java
│   │
│   ├── repository/
│   │   ├── DoctorRepository.java
│   │   └── MedicineRepository.java
│   │
│   └── websocket/
│       ├── ChatWebSocketHandler.java
│       └── SessionManager.java
│
└── src/main/resources/
    ├── application.yml
    ├── prompts/
    │   ├── greeting.txt
    │   ├── info-collection.txt
    │   └── recommendation.txt
    └── data/
        ├── doctors.json
        └── medicines.json
```

---

## 🎓 总结

### 集成核心要点

1. **LangGraph4j 负责"流程"** - 状态管理、流程控制、条件路由
2. **LangChain4j 负责"能力"** - LLM 调用、工具集成、记忆管理
3. **两者通过 Node 集成** - 在节点中调用 LangChain4j 服务
4. **状态是连接桥梁** - AgentState 在两者之间传递数据

### 何时使用这种集成？

✅ **适合场景**：
- 多步骤的复杂工作流
- 需要条件分支和循环
- 多 Agent 协同工作
- 需要状态持久化和恢复

❌ **不适合场景**：
- 简单的一问一答
- 无状态的 API 调用
- 原型验证（MVP）阶段

### 下一步建议

对于你的医疗助手项目：
1. **第一阶段**：使用简化版（不集成 LangGraph4j），快速验证业务逻辑
2. **第二阶段**：当状态管理变复杂时，引入 LangGraph4j
3. **第三阶段**：优化性能，添加更多 Agent 和工具

**现在可以根据这个方案开始编码了！** 🚀


