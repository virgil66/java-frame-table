package org.virgil.javaframetable.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ProjectName : java-frame-table
 * @ClassName : StringUtils
 * @Description : 字符串工具类，提供字符串判断方法
 * @Date : 2026/1/29 16:49
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Slf4j
public class StringUtils {
	/**
	 * 私有构造函数，防止通过反射实例化
	 */
	private StringUtils() {
		// 防止通过反射实例化
		throw new UnsupportedOperationException("Utility class should not be instantiated");
	}

	/**
	 * @Name        : isEmpty
	 * @Description : 判断字符串是否为空（null 或者 空字符串 或者 只包含空白字符）
	 * @Param       : str - 要判断的字符串
	 * @Return      : boolean - 如果字符串为空（null 或者 空字符串 或者 只包含空白字符），则返回true；否则返回false
	 * @Date        : 2026/1/29 16:49
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	/**
	 * @Name        : isNotEmpty
	 * @Description : 判断字符串是否不为空（null 或者 空字符串 或者 只包含空白字符）
	 * @Param       : str - 要判断的字符串
	 * @Return      : boolean - 如果字符串不为空（null 或者 空字符串 或者 只包含空白字符），则返回true；否则返回false
	 * @Date        : 2026/1/29 16:49
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	/**
	 * @Name        : isBlank
	 * @Description : 判断字符串是否为纯空白字符（不为null但只包含空白字符）
	 * @Param       : str - 要判断的字符串
	 * @Return      : boolean - 如果字符串不为null但只包含空白字符，则返回true；否则返回false
	 * @Date        : 2026/1/29 16:50
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static boolean isBlank(String str) {
		return str != null && str.trim().isEmpty();
	}

	/**
	 * @Name        : isNotBlank
	 * @Description : 判断字符串是否不为纯空白字符（不为null但只包含空白字符）
	 * @Param       : str - 要判断的字符串
	 * @Return      : boolean - 如果字符串不为null但只包含空白字符，则返回true；否则返回false
	 * @Date        : 2026/1/29 16:50
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	/**
	 * @Name        : defaultIfEmpty
	 * @Description : 如果字符串为null或空字符串，返回默认值
	 * @Param       : str - 要判断的字符串
	 * @Param       : defaultStr - 默认值
	 * @Return      : String - 如果字符串为空，则返回默认字符串；否则返回字符串本身
	 * @Date        : 2026/1/29 16:50
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static String defaultIfEmpty(String str, String defaultStr) {
		return isEmpty(str) ? defaultStr : str;
	}

	/**
	 * @Name        : camelToUnderscore
	 * @Description : 驼峰命名转下划线命名
	 * @Param       : camelCase 驼峰命名字符串
	 * @Return      : String 下划线命名字符串
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static String camelToUnderscore(String camelCase) {
		if (camelCase == null || camelCase.isEmpty()) {
			return camelCase;
		}
		return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}

	/**
	 * @Name        : copyProperties
	 * @Description : 复制对象属性（默认忽略空值）
	 * @Param       : source 源对象
	 * @Param       : target 目标对象
	 * @Return      : void
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static void copyProperties(Object source, Object target) {
		copyProperties(source, target, true);
	}

	/**
	 * @Name        : copyProperties
	 * @Description : 复制对象属性（可选择是否忽略空值）
	 * @Param       : source 源对象
	 * @Param       : target 目标对象
	 * @Param       : ignoreNullValue 是否忽略空值
	 * @Return      : void
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static void copyProperties(Object source, Object target, boolean ignoreNullValue) {
		if (source == null || target == null) {
			throw new IllegalArgumentException("源对象或目标对象不能为空");
		}

		Class<?> sourceClass = source.getClass();
		Class<?> targetClass = target.getClass();

		// 获取字段映射关系（可根据需要扩展为注解或配置文件）
		Map<String, String> fieldMapping = getFieldMapping(sourceClass, targetClass);

		Field[] sourceFields = sourceClass.getDeclaredFields();
		for (Field sourceField : sourceFields) {
			sourceField.setAccessible(true);
			String sourceFieldName = sourceField.getName();

			// 查找目标字段名
			String targetFieldName = fieldMapping.getOrDefault(sourceFieldName, sourceFieldName);
			try {
				Field targetField = null;
				if (sourceFieldName.equals(targetFieldName)) {
					targetField = sourceField;
				} else {
					continue;
				}
				targetField.setAccessible(true);

				// 获取源字段值
				Object value = sourceField.get(source);
				if (ignoreNullValue && value == null) {
					// 忽略空值
					continue;
				}

				// 设置目标字段值（支持类型转换）
				setValue(targetField, target, value);
			} catch (IllegalAccessException e) {
				// 字段不存在或访问异常时跳过
				// continue;
				log.error("字段 {} 访问异常：{}", targetFieldName, e.getMessage(), e);
			}
		}
	}

	/**
	 * @Name        : getFieldMapping
	 * @Description : 获取字段映射关系（支持默认字段名一致的映射 + 自定义映射）
	 * @Param       : sourceClass 源类
	 * @Param       : targetClass 目标类
	 * @Return      : Map<String, String> 字段映射关系
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private static Map<String, String> getFieldMapping(Class<?> sourceClass, Class<?> targetClass) {
		Map<String, String> mapping = new HashMap<>();

		// 1. 获取源对象和目标对象的所有字段
		Field[] sourceFields = sourceClass.getDeclaredFields();
		Field[] targetFields = targetClass.getDeclaredFields();

		// 2. 构建目标字段名集合（用于快速查找）
		Set<String> targetFieldNames = Arrays.stream(targetFields)
				.map(Field::getName)
				.collect(Collectors.toSet());

		// 3. 默认映射：字段名相同的直接映射
		for (Field sourceField : sourceFields) {
			String sourceFieldName = sourceField.getName();
			if (targetFieldNames.contains(sourceFieldName)) {
				// 字段名一致，直接映射
				mapping.put(sourceFieldName, sourceFieldName);
			}
		}

		return mapping;
	}

	/**
	 * @Name        : setValue
	 * @Description : 设置目标字段值（支持类型转换）
	 * @Param       : field 目标字段
	 * @Param       : target 目标对象
	 * @Param       : value 值
	 * @Return      : void
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private static void setValue(Field field, Object target, Object value) throws IllegalAccessException {
		Class<?> fieldType = field.getType();
		if (value == null) {
			field.set(target, null);
			return;
		}

		// 类型匹配直接赋值
		if (fieldType.isAssignableFrom(value.getClass())) {
			field.set(target, value);
			return;
		}

		// 类型不匹配时尝试转换
		if (fieldType == String.class) {
			field.set(target, value.toString());
		} else if (fieldType == Integer.class || fieldType == int.class) {
			field.set(target, Integer.valueOf(value.toString()));
		} else if (fieldType == Long.class || fieldType == long.class) {
			field.set(target, Long.valueOf(value.toString()));
		} else if (fieldType == Double.class || fieldType == double.class) {
			field.set(target, Double.valueOf(value.toString()));
		} else if (fieldType == Boolean.class || fieldType == boolean.class) {
			field.set(target, Boolean.valueOf(value.toString()));
		} else {
			throw new IllegalArgumentException("不支持的类型转换：" + fieldType.getName());
		}
	}
}
