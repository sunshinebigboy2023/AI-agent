package com.yupi.yuaiagent.rag;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class KnowledgeClientIdSupport {

    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    public String requireValidClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalArgumentException("缺少 X-Client-Id");
        }
        if (!CLIENT_ID_PATTERN.matcher(clientId).matches()) {
            throw new IllegalArgumentException("X-Client-Id 非法，只允许 8 到 64 位字母、数字、短横线和下划线");
        }
        return clientId;
    }
}
