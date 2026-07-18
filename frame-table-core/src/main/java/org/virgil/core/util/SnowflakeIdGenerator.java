package org.virgil.core.util;

/**
 * @ProjectName : fortec-tech-business-matr
 * @ClassName : SnowflakeIdGenerator
 * @Description : 雪花ID生成器
 * @Date : 2026/1/30 09:29
 * @Author : dujing
 * @Version : 1.0
 * @Email : jing.du@forten-tech.com
 */
public class SnowflakeIdGenerator {
	// 起始时间戳（2026-01-01），避免时钟回拨问题
	private static final long START_TIMESTAMP = 1735689600000L;
	// 机器ID位数（最多支持32台机器）
	private static final long MACHINE_BIT = 5L;
	// 序列号位数（每毫秒最多生成32个ID）
	private static final long SEQUENCE_BIT = 5L;

	// 最大值计算
	private static final long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
	private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

	// 移位偏移量
	private static final long MACHINE_LEFT = SEQUENCE_BIT;
	private static final long TIMESTAMP_LEFT = SEQUENCE_BIT + MACHINE_BIT;

	// 机器ID（单机可固定为1，分布式需配置不同值）
	private final long machineId;
	// 序列号（毫秒内计数器）
	private long sequence = 0L;
	// 上一次生成ID的时间戳
	private long lastTimestamp = -1L;

	// 私有构造函数，防止外部实例化
	public SnowflakeIdGenerator(long machineId) {
		if (machineId < 0 || machineId > MAX_MACHINE_NUM) {
			throw new IllegalArgumentException("机器ID超出范围，应在0-" + MAX_MACHINE_NUM + "之间");
		}
		this.machineId = machineId;
	}

	// 单例实例（默认机器ID为0）
	private static volatile SnowflakeIdGenerator instance;

	/**
	 * 获取默认实例（机器ID为0）
	 *
	 * @return SnowflakeIdGenerator实例
	 */
	public static SnowflakeIdGenerator getInstance() {
		return getInstance(0L);
	}

	/**
	 * @Name        : getInstance
	 * @Description : 获取默认实例（机器ID为0）
	 * @Param       : machineId - 机器ID（0-31）
	 * @Return      : SnowflakeIdGenerator实例
	 * @Date        : 2026/1/30 09:50
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public static SnowflakeIdGenerator getInstance(long machineId) {
		if (instance == null) {
			synchronized (SnowflakeIdGenerator.class) {
				if (instance == null) {
					instance = new SnowflakeIdGenerator(machineId);
				}
			}
		}
		return instance;
	}

	/**
	 * @Name        : nextId
	 * @Description : 生成下一个唯一ID
	 * @Param       : null
	 * @Return      : long - 生成的唯一ID
	 * @Date        : 2026/1/30 09:50
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public synchronized long nextId() {
		long currentTimestamp = System.currentTimeMillis();

		// 时钟回拨处理
		if (currentTimestamp < lastTimestamp) {
			throw new RuntimeException(
					String.format("时钟回拨检测：lastTimestamp=%d, currentTimestamp=%d",
							lastTimestamp, currentTimestamp));
		}

		// 同一毫秒内，序列号自增
		if (currentTimestamp == lastTimestamp) {
			sequence = (sequence + 1) & MAX_SEQUENCE;

			// 毫秒内序列号用尽，等待下一毫秒
			if (sequence == 0L) {
				currentTimestamp = waitNextMillis(currentTimestamp);
			}
		} else {
			// 不同毫秒，序列号重置为0
			sequence = 0L;
		}

		lastTimestamp = currentTimestamp;

		// 拼接ID：时间戳 + 机器ID + 序列号
		return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_LEFT) |
				(machineId << MACHINE_LEFT) |
				sequence;
	}

	/**
	 * @Name        : waitNextMillis
	 * @Description : 等待下一毫秒，直到时间戳大于上一次生成ID的时间戳
	 * @Param       : currentTimestamp - 当前时间戳
	 * @Return      : long - 下一毫秒的时间戳
	 * @Date        : 2026/1/30 10:00
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	private long waitNextMillis(long currentTimestamp) {
		while (currentTimestamp <= lastTimestamp) {
			currentTimestamp = System.currentTimeMillis();
		}
		return currentTimestamp;
	}

	/**
	 * @Name        : nextIdStr
	 * @Description : 生成下一个唯一ID（字符串表示）
	 * @Param       : null
	 * @Return      : String - 生成的唯一ID（字符串表示）
	 * @Date        : 2026/1/30 10:00
	 * @Author      : dujing
	 * @Version     : 1.0
	 * @Email       : jing.du@forten-tech.com
	 */
	public String nextIdStr() {
		return String.valueOf(nextId());
	}
}
