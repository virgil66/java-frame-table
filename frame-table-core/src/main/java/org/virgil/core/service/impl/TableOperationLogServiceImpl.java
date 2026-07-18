package org.virgil.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.virgil.core.entity.TableOperationLog;
import org.virgil.core.mapper.TableOperationLogMapper;
import org.virgil.core.service.TableOperationLogService;
import org.virgil.core.util.SnowflakeIdGenerator;

import java.time.LocalDateTime;

/**
 * @ProjectName : java-frame-table
 * @ClassName : TableOperationLogServiceImpl
 * @Description : 表操作日志服务实现类
 * @Date : 2026/7/9 23:08
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Slf4j
@Service
public class TableOperationLogServiceImpl extends ServiceImpl<TableOperationLogMapper, TableOperationLog> implements TableOperationLogService {
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
	@Override
	public void generateTableOperationLog(String tableName, String operationType, Boolean upgradeSuccess, String upgradeFailReason, String executeSql) {
		// 查询是否已存在记录
		TableOperationLog existingLog = this.lambdaQuery()
				.eq(TableOperationLog::getTableName, tableName)
				.orderByDesc(TableOperationLog::getVersion)
				.last("LIMIT 1")
				.one();

		// 初始化版本号
		int version = 1;
		if (existingLog != null) {
			// 递增版本号
			version = existingLog.getVersion() + 1;
		}

		// 创建新的日志记录
		TableOperationLog tableOperationLog = new TableOperationLog();
		tableOperationLog
				.setTableName(tableName)
				.setOperationType(operationType)
				.setUpgradeSuccess(upgradeSuccess)
				.setUpgradeFailReason(upgradeFailReason)
				.setUpgradeTime(LocalDateTime.now())
				.setVersion(version)
				.setExecuteSql(executeSql);

		// 初始化雪花算法
		SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1);
		tableOperationLog.setId(idGenerator.nextId());

		this.baseMapper.insert(tableOperationLog);
		log.info("表升级日志已记录：表名={}, 操作类型={}, 版本号={}, 是否升级成功={}, 升级失败原因={}", tableName, operationType, version, upgradeSuccess, upgradeFailReason);
	}
}
