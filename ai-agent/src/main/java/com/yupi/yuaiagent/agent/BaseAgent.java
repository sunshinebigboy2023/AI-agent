package com.yupi.yuaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaiagent.agent.model.AgentSseEvent;
import com.yupi.yuaiagent.agent.model.AgentState;
import com.yupi.yuaiagent.agent.model.AgentStepPhase;
import com.yupi.yuaiagent.agent.model.AgentStepRecord;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM 大模型
    private ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    // 执行轨迹
    private List<AgentStepRecord> stepRecords = new ArrayList<>();

    // 工具结果最大长度
    private int maxObservationLength = 4000;

    // 用户原始语言偏好
    private String preferredResponseLanguage = "与用户问题相同的语言";

    private transient Consumer<AgentSseEvent> eventConsumer = event -> {};

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态
        this.state = AgentState.RUNNING;
        this.stepRecords = new ArrayList<>();
        this.preferredResponseLanguage = detectPreferredResponseLanguage(userPrompt);
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    // 单步执行
                    String stepResult = step();
                    if (StrUtil.isNotBlank(stepResult)) {
                        String result = "Step " + stepNumber + ": " + stepResult;
                        results.add(result);
                    }
                }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                String terminatedMessage = "Terminated: Reached max steps (" + maxSteps + ")";
                results.add(terminatedMessage);
                recordStep(stepNumber(), AgentStepPhase.FINAL, null, null, terminatedMessage, null, null);
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            recordStep(stepNumber(), AgentStepPhase.ERROR, null, null, null, null, abbreviate(e.getMessage()));
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        return runInternal(userPrompt, false);
    }

    public SseEmitter runStructuredStream(String userPrompt) {
        return runInternal(userPrompt, true);
    }

    private SseEmitter runInternal(String userPrompt, boolean structured) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 1、基础校验
            try {
            this.stepRecords = new ArrayList<>();
            this.eventConsumer = event -> sendSseEvent(sseEmitter, event, structured);
            this.preferredResponseLanguage = detectPreferredResponseLanguage(userPrompt);
            if (this.state != AgentState.IDLE) {
                    sendError("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sendError("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
            // 2、执行，更改状态
            this.state = AgentState.RUNNING;
            emitEvent("message", java.util.Map.of("content", abbreviate(userPrompt), "role", "user"));
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));
            // 保存结果列表
            List<String> results = new ArrayList<>();
            try {
                // 执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    // 单步执行
                    String stepResult = step();
                    if (StrUtil.isNotBlank(stepResult)) {
                        String result = "Step " + stepNumber + ": " + stepResult;
                        results.add(result);
                        // 输出当前每一步的结果到 SSE
                        if (!structured) {
                            sseEmitter.send(result);
                        } else {
                            emitEvent("message", java.util.Map.of("content", stepResult));
                        }
                    }
                }
                // 检查是否超出步骤限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    String terminatedMessage = "执行结束：达到最大步骤（" + maxSteps + "）";
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                    recordStep(stepNumber(), AgentStepPhase.FINAL, null, null, terminatedMessage, null, null);
                    emitEvent("done", java.util.Map.of("message", terminatedMessage, "steps", stepRecords));
                    if (!structured) {
                        sseEmitter.send(terminatedMessage);
                    }
                } else {
                    emitEvent("done", java.util.Map.of("message", "执行完成", "steps", stepRecords));
                }
                // 正常完成
                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                recordStep(stepNumber(), AgentStepPhase.ERROR, null, null, null, null, abbreviate(e.getMessage()));
                try {
                    sendError("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3、清理资源
                this.cleanup();
                this.eventConsumer = event -> {};
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
        this.messageList = new ArrayList<>();
        this.currentStep = 0;
        this.preferredResponseLanguage = "与用户问题相同的语言";
        if (this.state != AgentState.ERROR) {
            this.state = AgentState.IDLE;
        }
    }

    protected void recordStep(int stepNumber, AgentStepPhase phase, String toolName, String toolArguments,
                              String observation, String modelOutput, String errorMessage) {
        long now = System.currentTimeMillis();
        AgentStepRecord record = AgentStepRecord.builder()
                .stepNumber(stepNumber)
                .phase(phase)
                .toolName(toolName)
                .toolArguments(abbreviate(toolArguments))
                .observation(abbreviate(observation))
                .modelOutput(abbreviate(modelOutput))
                .errorMessage(abbreviate(errorMessage))
                .startTime(now)
                .endTime(now)
                .durationMs(0)
                .build();
        stepRecords.add(record);
        if (phase == AgentStepPhase.ACT) {
            emitEvent("tool_call", record);
        } else if (phase == AgentStepPhase.OBSERVATION) {
            emitEvent("tool_result", record);
        } else if (phase == AgentStepPhase.ERROR) {
            emitEvent("error", record);
        } else {
            emitEvent("step", record);
        }
    }

    protected String abbreviate(String content) {
        if (content == null) {
            return null;
        }
        String sanitized = content
                .replaceAll("(?i)(api[-_ ]?key|authorization|bearer)\\s*[:=]\\s*\\S+", "$1=***")
                .trim();
        if (sanitized.length() <= maxObservationLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxObservationLength) + "\n[内容过长，已截断]";
    }

    protected String getPreferredResponseLanguageInstruction() {
        return preferredResponseLanguage;
    }

    private String detectPreferredResponseLanguage(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return "与用户问题相同的语言";
        }
        if (userPrompt.matches(".*[\\u4e00-\\u9fff].*")) {
            return "请使用简体中文回答，并把英文工具结果整理成中文总结";
        }
        return "Please answer in the same language as the user";
    }

    private int stepNumber() {
        return currentStep > 0 ? currentStep : 1;
    }

    private void emitEvent(String eventName, Object payload) {
        eventConsumer.accept(new AgentSseEvent(eventName, payload));
    }

    private void sendError(String message) throws IOException {
        emitEvent("error", java.util.Map.of("message", abbreviate(message)));
    }

    private void sendSseEvent(SseEmitter sseEmitter, AgentSseEvent event, boolean structured) {
        try {
            if (structured) {
                sseEmitter.send(SseEmitter.event().name(event.event()).data(event.data()));
            } else if ("message".equals(event.event())) {
                Object data = event.data();
                if (data instanceof java.util.Map<?, ?> map) {
                    Object content = map.get("content");
                    if (content != null) {
                        sseEmitter.send(content.toString());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
