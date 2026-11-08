package egovframework.example.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Arkive 로컬 HSQLDB 데모 전용. 빈 DB만 초기화하고, 기존 DB에는 DDL/시드를 재실행하지 않는다.
 * 외부 DB로 기능을 이식할 때는 이 클래스와 ARKIVE_DB_INIT 테이블을 복사하지 않는다.
 */
final class HsqlDemoDatabaseInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsqlDemoDatabaseInitializer.class);
    private static final String INIT_TABLE = "ARKIVE_DB_INIT";
    private static final String LEGACY_TABLE = "SCHEMA_MIGRATION";
    private static final String APPLICATION_TABLES = "TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME NOT IN ('ARKIVE_DB_INIT', 'SCHEMA_MIGRATION')";

    // 삭제한 구 SQL을 실행하는 목록이 아니다. 전환 직전 체인이 완료된 DB의 1회 인수 조건이다.
    private static final Set<String> LEGACY_SCRIPTS = Set.of(
            "V1__baseline.sql", "V2__board_comment.sql", "V3__message.sql", "V4__board_notice.sql",
            "V5__permission.sql", "V6__mail_notification.sql", "V7__stage2_qna_audit.sql", "V8__leave.sql",
            "V9__totp.sql", "V10__terms.sql", "V11__search_chosung.sql", "V12__user_contact.sql",
            "V13__meeting.sql", "V14__approval.sql", "V15__field_crypto.sql", "V16__survey.sql",
            "V17__sms.sql", "V18__hr.sql", "V30__lecture.sql", "V31__map_guide.sql", "V32__chat.sql",
            "V33__batch.sql", "V34__user_phone_crypto.sql", "V35__board_view_log.sql");

    void initialize(Connection connection, String databasePath) throws IOException, SQLException {
        long started = System.nanoTime();
        SqlScript schema = SqlScript.load("db/schema.sql");
        SqlScript seed = SqlScript.load("db/seed.sql");
        Set<String> tables = tableNames(connection);

        if (tables.contains(INIT_TABLE)) {
            // 보통의 코드 수정·재시작 경로. 메모리 기준 DB도 만들지 않고 읽기만 한다.
            verifyInitializedDatabase(connection, schema, seed, databasePath);
            LOGGER.info("Reusing existing HSQLDB (no DDL or seed execution, {} ms)", elapsedMillis(started));
            return;
        }

        SchemaSnapshot actual = readSchema(connection);
        if (tables.isEmpty() && actual.empty()) {
            SchemaSnapshot expected = referenceSchema(schema, seed);
            initializeEmptyDatabase(connection, schema, seed, expected, databasePath);
            LOGGER.info("Initialized empty HSQLDB with schema.sql and seed.sql ({} ms)", elapsedMillis(started));
            return;
        }

        if (tables.contains(LEGACY_TABLE) && hasCompleteLegacyHistory(connection)) {
            SchemaSnapshot expected = referenceSchema(schema, seed);
            requireMatchingSchema(actual, expected, databasePath);
            recordInitialization(connection, schema, seed, actual.signature(), "READY");
            LOGGER.info("Adopted compatible legacy HSQLDB; existing data and permissions were not reseeded ({} ms)", elapsedMillis(started));
            return;
        }

        throw incompatible(databasePath, "초기화 완료 기록이 없거나 구 버전 적용 이력이 불완전합니다.");
    }

    private void verifyInitializedDatabase(Connection connection, SqlScript schema, SqlScript seed, String databasePath) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT STATUS, SCHEMA_CHECKSUM, SEED_CHECKSUM, SCHEMA_SIGNATURE FROM ARKIVE_DB_INIT WHERE ID = 1")) {
            if (!result.next() || !"READY".equals(result.getString("STATUS"))) {
                throw incompatible(databasePath, "이전 DB 초기화가 완료되지 않았습니다. DDL은 롤백되어 사라진다고 가정할 수 없습니다.");
            }
            if (!schema.checksum().equals(result.getString("SCHEMA_CHECKSUM"))
                    || !seed.checksum().equals(result.getString("SEED_CHECKSUM"))) {
                throw incompatible(databasePath, "schema.sql 또는 seed.sql이 초기화 당시와 다릅니다. 기존 DB를 자동 갱신하거나 재생성하지 않습니다.");
            }
            String signature = result.getString("SCHEMA_SIGNATURE");
            if (!readSchema(connection).signature().equals(signature)) {
                throw incompatible(databasePath, "기존 DB의 테이블·컬럼·제약조건·인덱스 등이 초기화 당시와 다릅니다.");
            }
        }
    }

    private void initializeEmptyDatabase(Connection connection, SqlScript schema, SqlScript seed, SchemaSnapshot expected, String databasePath) throws SQLException {
        // HSQLDB DDL은 자동 커밋한다. 먼저 INITIALIZING을 영구 기록해 부분 생성 DB를
        // 다음 기동에서 빈 DB로 오인하거나 시드를 다시 실행하지 않게 한다.
        recordInitialization(connection, schema, seed, expected.signature(), "INITIALIZING");
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            schema.execute(connection);
            requireMatchingSchema(readSchema(connection), expected, databasePath);
            // 시드 DML과 READY 전환은 같은 트랜잭션이다. 실패하면 완료로 기록하지 않는다.
            seed.execute(connection);
            try (Statement statement = connection.createStatement()) {
                if (statement.executeUpdate("UPDATE ARKIVE_DB_INIT SET STATUS = 'READY', INITIALIZED_AT = CURRENT_TIMESTAMP WHERE ID = 1 AND STATUS = 'INITIALIZING'") != 1) {
                    throw new IllegalStateException("DB initialization state could not be completed.");
                }
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            rollback(connection, e);
            throw new IllegalStateException("DB 최초 초기화 실패. 기존 파일은 삭제하지 않았습니다. 부분 생성 여부와 복구 방법은 docs/ai/project/migration.md를 확인하세요. DB: " + databasePath, e);
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void recordInitialization(Connection connection, SqlScript schema, SqlScript seed, String signature, String status) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE ARKIVE_DB_INIT (ID INTEGER PRIMARY KEY CHECK (ID = 1), SCHEMA_CHECKSUM VARCHAR(64) NOT NULL, SEED_CHECKSUM VARCHAR(64) NOT NULL, SCHEMA_SIGNATURE VARCHAR(64) NOT NULL, STATUS VARCHAR(16) NOT NULL CHECK (STATUS IN ('INITIALIZING', 'READY')), STARTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, INITIALIZED_AT TIMESTAMP)");
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO ARKIVE_DB_INIT (ID, SCHEMA_CHECKSUM, SEED_CHECKSUM, SCHEMA_SIGNATURE, STATUS, INITIALIZED_AT) VALUES (1, ?, ?, ?, ?, CASE WHEN ? = 'READY' THEN CURRENT_TIMESTAMP ELSE NULL END)")) {
                insert.setString(1, schema.checksum());
                insert.setString(2, seed.checksum());
                insert.setString(3, signature);
                insert.setString(4, status);
                insert.setString(5, status);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            rollback(connection, e);
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private boolean hasCompleteLegacyHistory(Connection connection) throws SQLException {
        Set<String> applied = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT VERSION, SCRIPT_NAME FROM SCHEMA_MIGRATION")) {
            while (result.next()) {
                String name = result.getString("SCRIPT_NAME");
                if (LEGACY_SCRIPTS.contains(name) && name.startsWith(result.getString("VERSION") + "__")) {
                    applied.add(name);
                }
            }
        }
        return applied.containsAll(LEGACY_SCRIPTS);
    }

    private SchemaSnapshot referenceSchema(SqlScript schema, SqlScript seed) throws SQLException {
        // 최초 생성과 구 체인의 1회 인수 때만 사용한다. 정상 재시작에는 실행하지 않는다.
        // shutdown=true로 마지막 연결이 닫히면 검사 DB도 해제된다.
        try (Connection reference = DriverManager.getConnection("jdbc:hsqldb:mem:ArkiveInitCheck_" + UUID.randomUUID() + ";shutdown=true", "sa", "")) {
            schema.execute(reference);
            SchemaSnapshot expected = readSchema(reference);
            seed.execute(reference);
            if (!expected.signature().equals(readSchema(reference).signature())) {
                throw new IllegalStateException("seed.sql must contain initial data only; put schema definitions in schema.sql.");
            }
            return expected;
        }
    }

    private void requireMatchingSchema(SchemaSnapshot actual, SchemaSnapshot expected, String databasePath) {
        if (!actual.signature().equals(expected.signature())) {
            throw incompatible(databasePath, "기존 DB 구조가 최신 schema.sql과 일치하지 않습니다. 미적용 변경 또는 부분 생성된 객체를 확인하세요.");
        }
    }

    private Set<String> tableNames(Connection connection) throws SQLException {
        Set<String> tables = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")) {
            while (result.next()) {
                tables.add(result.getString(1));
            }
        }
        return tables;
    }

    private SchemaSnapshot readSchema(Connection connection) throws SQLException {
        // 사용자 데이터는 읽지 않는다. 객체별 메타데이터를 일괄 조회하므로 테이블 수만큼
        // 왕복하거나 재시작마다 CREATE/INSERT를 실행하지 않는다. DB가 만든 제약/인덱스
        // 이름과 현재 시퀀스 값은 비교에서 제외하고 구조·순서·참조·규칙을 비교한다.
        List<String> rows = new ArrayList<>();
        collectRows(connection, "TABLES", "SELECT TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE " + APPLICATION_TABLES, false, rows);
        collectRows(connection, "COLUMNS", "SELECT TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, COLUMN_DEFAULT, IS_NULLABLE, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE, DATETIME_PRECISION, INTERVAL_TYPE, INTERVAL_PRECISION, CHARACTER_SET_SCHEMA, CHARACTER_SET_NAME, COLLATION_SCHEMA, COLLATION_NAME, DOMAIN_SCHEMA, DOMAIN_NAME, MAXIMUM_CARDINALITY, IS_IDENTITY, IDENTITY_GENERATION, IDENTITY_START, IDENTITY_INCREMENT, IDENTITY_MAXIMUM, IDENTITY_MINIMUM, IDENTITY_CYCLE, IS_GENERATED, GENERATION_EXPRESSION FROM INFORMATION_SCHEMA.COLUMNS WHERE " + APPLICATION_TABLES, false, rows);
        collectRows(connection, "CONSTRAINTS", """
                SELECT T.CONSTRAINT_NAME, T.TABLE_NAME, T.CONSTRAINT_TYPE, T.IS_DEFERRABLE, T.INITIALLY_DEFERRED,
                       K.COLUMN_NAME, K.ORDINAL_POSITION, P.TABLE_SCHEMA, P.TABLE_NAME, P.COLUMN_NAME,
                       R.MATCH_OPTION, R.UPDATE_RULE, R.DELETE_RULE, C.CHECK_CLAUSE
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T
                LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE K
                  ON K.CONSTRAINT_SCHEMA = T.CONSTRAINT_SCHEMA AND K.CONSTRAINT_NAME = T.CONSTRAINT_NAME
                LEFT JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS R
                  ON R.CONSTRAINT_SCHEMA = T.CONSTRAINT_SCHEMA AND R.CONSTRAINT_NAME = T.CONSTRAINT_NAME
                LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE P
                  ON P.CONSTRAINT_SCHEMA = R.UNIQUE_CONSTRAINT_SCHEMA AND P.CONSTRAINT_NAME = R.UNIQUE_CONSTRAINT_NAME
                 AND P.ORDINAL_POSITION = K.POSITION_IN_UNIQUE_CONSTRAINT
                LEFT JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS C
                  ON C.CONSTRAINT_SCHEMA = T.CONSTRAINT_SCHEMA AND C.CONSTRAINT_NAME = T.CONSTRAINT_NAME
                WHERE T.TABLE_SCHEMA = 'PUBLIC' AND T.TABLE_NAME NOT IN ('ARKIVE_DB_INIT', 'SCHEMA_MIGRATION')
                """, true, rows);
        collectRows(connection, "INDEXES", "SELECT TABLE_NAME || '.' || INDEX_NAME, TABLE_NAME, NON_UNIQUE, TYPE, ORDINAL_POSITION, COLUMN_NAME, ASC_OR_DESC, FILTER_CONDITION FROM INFORMATION_SCHEMA.SYSTEM_INDEXINFO WHERE TABLE_SCHEM = 'PUBLIC' AND TABLE_NAME NOT IN ('ARKIVE_DB_INIT', 'SCHEMA_MIGRATION')", true, rows);
        collectRows(connection, "SEQUENCES", "SELECT SEQUENCE_NAME, DATA_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE, START_VALUE, MINIMUM_VALUE, MAXIMUM_VALUE, INCREMENT, CYCLE_OPTION FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA = 'PUBLIC'", false, rows);
        collectRows(connection, "TRIGGERS", "SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_SCHEMA, EVENT_OBJECT_TABLE, ACTION_ORDER, ACTION_CONDITION, ACTION_STATEMENT, ACTION_ORIENTATION, ACTION_TIMING, ACTION_REFERENCE_OLD_TABLE, ACTION_REFERENCE_NEW_TABLE, ACTION_REFERENCE_OLD_ROW, ACTION_REFERENCE_NEW_ROW FROM INFORMATION_SCHEMA.TRIGGERS WHERE TRIGGER_SCHEMA = 'PUBLIC'", false, rows);
        collectRows(connection, "VIEWS", "SELECT TABLE_NAME, VIEW_DEFINITION, CHECK_OPTION, IS_UPDATABLE, INSERTABLE_INTO FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_SCHEMA = 'PUBLIC'", false, rows);
        rows.sort(String::compareTo);
        return new SchemaSnapshot(checksum(String.join("\n", rows)), rows.isEmpty());
    }

    private void collectRows(Connection connection, String section, String sql, boolean grouped, List<String> rows) throws SQLException {
        Map<String, Set<String>> groups = new TreeMap<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            int columns = result.getMetaData().getColumnCount();
            while (result.next()) {
                StringBuilder row = new StringBuilder();
                for (int column = grouped ? 2 : 1; column <= columns; column++) {
                    String value = result.getString(column);
                    row.append(value == null ? -1 : value.length()).append(':');
                    if (value != null) {
                        row.append(value);
                    }
                }
                if (grouped) {
                    groups.computeIfAbsent(result.getString(1), key -> new TreeSet<>()).add(row.toString());
                } else {
                    rows.add(section + ":" + row);
                }
            }
        }
        // 복합키/인덱스의 구성은 묶어서 비교하되 엔진이 부여한 객체 이름에는 의존하지 않는다.
        for (Set<String> group : groups.values()) {
            rows.add(section + ":" + String.join("|", group));
        }
    }

    private static IllegalStateException incompatible(String databasePath, String reason) {
        return new IllegalStateException(reason + " DB 파일과 데이터는 자동 삭제하지 않습니다. 앱을 종료한 뒤 별도 갱신을 검토하거나 Globals.hsql_server.DatabasePath를 새 경로로 지정하세요. 보존·복구 절차: docs/ai/project/migration.md. DB: " + databasePath);
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private static String checksum(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private record SchemaSnapshot(String signature, boolean empty) {
    }

    private record SqlScript(byte[] content, String path, String checksum) {
        static SqlScript load(String path) throws IOException {
            try (var input = new ClassPathResource(path).getInputStream()) {
                // 다른 PC의 Git CRLF/LF 설정만으로 기존 DB가 비호환으로 판정되지 않게 한다.
                String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
                return new SqlScript(sql.getBytes(StandardCharsets.UTF_8), path, HsqlDemoDatabaseInitializer.checksum(sql));
            }
        }

        void execute(Connection connection) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ByteArrayResource(content, path), StandardCharsets.UTF_8));
        }
    }
}
