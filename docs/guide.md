# java-frame-table 使用指南

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [核心功能详解](#核心功能详解)
  - [注解体系](#注解体系)
  - [自动建表流程](#自动建表流程)
  - [表结构自动升级](#表结构自动升级)
  - [操作日志记录](#操作日志记录)
- [完整使用示例](#完整使用示例)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

---

## 项目简介

java-frame-table 是一个基于 Spring Boot + MyBatis-Plus 的**数据库表自动管理框架**。开发者只需通过注解定义实体类字段，框架会在应用启动时自动完成：

- **建表** — 不存在的表自动创建
- **升级** — 已有表自动比对并增量修改（增删字段、修改类型、更新索引）
- **日志** — 每次 DDL 操作自动记录到 `sys_table_operation_log` 表

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 4.0.2 | 应用框架 |
| MyBatis-Plus | 3.5.16 | ORM 框架 |
| MySQL | - | 数据库 |
| Lombok | - | 代码简化 |
| Hibernate Validator | 8.0.1.Final | 参数校验 |

---

## 项目结构

```
java-frame-table/                          (父工程，pom 聚合)
│
├── frame-table-core/                      (核心库 JAR，提供自动建表能力)
│   └── src/main/java/org/virgil/core/
│       ├── common/                        自定义注解
│       │   ├── AutoTable.java             标记实体类参与自动建表
│       │   ├── EntityClassAnnotation.java 表备注注解
│       │   ├── EntityFieldAnnotation.java 字段列元数据注解
│       │   └── EntityIndexAnnotation.java 索引定义注解
│       ├── enums/
│       │   └── EntityFieldType.java       SQL 列类型枚举
│       ├── entity/
│       │   ├── BaseEntity.java            基础实体（含审计字段）
│       │   └── TableOperationLog.java     表操作日志实体
│       ├── dto/
│       │   └── BaseDTO.java               基础 DTO（含校验分组）
│       ├── mapper/
│       │   └── TableOperationLogMapper.java
│       ├── service/
│       │   ├── TableOperationLogService.java
│       │   └── impl/TableOperationLogServiceImpl.java
│       ├── config/
│       │   └── MyBatisPlusConfig.java     MyBatis-Plus 自动填充配置
│       ├── runner/
│       │   └── TableAutoCreateRunner.java 核心启动器（DDL 执行引擎）
│       └── util/
│           ├── EntityFieldParser.java     实体字段 → SQL 列定义解析器
│           └── SnowflakeIdGenerator.java  雪花算法 ID 生成器
│
└── frame-table-exec/                      (可执行 Spring Boot 应用，演示项目)
    └── src/main/java/org/virgil/exec/
        ├── JavaFrameTableApplication.java 启动类
        ├── config/WebConfig.java          Web 配置（注册拦截器）
        ├── controller/HelloController.java
        ├── interceptor/TraceIdInterceptor.java
        └── util/TraceIdContext.java
```

---

## 快速开始

### 1. 安装到本地仓库

```bash
git clone https://gitee.com/your-repo/java-frame-table.git
cd java-frame-table
mvn clean install
```

### 2. 在你的项目中引入依赖

在你的 Spring Boot 项目的 `pom.xml` 中添加：

```xml
<!-- 私服仓库（如果 frame-table-core 发布在私服） -->
<repositories>
    <repository>
        <id>virgil-maven</id>
        <url>http://virgil6.com:8081/repository/maven-public/</url>
    </repository>
</repositories>

<!-- 引入核心依赖 -->
<dependency>
    <groupId>org.virgil</groupId>
    <artifactId>frame-table-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. 配置数据源

在 `application.yml` 中配置 MySQL 数据源：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 4. 配置 MyBatis-Plus

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: flagDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 5. 配置扫描包路径

在 `TableAutoCreateRunner` 中，默认扫描包路径为 `org.virgil.javaframetable`。如需修改，可覆盖配置：

```yaml
table:
  base-package: org.virgil.javaframetable
```

### 6. 启动应用

启动 Spring Boot 应用后，框架会自动扫描所有标注了 `@AutoTable` 的实体类并执行 DDL。

---

## 核心功能详解

### 注解体系

框架提供 4 个核心注解来定义数据库表结构：

#### `@AutoTable` — 标记实体类参与自动建表

```java
@AutoTable          // 默认启用
@AutoTable(enable = false)  // 临时禁用自动建表
```

#### `@EntityClassAnnotation` — 定义表备注

```java
@EntityClassAnnotation(remark = "用户信息表")
```

#### `@EntityFieldAnnotation` — 定义列元数据（核心注解）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `text` | String | `""` | 字段显示名称（用于 COMMENT） |
| `type` | EntityFieldType | `DEFAULT` | SQL 列类型（DEFAULT = 按 Java 类型自动推断） |
| `length` | String | `""` | 列长度（仅 VARCHAR/DECIMAL 有效） |
| `not_null` | boolean | `false` | 是否 NOT NULL |
| `default_value` | String | `""` | 列默认值 |
| `comment` | String | `""` | 附加注释（与 text 拼接） |
| `pk` | boolean | `false` | 是否为主键 |

#### `@EntityIndexAnnotation` — 定义索引

```java
@EntityIndexAnnotation(
    ux = {"email"},               // 唯一索引字段
    ix = {"username", "createTime"} // 普通索引字段
)
```

索引命名规则：`idx_unique_{field}`（唯一）、`idx_normal_{field}`（普通）。

### EntityFieldType 枚举 — SQL 类型映射

| 枚举值 | SQL 类型 | 对应 Java 类型（自动推断） |
|--------|----------|--------------------------|
| `DEFAULT` | 根据 Java 类型推断 | - |
| `LONG` | BIGINT | Long / long |
| `TINYINT` | TINYINT | - |
| `VARCHAR` | VARCHAR | String |
| `INT` | INT | Integer / int |
| `BOOLEAN` | TINYINT(1) | Boolean / boolean |
| `DOUBLE` | DOUBLE | Double / double |
| `DECIMAL` | DECIMAL | BigDecimal |
| `DATETIME` | DATETIME | LocalDateTime / Date |
| `DATE` | DATE | LocalDate |
| `TEXT` | TEXT | （默认回退类型） |

> 当 `type = DEFAULT` 时，框架会根据 Java 字段类型自动推断 SQL 类型。显式指定 `type` 优先级更高。

### 自动建表流程

应用启动时，`TableAutoCreateRunner` 按以下流程执行：

```
1. 扫描 basePackage 下所有 @AutoTable 实体类
2. 对每个实体类：
   ├── 表不存在 → 执行 CREATE TABLE + 创建索引
   └── 表已存在 → 执行增量比对：
       ├── 阶段1：比对表备注
       ├── 阶段2：比对索引（增删）
       ├── 阶段3：比对字段（修改/新增）
       ├── 阶段4：补全主键约束
       └── 阶段5：删除废弃字段
3. 所有 DDL 记录到 sys_table_operation_log
```

### 表结构自动升级

当实体类变更后重启应用，框架会自动检测差异：

| 场景 | 执行操作 |
|------|----------|
| 新增字段 | `ALTER TABLE ... ADD COLUMN ...` |
| 修改字段类型/长度 | `ALTER TABLE ... MODIFY COLUMN ...` |
| 字段不再标注 `@EntityFieldAnnotation` | `ALTER TABLE ... DROP COLUMN ...` |
| 表备注变更 | `ALTER TABLE ... COMMENT '...'` |
| 新增索引 | `CREATE INDEX ...` |
| 删除索引 | `DROP INDEX ...` |
| 新增主键 | `ALTER TABLE ... ADD PRIMARY KEY (...)` |

### 操作日志记录

每次 DDL 操作都会记录到 `sys_table_operation_log` 表：

| 字段 | 说明 |
|------|------|
| `table_name` | 表名 |
| `operation_type` | `create`（新建）/ `update`（升级） |
| `version` | 版本号（自动递增） |
| `upgrade_success` | 是否成功 |
| `upgrade_fail_reason` | 失败原因 |
| `upgrade_time` | 操作时间 |
| `execute_sql` | 执行的完整 SQL |

---

## 完整使用示例

### 示例：创建一个用户表

```java
package org.virgil.javaframetable.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.virgil.core.common.AutoTable;
import org.virgil.core.common.EntityClassAnnotation;
import org.virgil.core.common.EntityFieldAnnotation;
import org.virgil.core.common.EntityIndexAnnotation;
import org.virgil.core.enums.EntityFieldType;
import org.virgil.core.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 用户信息表
 */
@AutoTable
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@EntityClassAnnotation(remark = "用户信息表")
@EntityIndexAnnotation(ux = {"email"}, ix = {"username", "createTime"})
@TableName("sys_user")
public class User extends BaseEntity {

    @EntityFieldAnnotation(text = "用户名", type = EntityFieldType.VARCHAR, length = "50", not_null = true)
    private String username;

    @EntityFieldAnnotation(text = "邮箱", type = EntityFieldType.VARCHAR, length = "100", not_null = true)
    private String email;

    @EntityFieldAnnotation(text = "手机号", type = EntityFieldType.VARCHAR, length = "20")
    private String phone;

    @EntityFieldAnnotation(text = "年龄", type = EntityFieldType.INT)
    private Integer age;

    @EntityFieldAnnotation(text = "余额", type = EntityFieldType.DECIMAL, length = "10,2")
    private java.math.BigDecimal balance;

    @EntityFieldAnnotation(text = "是否启用", type = EntityFieldType.BOOLEAN, default_value = "1")
    private Boolean enabled;

    @EntityFieldAnnotation(text = "注册时间", type = EntityFieldType.DATETIME, not_null = true)
    private LocalDateTime registerTime;

    @EntityFieldAnnotation(text = "备注", type = EntityFieldType.TEXT)
    private String remark;
}
```

启动后，框架会自动生成如下 SQL：

```sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    age INT COMMENT '年龄',
    balance DECIMAL(10,2) COMMENT '余额',
    enabled TINYINT(1) DEFAULT '1' COMMENT '是否启用',
    register_time DATETIME NOT NULL COMMENT '注册时间',
    remark TEXT COMMENT '备注',
    create_time DATETIME COMMENT '创建时间',
    create_name VARCHAR(50) COMMENT '创建人',
    create_code VARCHAR(10) COMMENT '创建人工号',
    modify_time DATETIME COMMENT '修改时间',
    modify_name VARCHAR(50) COMMENT '修改人',
    modify_code VARCHAR(10) COMMENT '修改人工号',
    delete_time DATETIME COMMENT '删除时间',
    delete_name VARCHAR(50) COMMENT '删除人',
    delete_code VARCHAR(10) COMMENT '删除人工号',
    flag_deleted TINYINT DEFAULT '0' COMMENT '是否删除标识 | 0：未删除；1：已删除',
    PRIMARY KEY (id)
) COMMENT '用户信息表';

CREATE UNIQUE INDEX idx_unique_email ON sys_user (email(100));
CREATE INDEX idx_normal_username ON sys_user (username(50));
CREATE INDEX idx_normal_create_time ON sys_user (create_time);
```

### 后续升级示例

如果后续需要新增 `avatar` 字段，只需在实体类中添加：

```java
@EntityFieldAnnotation(text = "头像URL", type = EntityFieldType.VARCHAR, length = "255")
private String avatar;
```

重启应用后，框架自动执行：

```sql
ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(255) COMMENT '头像URL';
```

---

## 配置说明

### application.yml 完整配置参考

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

mybatis-plus:
  type-aliases-package: org.virgil.javaframetable.entity
  mapper-locations: classpath*:/**/mapper/xml/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: flagDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 生成 Javadoc API 文档

```bash
# 在项目根目录执行
mvn javadoc:javadoc -Ddoctitle="java-frame-table API" -Dwindowtitle="java-frame-table API"

# 文档生成在各模块的 target/site/apidocs/ 目录下
```

---

## 常见问题

### Q: 如何临时禁用某个表的自动建表？

在实体类上设置 `enable = false`：

```java
@AutoTable(enable = false)
```

### Q: 如何修改扫描的包路径？

`TableAutoCreateRunner` 中默认扫描 `org.virgil.javaframetable`。如需修改，可以继承 `TableAutoCreateRunner` 并覆盖 `run()` 方法中的 `basePackage` 值。

### Q: 表已存在数据，升级会丢失数据吗？

不会。框架使用 `ALTER TABLE` 增量修改，不会删除或重建表。仅在以下情况执行 DROP：
- 实体类中移除了某个字段的 `@EntityFieldAnnotation` 注解
- 该字段不是索引列也不是主键

### Q: 主键策略是什么？

默认使用雪花算法（Snowflake）生成 64 位 Long 型主键。`BaseEntity` 中的 `id` 字段已预置 `@EntityFieldAnnotation(pk = true)`。

### Q: 支持哪些数据库？

当前版本针对 MySQL 语法优化（如 `COMMENT` 语法、索引命名规则等）。其他数据库暂不保证兼容。

### Q: 多数据源场景如何使用？

`TableAutoCreateRunner` 通过 `@Resource` 注入 `DataSource`。在多数据源场景下，需要指定注入的数据源 Bean，或为每个数据源创建独立的 Runner 实例。
