package org.virgil.javaframetable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.virgil.javaframetable.entity.TableOperationLog;

/**
 * @ProjectName : java-frame-table
 * @ClassName : TableOperationLogService
 * @Description : 表操作日志服务
 * @Date : 2026/7/9 23:06
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
public interface TableOperationLogService extends IService<TableOperationLog> {
	/**
	 * @Name        : generateTableOperationLog
	 * @Description : 生成表操作日志
	 * @Param       : tableName 表名
	 * @Param       : operationType 操作类型
	 * @Param       : upgradeSuccess 是否成功
	 * @Param       : upgradeFailReason 升级失败原因
	 * @Param       : executeSql 执行的SQL
	 * @Return 			: void
	 * @Date        : 2026/2/15
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	void generateTableOperationLog(String tableName, String operationType, Boolean upgradeSuccess, String upgradeFailReason, String executeSql);
}
