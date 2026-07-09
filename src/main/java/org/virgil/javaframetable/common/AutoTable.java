package org.virgil.javaframetable.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ProjectName : java-frame-table
 * @ClassName : AutoTable
 * @Description : 标识实体类是否参与自动建表
 * @Date : 2026/7/9 22:45
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Target(ElementType.TYPE) // 作用于类
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
public @interface AutoTable {
	/**
	 * 是否开启自动建表
	 * @return true: 开启自动建表；false: 关闭自动建表
	 */
	boolean enable() default true;
}
