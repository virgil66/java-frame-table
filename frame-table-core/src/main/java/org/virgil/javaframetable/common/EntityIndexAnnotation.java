package org.virgil.javaframetable.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ProjectName : java-frame-table
 * @ClassName : EntityIndexAnnotation
 * @Description : 实体索引注解
 * @Date : 2026/7/9 22:51
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Target(ElementType.TYPE) // 作用于类
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
public @interface EntityIndexAnnotation {
	/**
	 * 唯一索引字段
	 * @return 默认为空
	 */
	String[] ux() default {};

	/**
	 * 普通索引字段
	 * @return 默认为空
	 */
	String[] ix() default {};
}
