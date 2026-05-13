package com.yupi.yuaiagent.controller;

public record ApiError(
        long timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
