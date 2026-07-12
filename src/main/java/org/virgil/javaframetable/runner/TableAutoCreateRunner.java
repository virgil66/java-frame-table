package org.virgil.javaframetable.runner;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.virgil.javaframetable.common.AutoTable;
import org.virgil.javaframetable.common.EntityClassAnnotation;
import org.virgil.javaframetable.common.EntityFieldAnnotation;
import org.virgil.javaframetable.common.EntityIndexAnnotation;
import org.virgil.javaframetable.service.TableOperationLogService;
import org.virgil.javaframetable.util.EntityFieldParser;
import org.virgil.javaframetable.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

/**
 * @ProjectName : java-frame-table
 * @ClassName : TableAutoCreateRunner
 * @Description : 创建数据库表
 * @Date : 2026/7/9 23:17
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Slf4j
@Component
public class TableAutoCreateRunner implements ApplicationRunner {
	@Resource
	private DataSource dataSource;
	@Resource
	private TableOperationLogService tableOperationLogService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// 实体类所在包
		String basePackage = "org.virgil.javaframetable";
		Set<Class<?>> entityClasses = scanEntityClasses(basePackage);

		for (Class<?> entityClass : entityClasses) {
			if (entityClass.isAnnotationPresent(AutoTable.class)) {
				AutoTable autoTable = entityClass.getAnnotation(AutoTable.class);
				if (!autoTable.enable()) {
					continue;
				}

				String tableName = getTableName(entityClass);
				String tableRemark = getTableRemark(entityClass);
				List<String> fieldDefinitions =  EntityFieldParser.parseEntityFields(entityClass);

				try (Connection connection = dataSource.getConnection()) {
					if (!tableExists(connection, tableName)) {
						StringBuilder createSqlAll = new StringBuilder();
						try {
							// 创建表
							String createSql = generateCreateTableSql(tableName, fieldDefinitions, tableRemark);
							createSqlAll.append(createSql);
							executeSql(connection, createSql);
							log.info("表 {} 已创建，创建表结构语句：{}", tableName, createSql);

							// 解析并创建索引
							if (entityClass.isAnnotationPresent(EntityIndexAnnotation.class)) {
								EntityIndexAnnotation indexAnnotation = entityClass.getAnnotation(EntityIndexAnnotation.class);

								// 处理唯一索引
								for (String field : indexAnnotation.ux()) {
									String sql = createIndex(connection, tableName, field, entityClass, true);
									if (StringUtils.isNotBlank(sql)) {
										createSqlAll.append(sql);
									}
								}

								// 处理普通索引
								for (String field : indexAnnotation.ix()) {
									String sql = createIndex(connection, tableName, field, entityClass, false);
									if (StringUtils.isNotBlank(sql)) {
										createSqlAll.append(sql);
									}
								}
							}

							// 记录日志
							recordTableOperationLog(tableName, "create", true, null, createSqlAll.toString());
						} catch (Exception e) {
							// 记录失败日志
							recordTableOperationLog(tableName, "create", false, e.getMessage(), createSqlAll.toString());
							throw e;
						}

					} else {
						StringBuilder alterSqlAll = new StringBuilder();
						try {
							List<String> allUpdateSql = new ArrayList<>();

							// 阶段1：生成表备注变更 SQL（纯生成，不执行）
							String updateTableRemarkSql = generateUpdateTableRemarkSql(connection, tableName, tableRemark);
							if (StringUtils.isNotBlank(updateTableRemarkSql)) {
								allUpdateSql.add(updateTableRemarkSql);
							}

							// 阶段2：生成索引变更 SQL 列表（纯生成，不执行）
							List<String> indexSqlList = generateUpdateIndexesSql(connection, tableName, entityClass);
							allUpdateSql.addAll(indexSqlList);

							// 阶段3：生成字段 MODIFY / ADD SQL（纯生成，不执行）
							Set<String> expectedColumns = new HashSet<>();
							Set<String> definedPkFields = new HashSet<>();
							for (String columnDefinition : fieldDefinitions) {
								if (columnDefinition.toUpperCase().startsWith("PRIMARY KEY")) {
									// 收集主键定义，但不加入 allUpdateSql（稍后统一处理）
									String pkFields = columnDefinition.substring("PRIMARY KEY (".length());
									pkFields = pkFields.substring(0, pkFields.length() - 1);
									String[] pkFieldArray = pkFields.split(",\\s*");
									for (String pkField : pkFieldArray) {
										definedPkFields.add(pkField.trim().toLowerCase());
									}
									continue;
								}

								String fieldName = columnDefinition.split("\\s+")[0];
								expectedColumns.add(fieldName.toLowerCase());

								Map<String, String> currentMetadata = getColumnMetadata(connection, tableName, fieldName);

								if (!currentMetadata.isEmpty()) {
									String alterSql = generateAlterColumnSql(tableName, fieldName, columnDefinition, currentMetadata);
									if (alterSql != null) {
										allUpdateSql.add(alterSql);
										log.info("字段 {} 需要更新，执行语句：{}", fieldName, alterSql);
									} else {
										log.info("字段 {} 无需更新", fieldName);
									}
								} else {
									String addColumnSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition + ";";
									allUpdateSql.add(addColumnSql);
									log.info("字段 {} 需要添加，执行语句：{}", fieldName, addColumnSql);
								}
							}

							// 阶段4：补全主键
							Set<String> existingPK = getPrimaryKeyColumns(connection, tableName);
							for (String pkField : definedPkFields) {
								if (!existingPK.contains(pkField)) {
									String addPkSql = "ALTER TABLE " + tableName + " ADD PRIMARY KEY (" + pkField + ");";
									allUpdateSql.add(addPkSql);
									log.info("主键字段 {} 需要添加主键约束", pkField);
									break; // 只加一次 PRIMARY KEY
								}
							}

							// 阶段5：废弃字段 DROP
							Set<String> indexedColumns = getIndexedColumns(connection, tableName);
							Set<String> actualColumns = getTableColumns(connection, tableName);
							for (String actualColumn : actualColumns) {
								if (!expectedColumns.contains(actualColumn.toLowerCase())
										&& !indexedColumns.contains(actualColumn.toLowerCase())
										&& !existingPK.contains(actualColumn.toLowerCase())) {
									String dropSql = "ALTER TABLE " + tableName + " DROP COLUMN " + actualColumn + ";";
									allUpdateSql.add(dropSql);
									log.info("废弃字段 {} 需要删除", actualColumn);
								}
							}

							// 阶段6：统一执行所有 SQL（先记录，后执行）
							for (String sql : allUpdateSql) {
								alterSqlAll.append(sql);
								executeSql(connection, sql);
							}

							// 记录日志
							recordTableOperationLog(tableName, "update", true, null, alterSqlAll.toString());
						} catch (Exception e) {
							// 记录失败日志
							recordTableOperationLog(tableName, "update", false, e.getMessage(), alterSqlAll.toString());
							throw e;
						}
					}
				}
			}
		}
	}

	/**
	 * @Name        : generateUpdateTableRemarkSql
	 * @Description : 生成表备注变更 SQL
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Param       : newRemark 新的表备注
	 * @Return      : String SQL
	 * @Date        : 2026/7/13
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String generateUpdateTableRemarkSql(Connection connection, String tableName, String newRemark) throws SQLException {
		String currentRemark = getCurrentTableRemark(connection, tableName);
		if (!Objects.equals(currentRemark, newRemark)) {
			String sql = "ALTER TABLE " + tableName + " COMMENT '" + newRemark + "';";
			log.info("表 {} 的备注需要更新为：{}", tableName, newRemark);
			return sql;
		} else {
			log.info("表 {} 的备注无需更新", tableName);
			return "";
		}
	}

	/**
	 * @Name        : generateUpdateIndexesSql
	 * @Description : 获取索引变更 SQL
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Param       : entityClass 实体类
	 * @Return      : List<String> SQL 列表
	 * @Date        : 2026/7/13
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String generateCreateIndexSql(String tableName, String field, Class<?> entityClass, boolean isUnique) throws SQLException {
		Map<String, String> fieldInfo = getFieldInfo(entityClass, field);
		if (fieldInfo.isEmpty()) {
			return "";
		}
		String fieldType = fieldInfo.get("type");
		String fieldLength = fieldInfo.get("length");

		String indexLength = getIndexLength(fieldType, fieldLength);
		String fieldNamePrefix = isUnique ? "idx_unique_" : "idx_normal_";
		String fieldName = StringUtils.camelToUnderscore(field);
		String indexFieldName = fieldNamePrefix + fieldName;
		String indexType = isUnique ? "UNIQUE" : "";

		return "CREATE " + indexType + " INDEX " + indexFieldName
				+ " ON " + tableName + " (" + fieldName + indexLength + ");";
	}

	/**
	 * @Name        : generateDropIndexSql
	 * @Description : 获取索引删除 SQL
	 * @Param       : tableName 表名
	 * @Param       : indexName 索引名
	 * @Return      : String SQL
	 * @Date        : 2026/7/13
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String generateDropIndexSql(String tableName, String indexName) {
		return "DROP INDEX " + indexName + " ON " + tableName + ";";
	}

	/**
	 * @Name        : generateUpdateIndexesSql
	 * @Description : 获取索引变更 SQL
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Param       : entityClass 实体类
	 * @Return      : List<String> SQL 列表
	 * @Date        : 2026/7/13
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private List<String> generateUpdateIndexesSql(Connection connection, String tableName, Class<?> entityClass) throws SQLException {
		List<String> sqlList = new ArrayList<>();

		if (!entityClass.isAnnotationPresent(EntityIndexAnnotation.class)) {
			return sqlList;
		}

		EntityIndexAnnotation indexAnnotation = entityClass.getAnnotation(EntityIndexAnnotation.class);
		Set<String> currentIndexes = getCurrentIndexes(connection, tableName);

		// 处理唯一索引
		Set<String> expectedUniqueIndexes = new HashSet<>();
		for (String field : indexAnnotation.ux()) {
			String indexName = "idx_unique_" + StringUtils.camelToUnderscore(field);
			expectedUniqueIndexes.add(indexName);
			if (!currentIndexes.contains(indexName)) {
				String sql = generateCreateIndexSql(tableName, field, entityClass, true);
				if (StringUtils.isNotBlank(sql)) {
					sqlList.add(sql);
					log.info("需要创建唯一索引：{}", indexName);
				}
			}
		}

		// 处理普通索引
		Set<String> expectedNormalIndexes = new HashSet<>();
		for (String field : indexAnnotation.ix()) {
			String indexName = "idx_normal_" + StringUtils.camelToUnderscore(field);
			expectedNormalIndexes.add(indexName);
			if (!currentIndexes.contains(indexName)) {
				String sql = generateCreateIndexSql(tableName, field, entityClass, false);
				if (StringUtils.isNotBlank(sql)) {
					sqlList.add(sql);
					log.info("需要创建普通索引：{}", indexName);
				}
			}
		}

		// 删除多余的索引（排除 PRIMARY）
		for (String currentIndex : currentIndexes) {
			if ("PRIMARY".equalsIgnoreCase(currentIndex)) {
				continue;
			}
			if (!expectedUniqueIndexes.contains(currentIndex) && !expectedNormalIndexes.contains(currentIndex)) {
				String sql = generateDropIndexSql(tableName, currentIndex);
				sqlList.add(sql);
				log.info("需要删除多余索引：{}", currentIndex);
			}
		}

		return sqlList;
	}

	/**
	 * @Name        : scanEntityClasses
	 * @Description : 扫描实体类
	 * @Param       : basePackage 包名
	 * @Return      : Set<Class<?>> 实体类集合
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Set<Class<?>> scanEntityClasses(String basePackage) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(AutoTable.class));

		Set<Class<?>> entityClasses = new HashSet<>();
		for (BeanDefinition beanDefinition : scanner.findCandidateComponents(basePackage)) {
			try {
				entityClasses.add(Class.forName(beanDefinition.getBeanClassName()));
			} catch (ClassNotFoundException e) {
				log.error("无法加载实体类: {}", beanDefinition.getBeanClassName(), e);
			}
		}
		return entityClasses;
	}

	/**
	 * @Name        : generateCreateTableSql
	 * @Description : 生成创建表的SQL语句
	 * @Param       : tableName 表名
	 * @Param       : fieldDefinitions 字段定义列表
	 * @Return      : String 创建表的SQL语句
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public String generateCreateTableSql(String tableName, List<String> fieldDefinitions, String tableRemark) {
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
		sql.append(tableName).append(" (");

		// 添加雪花算法生成的主键字段
		// sql.append("id BIGINT NOT NULL, ");


		for (int i = 0; i < fieldDefinitions.size(); i++) {
			sql.append(fieldDefinitions.get(i));
			if (i < fieldDefinitions.size() - 1) {
				sql.append(", ");
			}
		}
		sql.append(")");

		// 添加表备注信息
		if (tableRemark != null && !tableRemark.isEmpty()) {
			sql.append(" COMMENT '").append(tableRemark).append("'");
		}

		return sql.toString() + ";";
	}

	/**
	 * @Name        : generateAlterColumnSql
	 * @Description : 生成字段变更的 SQL 语句
	 * @Param       : tableName 表名
	 * @Param       : columnName 字段名
	 * @Param       : newDefinition 新字段定义
	 * @Param       : currentMetadata 当前字段元数据
	 * @Return      : String 字段变更 SQL 语句
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String generateAlterColumnSql(String tableName, String columnName, String newDefinition, Map<String, String> currentMetadata) {
		String currentType = currentMetadata.get("type");
		String currentSize = currentMetadata.get("size");
		String currentNullable = currentMetadata.get("nullable");

		// 解析新字段定义中的类型和长度
		String[] parts = newDefinition.trim().split("\\s+");
		String newType = parts[1];
		String newSize = newType.contains("(") ? newType.replaceAll(".*\\((.*)\\).*", "$1") : null;

		// 判断 nullability 是否变更
		boolean newNotNull = newDefinition.toUpperCase().contains("NOT NULL");
		boolean currentNotNull = "NO".equals(currentNullable);

		boolean typeChanged = !newType.equalsIgnoreCase(currentType);
		boolean sizeChanged = newSize != null && !newSize.equals(currentSize);
		boolean nullableChanged = newNotNull != currentNotNull;

		// 判断是否需要变更
		if (typeChanged || sizeChanged || nullableChanged) {
			return "ALTER TABLE " + tableName + " MODIFY COLUMN " + newDefinition + ";";
		}

		// 无需变更
		return null;
	}

	/**
	 * @Name        : getColumnMetadata
	 * @Description : 获取表中字段的原数据
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Param       : columnName 字段名
	 * @Return      : Map<String, String> 字段元数据（类型、长度等）
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Map<String, String> getColumnMetadata(Connection connection, String tableName, String columnName) throws SQLException {
		Map<String, String> metadata = new HashMap<>();
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName);

		if (resultSet.next()) {
			metadata.put("type", resultSet.getString("TYPE_NAME"));
			metadata.put("size", String.valueOf(resultSet.getInt("COLUMN_SIZE")));
			metadata.put("nullable", resultSet.getString("IS_NULLABLE"));
		}

		return metadata;
	}

	/**
	 * @Name        : getFieldInfo
	 * @Description : 获取字段类型和长度信息
	 * @Param       : entityClass 实体类
	 * @Param       : fieldName 字段名
	 * @Return      : Map<String, String> 字段类型和长度信息
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Map<String, String> getFieldInfo(Class<?> entityClass, String fieldName) {
		Map<String, String> fieldInfo = new HashMap<>();
		// 获取原始类，过滤掉代理类
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(entityClass);
		log.debug("正在解析字段: {}, 原始类: {}", fieldName, targetClass.getSimpleName());

		// 查找字段
		Field field = findField(targetClass, fieldName);
		if (field == null) {
			log.warn("字段 {} 在实体类 {} 中未找到", fieldName, targetClass.getSimpleName());
		} else {
			if (field.isAnnotationPresent(EntityFieldAnnotation.class)) {
				EntityFieldAnnotation annotation = field.getAnnotation(EntityFieldAnnotation.class);
				String fieldType = EntityFieldParser.getSqlTypeFromAnnotation(annotation, field.getType());
				// 获取字段长度
				String length = annotation.length();
				fieldInfo.put("type", fieldType);
				fieldInfo.put("length", length);
			} else {
				log.warn("字段 {} 在实体类 {} 中未标注 @EntityFieldAnnotation 注解", fieldName, targetClass.getSimpleName());
			}
		}

		return fieldInfo;
	}

	/**
	 * @Name        : findField
	 * @Description : 递归查找字段
	 * @Param       : clazz 类
	 * @Param       : fieldName 字段名
	 * @Return      : Field 字段
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Field findField(Class<?> clazz, String fieldName) {
		Class<?> current = clazz;
		while (current != null && current != Object.class) {
			try {
				return current.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				current = current.getSuperclass();
			}
		}
		return null;
	}

	/**
	 * @Name        : getPrimaryKeyColumns
	 * @Description : 获取表的主键字段
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Return      : Set<String> 主键字段
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Set<String> getPrimaryKeyColumns(Connection connection, String tableName) throws SQLException {
		Set<String> pkColumns = new HashSet<>();
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getPrimaryKeys(null, null, tableName);
		while (resultSet.next()) {
			pkColumns.add(resultSet.getString("COLUMN_NAME").toLowerCase());
		}
		return pkColumns;
	}

	/**
	 * @Name        : getTableColumns
	 * @Description : 获取表中的所有字段
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Return      : Set<String> 表中的字段
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Set<String> getTableColumns(Connection connection, String tableName) throws SQLException {
		Set<String> columns = new HashSet<>();
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getColumns(null, null, tableName, null);
		while (resultSet.next()) {
			columns.add(resultSet.getString("COLUMN_NAME").toLowerCase());
		}
		return columns;
	}

	/**
	 * @Name        : getTableName
	 * @Description : 获取表名
	 * @Param       : entityClass 实体类
	 * @Return      : String 表名
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String getTableName(Class<?> entityClass) {
		// 检查是否有 @TableName 注解
		if (entityClass.isAnnotationPresent(TableName.class)) {
			TableName tableNameAnnotation = entityClass.getAnnotation(TableName.class);
			// 返回注解中的表名
			return tableNameAnnotation.value();
		} else {
			// 没有注解时，将类名转换为下划线形式
			return StringUtils.camelToUnderscore(entityClass.getSimpleName());
		}
	}

	/**
	 * @Name        : getTableRemark
	 * @Description : 获取表备注
	 * @Param       : entityClass 实体类
	 * @Return      : String 表备注
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String getTableRemark(Class<?> entityClass) {
		if (entityClass.isAnnotationPresent(EntityClassAnnotation.class)) {
			EntityClassAnnotation annotation = entityClass.getAnnotation(EntityClassAnnotation.class);
			return annotation.remark();
		}
		return null;
	}

	/**
	 * @Name        : tableExists
	 * @Description : 检查表是否存在
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Return      : boolean true：存在，false：不存在
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private boolean tableExists(Connection connection, String tableName) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
		return resultSet.next();
	}

	/**
	 * @Name        : executeSql
	 * @Description : 执行 SQL
	 * @Param       : connection 数据库连接
	 * @Param       : sql SQL
	 * @Return      : void
	 * @Date        : 2026/2/14
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private void executeSql(Connection connection, String sql) throws SQLException {
		try {
			log.info("执行 SQL: {}", sql);
			Statement statement = connection.createStatement();
			statement.executeUpdate(sql);
			statement.close();
		} catch (SQLException e) {
			// 记录失败的 SQL 执行日志
			log.error("SQL 执行失败: {}，失败原因：{}", sql, e.getMessage());
			throw e;
		}
	}

	/**
	 * @Name        : getCurrentTableRemark
	 * @Description : 获取当前表的备注信息
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Return      : String 当前表备注
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String getCurrentTableRemark(Connection connection, String tableName) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"});

		if (resultSet.next()) {
			return resultSet.getString("REMARKS");
		}

		return null;
	}

	/**
	 * @Name        : getCurrentIndexes
	 * @Description : 获取当前表的所有索引名称
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Return      : Set<String> 索引名称集合
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Set<String> getCurrentIndexes(Connection connection, String tableName) throws SQLException {
		Set<String> indexes = new HashSet<>();
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false);

		while (resultSet.next()) {
			indexes.add(resultSet.getString("INDEX_NAME"));
		}

		return indexes;
	}

	/**
	 * @Name        : getIndexedColumns
	 * @Description : 获取索引的列
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Return      : Set<String> 索引的列名称集合
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private Set<String> getIndexedColumns(Connection connection, String tableName) throws SQLException {
		Set<String> indexedColumns = new HashSet<>();
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet resultSet = metaData.getIndexInfo(null, null, tableName, false, false);
		while (resultSet.next()) {
			String columnName = resultSet.getString("COLUMN_NAME");
			if (columnName != null) {
				indexedColumns.add(columnName.toLowerCase());
			}
		}
		return indexedColumns;
	}

	/**
	 * @Name        : createIndex
	 * @Description : 创建索引（唯一索引或普通索引）
	 * @Param       : connection 数据库连接
	 * @Param       : tableName 表名
	 * @Param       : field 字段名
	 * @Param       : indexAnnotation 索引注解
	 * @Param       : isUnique 是否为唯一索引
	 * @Return      : String 执行 SQL
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String createIndex(Connection connection, String tableName, String field, Class<?> entityClass, boolean isUnique) throws SQLException {
		Map<String, String> fieldInfo = getFieldInfo(entityClass, field);
		String fieldType = fieldInfo.get("type");
		String fieldLength = fieldInfo.get("length");

		String indexLength = getIndexLength(fieldType, fieldLength);
		String fieldNamePrefix = isUnique ? "idx_unique_" : "idx_normal_";
		String fieldName = StringUtils.camelToUnderscore(field);
		String indexFieldName = fieldNamePrefix + StringUtils.camelToUnderscore(field);
		String indexType = isUnique ? "UNIQUE" : "";
		String sql = "CREATE " + indexType + " INDEX " + indexFieldName + " ON " + tableName + " (" + fieldName + indexLength + ");";
		executeSql(connection, sql);
		log.info("{}索引 {} 已创建，创建语句：{}", isUnique ? "唯一" : "普通", indexFieldName, sql);
		return sql;
	}

	/**
	 * @Name        : getIndexLength
	 * @Description : 根据字段类型和长度计算索引长度
	 * @Param       : fieldType 字段类型
	 * @Param       : fieldLength 字段长度
	 * @Return      : String 索引长度
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private String getIndexLength(String fieldType, String fieldLength) {
		String indexLength = "";
		if ("TEXT".equals(fieldType)) {
			indexLength = "(2048)";
		} else if ("VARCHAR".equals(fieldType) && !fieldLength.isEmpty()) {
			indexLength = "(" + fieldLength + ")";
		}
		return indexLength;
	}

	/**
	 * @Name        : recordTableOperationLog
	 * @Description : 记录表操作日志
	 * @Param       : tableName 表名
	 * @Param       : operationType 操作类型
	 * @Param       : isSuccess 是否成功
	 * @Param       : failReason 失败原因
	 * @Param       : executeSql 执行的 SQL
	 * @Return      : void
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private void recordTableOperationLog(String tableName, String operationType, boolean isSuccess, String failReason, String executeSql) {
		try {
			tableOperationLogService.generateTableOperationLog(tableName, operationType, isSuccess, failReason, executeSql);
			if (isSuccess) {
				log.info("表 {} 操作 {} 成功，执行 SLQ ：{}", tableName, operationType, executeSql);
			} else {
				log.warn("表 {} 操作 {} 失败，失败原因：{}，，执行 SLQ ：{}", tableName, operationType, failReason, executeSql);
			}
		} catch (Exception e) {
			log.error("记录表 {} 操作 {} 日志时发生异常：{}", tableName, operationType, e.getMessage(), e);
		}
	}
}
