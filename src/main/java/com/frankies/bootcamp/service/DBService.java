package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.EmailAccess;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DBService {
    DataSource ds;
    {
        try {
            ds = (DataSource) new InitialContext().lookup("java:jboss/strava");
            ensureZenBotMessagesTable();
            ensureAthleteAuditLogTable();
            ensureAuthTables();
        } catch (NamingException e) {
            throw new RuntimeException("Failed to initialize DataSource", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ZenBot message storage", e);
        }
    }

    private void ensureZenBotMessagesTable() throws SQLException {
        try (
                Connection connection = ds.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS zenbot_messages (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "athlete_id VARCHAR(50) NOT NULL, " +
                            "athlete_email VARCHAR(255) NULL, " +
                            "athlete_name VARCHAR(255) NULL, " +
                            "conversation_turn INT NOT NULL, " +
                            "prompt TEXT NOT NULL, " +
                            "reply TEXT NOT NULL, " +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "INDEX idx_zenbot_messages_athlete_id (athlete_id), " +
                            "CONSTRAINT fk_zenbot_messages_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id)" +
                            ")"
            );
        }
    }

    private void ensureAthleteAuditLogTable() throws SQLException {
        try (
                Connection connection = ds.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS athlete_audit_log (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "athlete_id VARCHAR(50) NOT NULL, " +
                            "event_type VARCHAR(50) NOT NULL, " +
                            "event_detail VARCHAR(255) NULL, " +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "INDEX idx_athlete_audit_log_athlete_id (athlete_id), " +
                            "INDEX idx_athlete_audit_log_event_type (event_type), " +
                            "CONSTRAINT fk_athlete_audit_log_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id)" +
                            ")"
            );
        }
    }

    private void ensureAuthTables() throws SQLException {
        try (
                Connection connection = ds.getConnection();
                Statement statement = connection.createStatement()
        ) {
            ensureAthleteUserIdColumn(connection, statement);
            ensureNullableAppUserAthleteLink(connection, statement);

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS app_users (" +
                            "id VARCHAR(50) NOT NULL PRIMARY KEY, " +
                            "athlete_id VARCHAR(50) NULL, " +
                            "email VARCHAR(255) NOT NULL UNIQUE, " +
                            "display_name VARCHAR(255) NOT NULL, " +
                            "handle VARCHAR(100) NOT NULL, " +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "INDEX idx_app_users_athlete_id (athlete_id), " +
                            "CONSTRAINT fk_app_users_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id)" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS auth_identities (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "user_id VARCHAR(50) NOT NULL, " +
                            "provider VARCHAR(50) NOT NULL, " +
                            "provider_subject VARCHAR(255) NOT NULL, " +
                            "provider_email VARCHAR(255) NULL, " +
                            "password_hash VARCHAR(255) NULL, " +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "UNIQUE KEY uq_auth_identity_provider (provider, provider_subject), " +
                            "INDEX idx_auth_identity_user_id (user_id), " +
                            "CONSTRAINT fk_auth_identity_user FOREIGN KEY (user_id) REFERENCES app_users(id)" +
                            ")"
            );
        }
    }

    private void ensureNullableAppUserAthleteLink(Connection connection, Statement statement) throws SQLException {
        if (tableExists(connection, "app_users") && isColumnNotNull(connection, "app_users", "athlete_id")) {
            statement.execute("ALTER TABLE app_users MODIFY COLUMN athlete_id VARCHAR(50) NULL");
        }
    }

    private void ensureAthleteUserIdColumn(Connection connection, Statement statement) throws SQLException {
        if (!columnExists(connection, "athletes", "user_id")) {
            statement.execute("ALTER TABLE athletes ADD COLUMN user_id VARCHAR(50) NULL");
        }

        if (!indexExists(connection, "athletes", "idx_athletes_user_id")) {
            statement.execute("CREATE INDEX idx_athletes_user_id ON athletes(user_id)");
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?"
        )) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?"
        )) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean isColumnNotNull(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?"
        )) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && "NO".equalsIgnoreCase(resultSet.getString("is_nullable"));
            }
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?"
        )) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<BootcampAthlete> findAllAthletes() throws SQLException {
        List<BootcampAthlete> user = new ArrayList<>();

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM athletes")
        ) {
            setAthlete(user, statement);
        }
        return user;
    }

    public BootcampAthlete findAthleteByEmail(String email) throws SQLException {
        List<BootcampAthlete> user = new ArrayList<>();

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM athletes where email = ?")
        ) {
            statement.setString(1, email);
            setAthlete(user, statement);
        }
        if(user.isEmpty()){
            return null;
        }
        return user.getFirst();
    }

    public BootcampAthlete findAthleteByUserId(String userId) throws SQLException {
        List<BootcampAthlete> user = new ArrayList<>();

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM athletes where user_id = ? LIMIT 1")
        ) {
            statement.setString(1, userId);
            setAthlete(user, statement);
        }
        if(user.isEmpty()){
            return null;
        }
        return user.getFirst();
    }

    public BootcampAthlete findAthleteByStravaID(String stravaId) throws SQLException {
        List<BootcampAthlete> user = new ArrayList<>();

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM athletes where id = ?")
        ) {
            statement.setString(1, stravaId);
            setAthlete(user, statement);
        }
        if(user.isEmpty()){
            return null;
        }
        return user.getFirst();
    }

    private void setAthlete(List<BootcampAthlete> user, PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                BootcampAthlete athlete = new BootcampAthlete();
                athlete.setId(resultSet.getString("id"));
                athlete.setUserId(resultSet.getString("user_id"));
                athlete.setAccessToken(resultSet.getString("access_token"));
                athlete.setRefreshToken(resultSet.getString("refresh_token"));
                athlete.setExpiresAt(resultSet.getLong("expires_at"));
                athlete.setTokenType(resultSet.getString("token_type"));
                athlete.setExpiresIn(resultSet.getInt("expires_in"));
                athlete.setLastname(resultSet.getString("lastname"));
                athlete.setFirstname(resultSet.getString("firstname"));
                athlete.setEmail(resultSet.getString("email"));
                athlete.setGoal(resultSet.getDouble("start_goal"));
                athlete.setSickWeeks(resultSet.getString("sick_week"));
                user.add(athlete);
            }
        }
    }

    public void saveAthlete(BootcampAthlete athlete) throws SQLException {

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("insert into athletes " +
                        "(access_token, refresh_token, expires_at, token_type, expires_in, lastname, firstname, id, user_id, email, start_goal)" +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "access_token=?, refresh_token=?, expires_at=?, token_type=?, expires_in=?, lastname=?, firstname=?, user_id=COALESCE(VALUES(user_id), user_id), email=COALESCE(VALUES(email), email), start_goal=COALESCE(VALUES(start_goal), start_goal)")
        ) {
            statement.setString(1, athlete.getAccessToken());
            statement.setString(2, athlete.getRefreshToken());
            statement.setLong(3, athlete.getExpiresAt());
            statement.setString(4, athlete.getTokenType());
            statement.setInt(5, athlete.getExpiresIn());
            statement.setString(6, athlete.getLastname());
            statement.setString(7, athlete.getFirstname());
            statement.setString(8, athlete.getId());
            statement.setString(9, athlete.getUserId());
            statement.setString(10, athlete.getEmail());
            if (athlete.getGoal() == null) {
                statement.setNull(11, java.sql.Types.DOUBLE);
            } else {
                statement.setDouble(11, athlete.getGoal());
            }
            statement.setString(12, athlete.getAccessToken());
            statement.setString(13, athlete.getRefreshToken());
            statement.setLong(14, athlete.getExpiresAt());
            statement.setString(15, athlete.getTokenType());
            statement.setInt(16, athlete.getExpiresIn());
            statement.setString(17, athlete.getLastname());
            statement.setString(18, athlete.getFirstname());

            statement.execute();
        }
    }

    public void linkAthleteToUser(String athleteId, String userId) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE athletes SET user_id = ? WHERE id = ? AND (user_id IS NULL OR user_id = '')"
                )
        ) {
            statement.setString(1, userId);
            statement.setString(2, athleteId);
            statement.executeUpdate();
        }
    }

    public void updateAuthUserAthleteId(String userId, String athleteId) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE app_users SET athlete_id = ? WHERE id = ?"
                )
        ) {
            statement.setString(1, athleteId);
            statement.setString(2, userId);
            statement.executeUpdate();
        }
    }

    public String generateAthleteId() {
        return "local-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    public String createAuthUser(String athleteId, String email, String handle, String displayName) throws SQLException {
        String userId = "usr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO app_users (id, athlete_id, email, display_name, handle) VALUES (?, ?, ?, ?, ?)"
                )
        ) {
            statement.setString(1, userId);
            if (athleteId == null || athleteId.isBlank()) {
                statement.setNull(2, java.sql.Types.VARCHAR);
            } else {
                statement.setString(2, athleteId);
            }
            statement.setString(3, email);
            statement.setString(4, displayName);
            statement.setString(5, handle);
            statement.execute();
        }
        return userId;
    }

    public void linkIdentity(String userId, String provider, String providerSubject, String providerEmail, String passwordHash) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO auth_identities (user_id, provider, provider_subject, provider_email, password_hash) VALUES (?, ?, ?, ?, ?)"
                )
        ) {
            statement.setString(1, userId);
            statement.setString(2, provider);
            statement.setString(3, providerSubject);
            statement.setString(4, providerEmail);
            statement.setString(5, passwordHash);
            statement.execute();
        }
    }

    public AuthenticatedUser findAuthUserByEmail(String email) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT u.id, u.athlete_id, u.email, u.display_name, u.handle, i.provider, i.provider_subject " +
                                "FROM app_users u LEFT JOIN auth_identities i ON i.user_id = u.id WHERE LOWER(u.email) = LOWER(?) ORDER BY i.id ASC LIMIT 1"
                )
        ) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    AuthenticatedUser user = new AuthenticatedUser();
                    user.setUserId(resultSet.getString("id"));
                    user.setAthleteId(resultSet.getString("athlete_id"));
                    user.setEmail(resultSet.getString("email"));
                    user.setDisplayName(resultSet.getString("display_name"));
                    user.setHandle(resultSet.getString("handle"));
                    user.setProvider(resultSet.getString("provider"));
                    user.setProviderSubject(resultSet.getString("provider_subject"));
                    return user;
                }
            }
        }
        return null;
    }

    public AuthenticatedUser findAuthUserByProvider(String provider, String providerSubject) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT u.id, u.athlete_id, u.email, u.display_name, u.handle, i.provider, i.provider_subject " +
                                "FROM auth_identities i JOIN app_users u ON u.id = i.user_id WHERE i.provider = ? AND i.provider_subject = ? LIMIT 1"
                )
        ) {
            statement.setString(1, provider);
            statement.setString(2, providerSubject);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    AuthenticatedUser user = new AuthenticatedUser();
                    user.setUserId(resultSet.getString("id"));
                    user.setAthleteId(resultSet.getString("athlete_id"));
                    user.setEmail(resultSet.getString("email"));
                    user.setDisplayName(resultSet.getString("display_name"));
                    user.setHandle(resultSet.getString("handle"));
                    user.setProvider(resultSet.getString("provider"));
                    user.setProviderSubject(resultSet.getString("provider_subject"));
                    return user;
                }
            }
        }
        return null;
    }

    public EmailAccess getEmailAccess() throws SQLException {
        EmailAccess email = new EmailAccess();
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM emailaccess")
        ) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    email.setAccess_token(resultSet.getString("access_token"));
                    email.setClient_ID(resultSet.getString("client_ID"));
                    email.setClient_secret(resultSet.getString("client_secret"));
                    email.setLast_refresh(resultSet.getLong("last_refresh"));
                    email.setRefresh_token(resultSet.getString("refresh_token"));
                }
            }
        }

        return email;
    }

    public void updateEmail(EmailAccess email) throws SQLException {

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("insert into emailaccess " +
                        "(client_ID, client_secret, access_token, refresh_token, last_refresh)" +
                        "values (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "client_ID=?, client_secret=?, access_token=?, refresh_token=?, last_refresh=?")
        ) {
            statement.setString(1, email.getClient_ID());
            statement.setString(2, email.getClient_secret());
            statement.setString(3, email.getAccess_token());
            statement.setString(4, email.getRefresh_token());
            statement.setLong(5, email.getLast_refresh());
            statement.setString(6, email.getClient_ID());
            statement.setString(7, email.getClient_secret());
            statement.setString(8, email.getAccess_token());
            statement.setString(9, email.getRefresh_token());
            statement.setLong(10, email.getLast_refresh());

            statement.execute();
        }
    }

    public void saveZenBotMessage(String athleteId, String athleteEmail, String athleteName,
                                  int conversationTurn, String prompt, String reply) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO zenbot_messages " +
                                "(athlete_id, athlete_email, athlete_name, conversation_turn, prompt, reply) " +
                                "VALUES (?, ?, ?, ?, ?, ?)"
                )
        ) {
            statement.setString(1, athleteId);
            statement.setString(2, athleteEmail);
            statement.setString(3, athleteName);
            statement.setInt(4, conversationTurn);
            statement.setString(5, prompt == null ? "" : prompt);
            statement.setString(6, reply == null ? "" : reply);
            statement.execute();
        }
    }

    public void saveAthleteAuditEvent(String athleteId, String eventType, String eventDetail) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO athlete_audit_log (athlete_id, event_type, event_detail) VALUES (?, ?, ?)"
                )
        ) {
            statement.setString(1, athleteId);
            statement.setString(2, eventType);
            statement.setString(3, eventDetail);
            statement.execute();
        }
    }
}
