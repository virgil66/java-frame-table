package org.virgil.javaframetable.util;

import org.virgil.javaframetable.common.EntityFieldAnnotation;
import org.virgil.javaframetable.enums.EntityFieldType;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @ProjectName : java-frame-table
 * @ClassName : EntityFieldParser
 * @Description : 解析实体类字段
 * @Date : 2026/7/9 22:55
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
public class EntityFieldParser {
	/**
	 * @Name        : parseEntityFields
	 * @Description : 解析实体字段
	 * @Param       : entityClass 实体类
	 * @Return      : List<String> 字段定义列表
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static List<String> parseEntityFields(Class<?> entityClass) {
		List<String> fieldDefinitions = new ArrayList<>();
		List<String> primaryKeyFields = new ArrayList<>();

		Field[] fields = getAllFields(entityClass).toArray(new Field[0]);
		for (Field field : fields) {
			if (field.isAnnotationPresent(EntityFieldAnnotation.class)) {
				EntityFieldAnnotation annotation = field.getAnnotation(EntityFieldAnnotation.class);

				// 获取字段名
				String fieldName = StringUtils.camelToUnderscore(field.getName());

				// 获取字段类型（优先使用注解中的 type，否则使用 Java 类型）
				String fieldType = getSqlTypeFromAnnotation(annotation, field.getType());

				// 只有 VARCHAR 和 DECIMAL 才支持长度参数
				boolean supportsLength = "VARCHAR".equals(fieldType) || "DECIMAL".equals(fieldType);
				// 获取字段长度（如果注解中有 length 属性）
				String length = supportsLength && !annotation.length().isEmpty() ? "(" + annotation.length() + ")" : "";

				// 是否允许为空
				boolean nullable = !annotation.not_null();

				// 构造字段定义
				StringBuilder columnDefinition = new StringBuilder();
				columnDefinition.append(fieldName)
						.append(" ")
						.append(fieldType)
						.append(length);

				// 处理主键
//				if (field.isAnnotationPresent(TableId.class)) {
//					TableId tableId = field.getAnnotation(TableId.class);
//					if (tableId.type() == IdType.AUTO) {
//						columnDefinition.append(" AUTO_INCREMENT");
//					}
//					// 记录主键字段
//					primaryKeyFields.add(fieldName);
//				}

				if (!nullable) {
					columnDefinition.append(" NOT NULL");
				}

				// 设置默认值（如果有）
				if (!annotation.default_value().isEmpty()) {
					columnDefinition.append(" DEFAULT '").append(annotation.default_value()).append("'");
				}

				// 字段名称（text）和描述信息（comment）
				StringBuilder commentBuilder = new StringBuilder();
				if (!annotation.text().isEmpty()) {
					commentBuilder.append(annotation.text());
				}
				if (!annotation.comment().isEmpty()) {
					if (!commentBuilder.isEmpty()) {
						commentBuilder.append(" | ");
					}
					commentBuilder.append(annotation.comment());
				}
				if (!commentBuilder.isEmpty()) {
					columnDefinition.append(" COMMENT '").append(commentBuilder.toString()).append("'");
				}

				// 检查是否为主键
				if (annotation.pk()) {
					// 记录主键字段
					primaryKeyFields.add(fieldName);
				}

				fieldDefinitions.add(columnDefinition.toString());
			}
		}

		// 如果存在主键字段，追加 PRIMARY KEY 定义
		if (!primaryKeyFields.isEmpty()) {
			StringBuilder primaryKeyDefinition = new StringBuilder();
			primaryKeyDefinition.append("PRIMARY KEY (");
			for (int i = 0; i < primaryKeyFields.size(); i++) {
				primaryKeyDefinition.append(primaryKeyFields.get(i));
				if (i < primaryKeyFields.size() - 1) {
					primaryKeyDefinition.append(", ");
				}
			}
			primaryKeyDefinition.append(")");
			fieldDefinitions.add(primaryKeyDefinition.toString());
		}

		return fieldDefinitions;
	}

	/**
	 * @Name        : getAllFields
	 * @Description : 获取类的所有字段（包括父类）
	 * @Param       : clazz 类
	 * @Return      : List<Field> 字段列表
	 * @Date        : 2026/7/10
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private static List<Field> getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		while (clazz != null && clazz != Object.class) {
			fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	/**
	 * @Name        : getSqlTypeFromAnnotation
	 * @Description : 根据注解获取 SQL 类型
	 * @Param       : annotation 注解
	 * @Param       : javaType Java 类型
	 * @Return      : String SQL 类型
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static String getSqlTypeFromAnnotation(EntityFieldAnnotation annotation, Class<?> javaType) {
		// 优先使用注解中的 type 属性
		if (annotation.type() != EntityFieldType.DEFAULT) {
			switch (annotation.type()) {
				case LONG:
					return "BIGINT";
				case TINYINT:
					return "TINYINT";
				case VARCHAR:
					return "VARCHAR";
				case INT:
					return "INT";
				case BOOLEAN:
					return "TINYINT(1)";
				case DOUBLE:
					return "DOUBLE";
				case DECIMAL:
					return "DECIMAL";
				case DATETIME:
					return "DATETIME";
				case DATE:
					return "DATE";
				case TEXT:
					return "TEXT";
				default:
					break;
			}
		}

		// 否则根据 Java 类型推断
		if (javaType == String.class) {
			return "VARCHAR";
		}
		if (javaType == Integer.class || javaType == int.class) {
			return "INT";
		}
		if (javaType == Long.class || javaType == long.class) {
			return "BIGINT";
		}
		if (javaType == Boolean.class || javaType == boolean.class) {
			return "BOOLEAN";
		}
		if (javaType == Double.class || javaType == double.class) {
			return "DOUBLE";
		}
		if (javaType == java.util.Date.class || javaType == LocalDateTime.class) {
			return "DATETIME";
		}
		if (javaType == java.time.LocalDate.class) {
			return "DATE";
		}
		if (javaType == java.math.BigDecimal.class) {
			return "DECIMAL";
		}

		// 默认类型
		return "TEXT";
	}
}
