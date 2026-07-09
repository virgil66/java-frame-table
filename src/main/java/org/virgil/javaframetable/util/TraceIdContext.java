package org.virgil.javaframetable.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * @ProjectName : java-frame-table
 * @ClassName : TraceIdContext
 * @Description : 链路追踪上下文工具类，存储TraceID
 * @Date : 2026/1/29 16:42
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
public class TraceIdContext {
	// ThreadLocal 存储当前线程的TraceID
	private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

	// 默认未知TraceId值
	private static final String UNKNOWN_TRACE_ID = "UNKNOWN";

	// TraceID的MDC key（与日志配置中的%X{traceId}对应）
	private static final String TRACE_ID_KEY = "traceId";

	/**
	 * 私有构造函数，防止通过反射实例化
	 */
	private TraceIdContext() {
		// 防止通过反射实例化
		throw new UnsupportedOperationException("Utility class should not be instantiated");
	}

	/**
	 * @Name        : setTraceId
	 * @Description : 生成并设置TraceID（UUID随机生成）
	 * @Param       : null
	 * @Return      : void
	 * @Date        : 2026/1/29 16:42
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static void setTraceId() {
		String traceId = UUID.randomUUID().toString().replace("-", "");
		// TRACE_ID_HOLDER.set(traceId);
		MDC.put(TRACE_ID_KEY, traceId);
	}

	/**
	 * @Name        : setTraceId
	 * @Description : 设置TraceID（手动传递，比如跨服务调用时传递）
	 * @Param       : traceId - 手动传递的TraceID
	 * @Return      : void
	 * @Date        : 2026/1/29 16:42
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static void setTraceId(String traceId) {
		if (StringUtils.isNotEmpty(traceId)) {
			// TRACE_ID_HOLDER.set(traceId);
			MDC.put(TRACE_ID_KEY, traceId);
		}
	}

	/**
	 * @Name        : getTraceId
	 * @Description : 获取当前线程的TraceID
	 * @Param       : null
	 * @Return      : String - 当前线程的TraceID
	 * @Date        : 2026/1/29 16:42
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static String getTraceId() {
		// String traceId = TRACE_ID_HOLDER.get();
		String traceId = MDC.get(TRACE_ID_KEY);
		// 若未生成，返回默认值（避免空指针）
		return traceId != null ? traceId : UNKNOWN_TRACE_ID;
	}

	/**
	 * @Name        : clear
	 * @Description : 清除ThreadLocal中的TraceID（必须，避免内存泄漏）
	 * @Param       : null
	 * @Return      : void
	 * @Date        : 2026/1/29 16:42
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static void clear() {
		// TRACE_ID_HOLDER.remove();
		MDC.remove(TRACE_ID_KEY);
	}
}
