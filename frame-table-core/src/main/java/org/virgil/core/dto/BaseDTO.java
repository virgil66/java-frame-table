package org.virgil.core.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @ProjectName : java-frame-table
 * @ClassName : BaseDTO
 * @Description : 基础DTO，新增/编辑通用校验分组
 * @Date : 2026/7/19 01:23
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Data
public class BaseDTO {
	/**
	 * 编辑时必填，新增不传
	 */
	@NotNull(message = "主键ID不能为空", groups = EditGroup.class)
	@Min(value = 1, message = "ID必须大于0", groups = EditGroup.class)
	private Long id;

	/**
	 * 新增分组
	 */
	public interface AddGroup {}

	/**
	 * 编辑分组
	 */
	public interface EditGroup {}
}
