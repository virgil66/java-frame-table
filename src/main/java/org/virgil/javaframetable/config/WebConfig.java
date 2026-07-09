package org.virgil.javaframetable.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.virgil.javaframetable.interceptor.TraceIdInterceptor;

/**
 * @ProjectName : fortec-tech-business-matr
 * @ClassName : WebConfig
 * @Description : Web配置类：注册拦截器
 * @Date : 2026/1/29 17:52
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Resource
	private TraceIdInterceptor traceIdInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册TraceID拦截器，拦截所有请求（/**）
		registry.addInterceptor(traceIdInterceptor)
				// 拦截所有路径
				.addPathPatterns("/**")
				// 排除错误页面路径（可选）
				.excludePathPatterns(
						"/error",
						"/static/**",
						"/public/**",
						"/assets/**",
						"/favicon.ico",
						"/actuator/**"
				);
	}
}
