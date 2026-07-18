package org.virgil.javaframetable.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * @ProjectName : java-frame-table
 * @ClassName : MyBatisPlusConfig
 * @Description : MyBatisPlus 配置类
 * @Date : 2026/7/10 01:03
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {
	private static final String SYSTEM_USER = "SYSTEM";

	@Override
	public void insertFill(MetaObject metaObject) {
		this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
		this.strictInsertFill(metaObject, "createName", String.class, SYSTEM_USER);
		this.strictInsertFill(metaObject, "createCode", String.class, SYSTEM_USER);
		this.strictInsertFill(metaObject, "modifyTime", LocalDateTime.class, LocalDateTime.now());
		this.strictInsertFill(metaObject, "modifyName", String.class, SYSTEM_USER);
		this.strictInsertFill(metaObject, "modifyCode", String.class, SYSTEM_USER);
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		this.strictUpdateFill(metaObject, "modifyTime", LocalDateTime.class, LocalDateTime.now());
		this.strictUpdateFill(metaObject, "modifyName", String.class, SYSTEM_USER);
		this.strictUpdateFill(metaObject, "modifyCode", String.class, SYSTEM_USER);
	}
}
