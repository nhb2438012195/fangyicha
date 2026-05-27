package com.fangyicha.service;

import java.util.Map;

/**
 * Function Calling 服务接口
 */
public interface FunctionCallService {

    /**
     * 执行函数调用
     */
    FunctionResult execute(String functionName, Map<String, Object> arguments, Long customerId);

    /**
     * 函数调用结果
     */
    class FunctionResult {
        private boolean success;
        private String message;
        private Map<String, Object> data;

        public FunctionResult() {}

        public FunctionResult(boolean success, String message, Map<String, Object> data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}
