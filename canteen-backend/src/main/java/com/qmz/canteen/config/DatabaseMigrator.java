package com.qmz.canteen.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库自动迁移器：启动时执行 classpath:db/migration/ 下尚未执行过的增量 SQL，
 * 用 schema_migrations 表记录已执行的脚本，避免重复执行。
 * <p>
 * 小组协作时，谁改了数据库结构就往 db/migration/ 里加一个 V{序号}__描述.sql，
 * 其他人拉代码重启后端即可自动同步，不用再手动跑 SQL。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseMigrator implements ApplicationRunner {

    private static final String MIGRATION_DIR = "classpath:db/migration/*.sql";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Value("${canteen.db-migration.enabled:true}")
    private boolean enabled;

    public DatabaseMigrator(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("数据库自动迁移已关闭（canteen.db-migration.enabled=false）");
            return;
        }
        ensureVersionTable();
        Set<String> applied = appliedVersions();
        List<Migration> migrations = scanMigrations();

        int executed = 0;
        for (Migration migration : migrations) {
            if (applied.contains(migration.version())) {
                continue;
            }
            apply(migration);
            record(migration);
            executed++;
        }
        if (executed > 0) {
            log.info("数据库迁移完成：本次执行 {} 个脚本", executed);
        } else {
            log.debug("数据库迁移：无新增脚本");
        }
    }

    private void ensureVersionTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version      VARCHAR(64) PRIMARY KEY COMMENT '版本号（文件名，如 V1__init）',
                    description  VARCHAR(255) NULL COMMENT '描述',
                    checksum     VARCHAR(64) NULL COMMENT '脚本内容 MD5',
                    executed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间'
                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '数据库迁移记录'
                """);
    }

    private Set<String> appliedVersions() {
        Set<String> versions = new HashSet<>();
        try {
            versions.addAll(jdbcTemplate.queryForList("SELECT version FROM schema_migrations", String.class));
        } catch (Exception e) {
            // 表刚建或查询异常，视为空
        }
        return versions;
    }

    private List<Migration> scanMigrations() {
        List<Migration> result = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(MIGRATION_DIR);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".sql")) {
                    continue;
                }
                result.add(new Migration(filename, resource));
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫描数据库迁移脚本失败", e);
        }
        result.sort(Comparator.comparingInt(Migration::versionNumber)
                .thenComparing(Migration::version));
        return result;
    }

    private void apply(Migration migration) {
        log.info("执行数据库迁移：{}", migration.version());
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, migration.resource());
        } catch (Exception e) {
            // 迁移失败必须中断启动，避免带着不一致的库继续跑
            throw new IllegalStateException("数据库迁移脚本执行失败：" + migration.version(), e);
        }
    }

    private void record(Migration migration) {
        jdbcTemplate.update(
                "INSERT INTO schema_migrations (version, description, checksum) VALUES (?, ?, ?)",
                migration.version(), migration.description(), migration.checksum());
    }

    /** 迁移脚本：文件名、版本号、描述、内容校验和 */
    private record Migration(String version, int versionNumber, String description, String checksum, Resource resource) {
        Migration(String filename, Resource resource) {
            this(filename, extractVersion(filename), extractDescription(filename), checksumOf(resource), resource);
        }
    }

    private static int extractVersion(String filename) {
        Matcher m = VERSION_PATTERN.matcher(filename);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String extractDescription(String filename) {
        Matcher m = VERSION_PATTERN.matcher(filename);
        if (m.find()) {
            String rest = filename.substring(m.end());
            return rest.endsWith(".sql") ? rest.substring(0, rest.length() - 4) : rest;
        }
        return filename.replaceFirst("\\.sql$", "");
    }

    private static String checksumOf(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
