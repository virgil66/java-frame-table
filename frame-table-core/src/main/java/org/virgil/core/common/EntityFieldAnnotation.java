package org.virgil.core.common;

import org.virgil.core.enums.EntityFieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ProjectName : java-frame-table
 * @ClassName : EntityFieldAnnotation
 * @Description : 实体字段注解
 * @Date : 2026/7/9 22:48
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Target(ElementType.FIELD) // 作用于字段
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
public @interface EntityFieldAnnotation {
	/**
	 * 字段描述
	 * @return 默认为空
	 */
	String text() default "";

	/**
	 * 字段类型
	 * @return 默认为 DEFAULT
	 */
	EntityFieldType type() default EntityFieldType.DEFAULT;

	/**
	 * 字段长度
	 * @return 默认为空
	 */
	String length() default "";

	/**
	 * 是否非空
	 * @return 默认为false，true：非空；false：空
	 */
	boolean not_null() default false;

	/**
	 * 默认值
	 * @return 默认为空
	 */
	String default_value() default "";

	/**
	 * 描述
	 * @return 默认为空
	 */
	String comment() default "";

	/**
	 * 是否主键
	 * @return 默认为false，true：主键；false：非主键
	 */
	boolean pk() default false;
}
