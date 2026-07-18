package org.virgil.javaframetable.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.virgil.javaframetable.util.TraceIdContext;

/**
 * @ProjectName : fortec-tech-business-matr
 * @ClassName : TraceIdInterceptor
 * @Description : 链路追踪拦截器：为每个请求生成TraceID
 * @Date : 2026/1/29 16:59
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Slf4j
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
	/**
	 * @Name        : preHandle
	 * @Description : 请求处理前：生成TraceID
	 * @Param       : request - 请求对象
	 * @Param       : response - 响应对象
	 * @Param       : handler - 处理器对象
	 * @Return      : boolean - 是否继续处理请求
	 * @Date        : 2026/1/29 17:30
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		// 生成TraceID并存入上下文
		TraceIdContext.setTraceId();
		String traceId = TraceIdContext.getTraceId();

		// 记录请求入口日志（包含TraceID、请求路径、方法等）
		log.info("===== 请求入口 | TraceID: {} | 请求路径: {} | 请求方法: {} | 客户端IP: {} =====",
				traceId,
				request.getRequestURI(),
				request.getMethod(),
				getClientIpAddress(request));

		// 将TraceID放入响应头，方便前端获取（可选）
		response.setHeader("X-Trace-ID", traceId);
		return true;
	}

	/**
	 * @Name        : afterCompletion
	 * @Description : 请求处理后：清理TraceID
	 * @Param       : request - 请求对象
	 * @Param       : response - 响应对象
	 * @Param       : handler - 处理器对象
	 * @Return      : void
	 * @Date        : 2026/1/29 17:30
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		try {
			String traceId = TraceIdContext.getTraceId();

			// 记录请求出口日志
			if (ex != null) {
				log.info("===== 请求出口 | TraceID: {} | 响应状态码: {} | 异常信息: {} =====", traceId, response.getStatus(), ex.getMessage());
			} else {
				log.info("===== 请求出口 | TraceID: {} | 响应状态码: {} =====", traceId, response.getStatus());
			}
		} finally {
			// 必须清理ThreadLocal，避免内存泄漏
			TraceIdContext.clear();
		}
	}

	/**
	 * @Name        : getClientIpAddress
	 * @Description : 获取客户端IP地址（支持X-Forwarded-For和X-Real-IP）
	 * @Param       : request - 请求对象
	 * @Return      : String - 客户端IP地址
	 * @Date        : 2026/1/29 17:47
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String getClientIpAddress(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
			// X-Forwarded-For可能包含多个IP地址，取第一个
			int index = xForwardedFor.indexOf(",");
			if (index > 0) {
				return xForwardedFor.substring(0, index).trim();
			}
			return xForwardedFor.trim();
		}

		String xRealIp = request.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
			return xRealIp;
		}

		String remoteAddr = request.getRemoteAddr();
		// 将本地回环地址转换为更友好的格式
		if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
			return "127.0.0.1";
		}

		return remoteAddr;
	}
}
