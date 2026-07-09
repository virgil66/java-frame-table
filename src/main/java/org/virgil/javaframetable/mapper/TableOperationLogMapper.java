package org.virgil.javaframetable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.virgil.javaframetable.entity.TableOperationLog;

/**
 * @ProjectName : java-frame-table
 * @ClassName : TableOperationLogMapper
 * @Description : 表操作日志 Mapper
 * @Date : 2026/7/9 23:09
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
@Mapper
public interface TableOperationLogMapper extends BaseMapper<TableOperationLog> {
}
