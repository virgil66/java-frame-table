package org.virgil.exec.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ProjectName : java-frame-table
 * @ClassName : HelloController
 * @Description : 测试控制器
 * @Date : 2026/1/29 14:03
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Slf4j
// 注解说明：@RestController = @Controller + @ResponseBody，返回 JSON/字符串而非页面
@RestController
@RequestMapping("/hello")
public class HelloController {
	// 定义 GET 请求接口，路径为 /hello
	// 注解说明：@GetMapping = @RequestMapping(method = RequestMethod.GET)，处理 GET 请求
	@GetMapping("/sayHello")
	public String hello(String param) {
		log.info("接收请求参数：{}", param);
		String result = param;
		log.info("处理结果：{}", result);
		return result;
	}
}
