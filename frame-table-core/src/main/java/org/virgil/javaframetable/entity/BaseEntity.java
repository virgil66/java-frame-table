package org.virgil.javaframetable.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.experimental.Accessors;
import org.virgil.javaframetable.common.EntityFieldAnnotation;
import org.virgil.javaframetable.enums.EntityFieldType;

import java.time.LocalDateTime;

/**
 * @ProjectName : java-frame-table
 * @ClassName : BaseEntity
 * @Description : 基础实体类
 * @Date : 2026/7/10 00:46
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Accessors(chain = true)
@Data
public class BaseEntity {
	@EntityFieldAnnotation(
			text = "主键ID",
			type = EntityFieldType.LONG,
			not_null = true,
			pk = true
	)
	private Long id;

	@TableField(fill = FieldFill.INSERT)
	@EntityFieldAnnotation(
			text = "创建时间",
			type = EntityFieldType.DATETIME
	)
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.INSERT)
	@EntityFieldAnnotation(
			text = "创建人",
			type = EntityFieldType.VARCHAR,
			length = "50"
	)
	private String createName;

	@TableField(fill = FieldFill.INSERT)
	@EntityFieldAnnotation(
			text = "创建人工号",
			type = EntityFieldType.VARCHAR,
			length = "10"
	)
	private String createCode;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@EntityFieldAnnotation(
			text = "修改时间",
			type = EntityFieldType.DATETIME
	)
	private LocalDateTime modifyTime;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@EntityFieldAnnotation(
			text = "修改人",
			type = EntityFieldType.VARCHAR,
			length = "50"
	)
	private String modifyName;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@EntityFieldAnnotation(
			text = "修改人工号",
			type = EntityFieldType.VARCHAR,
			length = "10"
	)
	private String modifyCode;

	@EntityFieldAnnotation(
			text = "删除时间",
			type = EntityFieldType.DATETIME
	)
	private LocalDateTime deleteTime;

	@EntityFieldAnnotation(
			text = "删除人",
			type = EntityFieldType.VARCHAR,
			length = "50"
	)
	private String deleteName;

	@EntityFieldAnnotation(
			text = "删除人工号",
			type = EntityFieldType.VARCHAR,
			length = "10"
	)
	private String deleteCode;

	@EntityFieldAnnotation(
			text = "是否删除标识",
			type = EntityFieldType.TINYINT,
			default_value = "0",
			comment = "0：未删除；1：已删除"
	)
	private Boolean flagDeleted;
}
