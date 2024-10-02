# Spring ai 项目, Java快速接入LLM大模型

Spring AI已经上架到Spring Initializr 上，它提供了一种更简洁的方式和AI交互，减轻Java业务中接入LLM模型应用的学习成本，目前在 https://start.spring.io/ 上可以使用并构建。

Spring AI 是一个人工智能工程的应用框架。其目标是将 Spring 生态系统设计原则（例如可移植性和模块化设计）应用于 AI 领域，并推广使用 POJO 作为 AI 领域应用程序的构建块。

# Features
跨 AI 提供商的便携式 API 支持聊天、文本到图像和嵌入模型。支持同步和流 API 选项。还支持配置参数访问特定Model。

### 支持的聊天模型
- OpenAI
- Azure Open AI
- Amazon Bedrock
  - Anthropic's Claude
  - Cohere's Command
  - AI21 Labs' Jurassic-2
  - Meta's LLama 2
  - Amazon's Titan
- Google Vertex AI
- HuggingFace - HuggingFace上的大量模型，例如Llama2
- Ollama - 支持本地无GPU情况下运行AI模型


### 支持的文生图模型
- OpenAI with DALL-E
- StabilityAI


### 支持的向量模型
- OpenAI
- Azure OpenAI
- Ollama
- ONNX
- PostgresML
- Bedrock Cohere
- Bedrock Titan
- Google VertexAI

# 本文以ollama模型为例

## Ollama

Ollama帮助我们在本地的电脑上无需GPU（显卡）资源，也能一键构建大模型，并且提供控制台、RestfulAPI方式快速测试和接入Ollama上的大模型。

## Ollama支持哪些模型？

Ollama官网：https://ollama.com/library

## 运行gemma2

下载：https://ollama.com/download

运行命令
```bash
ollama run gemma2:9b
```
2B参数 `ollama run gemma2:2b`

9B参数 `ollama run gemma2`

27B参数 `ollama run gemma2:27b`

Tips:
`gemma2`是谷歌Meta近期新发布的模型对中文支持比较友好

第一次运行会先下载模型文件（大概5.4G，会比较耗时）

下载完模型资源后会自动启动模型，可以在控制台测试和模型交互。

## 引入依赖
Tips：Spring AI的相关依赖并没有开放在Meven中央仓库，因此需要配置Spring的仓库
```xml
<properties>
    <java.version>22</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
    <spring-ai.version>0.8.0</spring-ai.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

## 配置Ollama模型
修改此项目的`application.yml`配置文件，增加如下：

```yml
spring:
  ai:
    ollama:
      chat:
        model: gemma2
```

## 测试
```java
package com.lifd.monkey;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringBootTest
class MonkeyApplicationTests {

    @Autowired
    private OllamaChatClient chatClient;

    @Test
    void contextLoads() {
        String message = """
                鲁迅和周树人是什么关系？
                """;
        System.out.println(chatClient.call(message));
    }

    /**
     * 流式访问
     */
    @Test
    void streamChat() throws ExecutionException, InterruptedException {
        // 构建一个异步函数，实现手动关闭测试函数
        CompletableFuture<Void> future = new CompletableFuture<>();
        String message = """
                年终总结
                """;
        PromptTemplate promptTemplate = new PromptTemplate("""
                你是一个Java开发工程师，你擅长于写公司年底的工作总结报告，
                根据：{message} 场景写100字的总结报告
                """);
        Prompt prompt = promptTemplate.create(Map.of("message", message));
        chatClient.stream(prompt).subscribe(
                chatResponse -> {
                    System.out.println("response: " + chatResponse.getResult().getOutput().getContent());
                },
                throwable -> {
                    System.err.println("err: " + throwable.getMessage());
                },
                () -> {
                    System.out.println("complete~!");
                    // 关闭函数
                    future.complete(null);
                }
        );
        future.get();
    }

}
```

示例代码：[https://github.com/lifengdi/monkey](https://github.com/lifengdi/monkey)

相关博客：[https://www.lifd.site/tech/springai-xiang-mu-java-kuai-su-jie-ru-llm-da-mo-xing/](https://www.lifd.site/tech/springai-xiang-mu-java-kuai-su-jie-ru-llm-da-mo-xing/)