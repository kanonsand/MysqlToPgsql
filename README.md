# MySQL to PostgreSQL SQL Conversion Tool

A MyBatis interceptor that automatically converts MySQL SQL to PostgreSQL format.

## Features

- **Column Name Mapping**: Map MySQL column names to PostgreSQL column names
- **Type Conversion**: TINYINT(1) → BOOLEAN, DATETIME → TIMESTAMP
- **Identifier Quoting**: Automatic PostgreSQL-style quoting ("name")
- **Clause Handlers**: SELECT, FROM, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT
- **JOIN Support**: Multi-table column mapping for JOIN queries

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>mybatistool</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Schema

Create MySQL and PostgreSQL schema files:

**mysql_schema.sql**:
```sql
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_name` VARCHAR(100) NOT NULL,
    `is_active` TINYINT(1) DEFAULT 1,
    PRIMARY KEY (`id`)
);
```

**postgres_schema.sql**:
```sql
CREATE TABLE "user" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,      -- user_name -> name
    "active" BOOLEAN DEFAULT TRUE      -- is_active -> active
);
```

### 3. Register Interceptors

```java
@Configuration
public class MyBatisConfig {
    @Autowired
    public void addInterceptors(SqlSessionFactory sqlSessionFactory) {
        SchemaRegistry registry = createSchemaRegistry();
        ConversionConfig config = new ConversionConfig();
        
        sqlSessionFactory.getConfiguration().addInterceptor(
            new SqlRewriteInterceptor(registry, config));
        sqlSessionFactory.getConfiguration().addInterceptor(
            new ParameterProxyInterceptor(config));
    }
}
```

## Skip Conversion

Sometimes you need to execute raw SQL without conversion. Use `ConversionControl`:

### Programmatic Skip

```java
// Using lambda (recommended)
ConversionControl.skip(() -> {
    mapper.executeRawSql();  // Will not be converted
});

// Manual control
ConversionControl.beginSkip();
try {
    mapper.executeRawSql();
} finally {
    ConversionControl.endSkip();
}
```

### Annotation-based Skip (Spring AOP)

1. Add `spring-boot-starter-aop` dependency

2. Create annotation and aspect in your project:

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipConversion {}

@Aspect
@Component
public class SkipConversionAspect {
    @Around("@annotation(SkipConversion)")
    public Object aroundSkipConversion(ProceedingJoinPoint pjp) throws Throwable {
        ConversionControl.beginSkip();
        try {
            return pjp.proceed();
        } finally {
            ConversionControl.endSkip();
        }
    }
}
```

3. Use the annotation:

```java
@SkipConversion
public void executeRawQuery() {
    // SQL will not be converted
    jdbcTemplate.execute("SELECT * FROM mysql_table");
}
```

### Configuration-based Exclude

```yaml
conversion:
  enabled: true
  exclude-sql-ids:
    - com.example.mapper.RawMapper.*
```

## Column Name Mapping

The tool automatically detects column name mappings based on ordinal position when column names differ between MySQL and PostgreSQL schemas.

| MySQL | PostgreSQL |
|-------|------------|
| user_name | name |
| is_active | active |
| order_no | order_number |

## Supported Conversions

| Feature | MySQL | PostgreSQL |
|---------|-------|------------|
| LIMIT offset, count | `LIMIT 0, 10` | `LIMIT 10 OFFSET 0` |
| Boolean type | `TINYINT(1)` | `BOOLEAN` |
| Auto increment | `AUTO_INCREMENT` | `SERIAL/BIGSERIAL` |
| Identifier quote | `` `name` `` | `"name"` |

## License

MIT
