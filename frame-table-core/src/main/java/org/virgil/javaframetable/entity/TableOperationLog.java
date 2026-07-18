package org.virgil.javaframetable.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.virgil.javaframetable.common.AutoTable;
import org.virgil.javaframetable.common.EntityClassAnnotation;
import org.virgil.javaframetable.common.EntityFieldAnnotation;
import org.virgil.javaframetable.common.EntityIndexAnnotation;
import org.virgil.javaframetable.enums.EntityFieldType;

import java.time.LocalDateTime;

/**
 * @ProjectName : java-frame-table
 * @ClassName : TableOperationLog
 * @Description : 表操作日志
 * @Date : 2026/7/9 23:02
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@AutoTable()
@Accessors(chain = true)
@Data
@EntityClassAnnotation(remark = "表升级日志表")
@EntityIndexAnnotation(ux = {}, ix = { "tableName", "operationType" })
@EqualsAndHashCode(callSuper = true)
@TableName("sys_table_operation_log")
public class TableOperationLog extends BaseEntity {
	// @TableId(type = IdType.AUTO)
	// private Long id;

	@EntityFieldAnnotation(
			text = "表名",
			type = EntityFieldType.VARCHAR,
			length = "50",
			not_null = true
	)
	private String tableName;

	@EntityFieldAnnotation(
			text = "操作类型",
			type = EntityFieldType.VARCHAR,
			length = "20",
			not_null = true,
			default_value = "create",
			comment = "create：创建表；update：更新表"
	)
	private String operationType;

	@EntityFieldAnnotation(
			text = "版本号",
			type = EntityFieldType.INT,
			not_null = true,
			default_value = "1"
	)
	private Integer version;

	@EntityFieldAnnotation(
			text = "是否升级成功",
			type = EntityFieldType.TINYINT,
			not_null = true,
			default_value = "1",
			comment = "0：失败；1：成功"
	)
	private Boolean upgradeSuccess;

	@EntityFieldAnnotation(
			text = "升级失败原因",
			type = EntityFieldType.VARCHAR,
			length = "1024"
	)
	private String upgradeFailReason;

	@EntityFieldAnnotation(
			text = "升级时间",
			type = EntityFieldType.DATETIME,
			not_null = true
	)
	private LocalDateTime upgradeTime;

	@EntityFieldAnnotation(
			text = "执行SQL",
			type = EntityFieldType.TEXT,
			not_null = true
	)
	private String executeSql;
}
