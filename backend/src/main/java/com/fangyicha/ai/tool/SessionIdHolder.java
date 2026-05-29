package com.fangyicha.ai.tool;

public final class SessionIdHolder {
    private static final ThreadLocal<Long> SESSION_ID = new ThreadLocal<>();
    private SessionIdHolder() {}
    public static void set(Long sessionId) { SESSION_ID.set(sessionId); }
    public static Long get() { return SESSION_ID.get(); }
    public static void clear() { SESSION_ID.remove(); }
}
