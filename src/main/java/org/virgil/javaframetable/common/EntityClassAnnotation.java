package org.virgil.javaframetable.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ProjectName : java-frame-table
 * @ClassName : EntityClassAnnotation
 * @Description : 实体类注解
 * @Date : 2026/7/9 22:46
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Target(ElementType.TYPE) // 作用于类
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
public @interface EntityClassAnnotation {
	/**
	 * 表备注
	 * @return 默认为空
	 */
	String remark() default "";
}
