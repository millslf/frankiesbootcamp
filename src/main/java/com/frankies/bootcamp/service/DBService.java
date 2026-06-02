package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.AuthenticatedUser;
import com.frankies.bootcamp.model.EmailAccess;
import com.frankies.bootcamp.model.WeeklyPerformance;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            ensureCompetitionPersistenceTables();
        } catch (NamingException e) {
            throw new RuntimeException("Failed to initialize DataSource", e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ZenBot message storage", e);
        }
    }

    private void ensureCompetitionPersistenceTables() throws SQLException {
        try (
                Connection connection = ds.getConnection();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competitions (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "name VARCHAR(255) NOT NULL, " +
                            "timezone VARCHAR(64) NOT NULL DEFAULT 'Australia/Sydney', " +
                            "start_timestamp BIGINT NOT NULL, " +
                            "end_timestamp BIGINT NULL, " +
                            "status VARCHAR(32) NOT NULL DEFAULT 'active', " +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );

            ensureDefaultCompetition(connection);

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_athlete (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_id BIGINT NOT NULL, " +
                            "athlete_id VARCHAR(50) NOT NULL, " +
                            "role VARCHAR(32) NOT NULL DEFAULT 'member', " +
                            "starting_goal DOUBLE NOT NULL, " +
                            "joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "status VARCHAR(32) NOT NULL DEFAULT 'active', " +
                            "UNIQUE KEY uq_competition_athlete (competition_id, athlete_id), " +
                            "INDEX idx_competition_athlete_athlete_id (athlete_id), " +
                            "CONSTRAINT fk_competition_athlete_comp FOREIGN KEY (competition_id) REFERENCES competitions(id), " +
                            "CONSTRAINT fk_competition_athlete_athlete FOREIGN KEY (athlete_id) REFERENCES athletes(id)" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_activity_detail (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_athlete_id BIGINT NOT NULL, " +
                            "week_number INT NOT NULL, " +
                            "strava_activity_id BIGINT NOT NULL, " +
                            "sport_type VARCHAR(100) NOT NULL, " +
                            "original_distance DOUBLE NULL, " +
                            "original_duration DOUBLE NULL, " +
                            "calculated_distance DOUBLE NOT NULL, " +
                            "UNIQUE KEY uq_comp_activity_detail (competition_athlete_id, strava_activity_id), " +
                            "INDEX idx_comp_activity_detail_week (competition_athlete_id, week_number), " +
                            "CONSTRAINT fk_comp_activity_detail_comp_athlete FOREIGN KEY (competition_athlete_id) REFERENCES competition_athlete(id) ON DELETE CASCADE" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_weekly_stats (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_athlete_id BIGINT NOT NULL, " +
                            "week_number INT NOT NULL, " +
                            "week_start_date DATE NOT NULL, " +
                            "week_end_date DATE NOT NULL, " +
                            "week_goal DOUBLE NOT NULL, " +
                            "total_distance DOUBLE NOT NULL, " +
                            "total_percent_of_goal DOUBLE NOT NULL, " +
                            "distance_left DOUBLE NOT NULL, " +
                            "week_goal_achievement_score DOUBLE NOT NULL, " +
                            "week_progression_bonus DOUBLE NOT NULL, " +
                            "week_score DOUBLE NOT NULL, " +
                            "average_weekly_score DOUBLE NOT NULL, " +
                            "is_sick BOOLEAN NOT NULL DEFAULT FALSE, " +
                            "activities_summary_text TEXT NULL, " +
                            "UNIQUE KEY uq_comp_weekly_stats (competition_athlete_id, week_number), " +
                            "CONSTRAINT fk_comp_weekly_stats_comp_athlete FOREIGN KEY (competition_athlete_id) REFERENCES competition_athlete(id) ON DELETE CASCADE" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_weekly_sport_stats (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_weekly_stats_id BIGINT NOT NULL, " +
                            "sport_type VARCHAR(100) NOT NULL, " +
                            "activity_count INT NOT NULL, " +
                            "calculated_distance_total DOUBLE NOT NULL, " +
                            "original_distance_total DOUBLE NULL, " +
                            "original_duration_total DOUBLE NULL, " +
                            "UNIQUE KEY uq_comp_weekly_sport (competition_weekly_stats_id, sport_type), " +
                            "CONSTRAINT fk_comp_weekly_sport_stats_week FOREIGN KEY (competition_weekly_stats_id) REFERENCES competition_weekly_stats(id) ON DELETE CASCADE" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_summary (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_athlete_id BIGINT NOT NULL, " +
                            "distance_to_date DOUBLE NOT NULL, " +
                            "score_to_date DOUBLE NOT NULL, " +
                            "current_week INT NOT NULL, " +
                            "current_week_goal DOUBLE NOT NULL, " +
                            "last_completed_week INT NOT NULL, " +
                            "original_weekly_goal DOUBLE NOT NULL, " +
                            "latest_week_score DOUBLE NOT NULL, " +
                            "latest_week_percent_of_goal DOUBLE NOT NULL, " +
                            "last_recalculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "UNIQUE KEY uq_comp_summary (competition_athlete_id), " +
                            "CONSTRAINT fk_comp_summary_comp_athlete FOREIGN KEY (competition_athlete_id) REFERENCES competition_athlete(id) ON DELETE CASCADE" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_summary_sport_stats (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_summary_id BIGINT NOT NULL, " +
                            "sport_type VARCHAR(100) NOT NULL, " +
                            "activity_count INT NOT NULL, " +
                            "calculated_distance_total DOUBLE NOT NULL, " +
                            "original_distance_total DOUBLE NULL, " +
                            "original_duration_total DOUBLE NULL, " +
                            "UNIQUE KEY uq_comp_summary_sport (competition_summary_id, sport_type), " +
                            "CONSTRAINT fk_comp_summary_sport_summary FOREIGN KEY (competition_summary_id) REFERENCES competition_summary(id) ON DELETE CASCADE" +
                            ")"
            );

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS competition_honour_roll (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                            "competition_id BIGINT NOT NULL, " +
                            "week_number INT NOT NULL, " +
                            "distance_winner_name VARCHAR(255) NOT NULL, " +
                            "distance_winner_value DOUBLE NOT NULL, " +
                            "percent_winner_name VARCHAR(255) NOT NULL, " +
                            "percent_winner_value DOUBLE NOT NULL, " +
                            "UNIQUE KEY uq_comp_honour_roll (competition_id, week_number), " +
                            "CONSTRAINT fk_comp_honour_roll_comp FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE CASCADE" +
                            ")"
            );
        }
    }

    private void ensureDefaultCompetition(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO competitions (id, name, timezone, start_timestamp, end_timestamp, status) " +
                        "SELECT 1, 'Frankies Bootcamp', 'Australia/Sydney', ?, NULL, 'active' " +
                        "WHERE NOT EXISTS (SELECT 1 FROM competitions WHERE id = 1)"
        )) {
            statement.setLong(1, com.frankies.bootcamp.constant.BootcampConstants.START_TIMESTAMP);
            statement.executeUpdate();
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

    public long ensureCompetitionAthlete(String athleteId, Double startingGoal) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO competition_athlete (competition_id, athlete_id, role, starting_goal, status) " +
                                "VALUES (1, ?, ?, ?, 'active') " +
                                "ON DUPLICATE KEY UPDATE starting_goal = VALUES(starting_goal)",
                        Statement.RETURN_GENERATED_KEYS
                )
        ) {
            insert.setString(1, athleteId);
            insert.setString(2, isFirstCompetitionAthlete(connection) ? "admin" : "member");
            insert.setDouble(3, startingGoal == null ? 0.0 : startingGoal);
            insert.executeUpdate();
        }

        try (
                Connection connection = ds.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT id FROM competition_athlete WHERE competition_id = 1 AND athlete_id = ?"
                )
        ) {
            select.setString(1, athleteId);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }
        throw new SQLException("Unable to find competition_athlete for athlete " + athleteId);
    }

    private boolean isFirstCompetitionAthlete(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM competition_athlete WHERE competition_id = 1")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 0;
            }
        }
    }

    public void replacePersistentCompetitionState(long competitionAthleteId,
                                                  List<PersistentActivityProcessService.PersistentActivityDetailRow> activityRows,
                                                  List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows,
                                                  PersistentActivityProcessService.PersistentSummaryRow summaryRow,
                                                  Map<String, PersistentActivityProcessService.PersistentSummarySportRow> summarySportRows) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            deletePersistentCompetitionState(connection, competitionAthleteId);
            insertActivityRows(connection, competitionAthleteId, activityRows);
            Map<Integer, Long> weeklyIds = insertWeeklyRows(connection, competitionAthleteId, weeklyRows);
            insertWeeklySportRows(connection, weeklyRows, weeklyIds);
            long summaryId = insertSummaryRow(connection, competitionAthleteId, summaryRow);
            insertSummarySportRows(connection, summaryId, summarySportRows);
        }
    }

    public void replaceCompetitionHonourRoll(long competitionId,
                                             Map<Integer, PersistentActivityProcessService.PersistentHonourRollRow> honourRollRows) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM competition_honour_roll WHERE competition_id = ?")) {
                delete.setLong(1, competitionId);
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO competition_honour_roll (competition_id, week_number, distance_winner_name, distance_winner_value, percent_winner_name, percent_winner_value) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (PersistentActivityProcessService.PersistentHonourRollRow row : honourRollRows.values()) {
                    insert.setLong(1, competitionId);
                    insert.setInt(2, row.weekNumber());
                    insert.setString(3, row.distanceWinnerName());
                    insert.setDouble(4, row.distanceWinnerValue());
                    insert.setString(5, row.percentWinnerName());
                    insert.setDouble(6, row.percentWinnerValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    public Map<String, HashMap<String, Double>> getPersistentLeaderboardSummaries(long competitionId) throws SQLException {
        HashMap<String, Double> currentWeekPercentageOfGoalSummary = new HashMap<>();
        HashMap<String, Double> currentYearlyScoreSummary = new HashMap<>();

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT a.firstname, cs.score_to_date, cs.latest_week_percent_of_goal " +
                                "FROM competition_summary cs " +
                                "JOIN competition_athlete ca ON ca.id = cs.competition_athlete_id " +
                                "JOIN athletes a ON a.id = ca.athlete_id " +
                                "WHERE ca.competition_id = ? AND ca.status = 'active'"
                )
        ) {
            statement.setLong(1, competitionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String firstname = resultSet.getString("firstname");
                    currentYearlyScoreSummary.put(firstname, resultSet.getDouble("score_to_date"));
                    currentWeekPercentageOfGoalSummary.put(firstname, resultSet.getDouble("latest_week_percent_of_goal"));
                }
            }
        }

        Map<String, HashMap<String, Double>> summaries = new HashMap<>();
        summaries.put(com.frankies.bootcamp.constant.BootcampConstants.currentWeekPercentageOfGoalSummary, sortByValue(currentWeekPercentageOfGoalSummary));
        summaries.put(com.frankies.bootcamp.constant.BootcampConstants.currentYearlyScoreSummary, sortByValue(currentYearlyScoreSummary));
        return summaries;
    }

    public HashMap<Integer, HashMap<String, Double>> getPersistentHonourRollTotalDistance(long competitionId) throws SQLException {
        return getPersistentHonourRollMap(competitionId, true);
    }

    public HashMap<Integer, HashMap<String, Double>> getPersistentHonourRollPercentageOfGoal(long competitionId) throws SQLException {
        return getPersistentHonourRollMap(competitionId, false);
    }

    public List<PersistentActivityProcessService.PersistentActivityDetailRow> getPersistentActivityRows(String athleteId) throws SQLException {
        List<PersistentActivityProcessService.PersistentActivityDetailRow> rows = new ArrayList<>();
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT cad.week_number, cad.strava_activity_id, cad.sport_type, cad.original_distance, cad.original_duration, cad.calculated_distance " +
                                "FROM competition_activity_detail cad " +
                                "JOIN competition_athlete ca ON ca.id = cad.competition_athlete_id " +
                                "WHERE ca.competition_id = 1 AND ca.athlete_id = ? " +
                                "ORDER BY cad.week_number ASC, cad.strava_activity_id ASC"
                )
        ) {
            statement.setString(1, athleteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new PersistentActivityProcessService.PersistentActivityDetailRow(
                            resultSet.getInt("week_number"),
                            resultSet.getLong("strava_activity_id"),
                            resultSet.getString("sport_type"),
                            getNullableDouble(resultSet, "original_distance"),
                            getNullableDouble(resultSet, "original_duration"),
                            resultSet.getDouble("calculated_distance")
                    ));
                }
            }
        }
        return rows;
    }

    public Map<Integer, WeeklyPerformance> getPersistentAthleteHistory(String athleteId) throws SQLException {
        Map<Integer, WeeklyPerformance> history = new LinkedHashMap<>();
        Map<Integer, Long> weeklyIds = new HashMap<>();

        try (
                Connection connection = ds.getConnection();
                PreparedStatement weeklyStatement = connection.prepareStatement(
                        "SELECT cws.id, cws.week_number, cws.week_goal, cws.total_distance, cws.total_percent_of_goal, cws.week_goal_achievement_score, cws.week_progression_bonus, cws.week_score, cws.average_weekly_score, cws.is_sick " +
                                "FROM competition_weekly_stats cws " +
                                "JOIN competition_athlete ca ON ca.id = cws.competition_athlete_id " +
                                "WHERE ca.competition_id = 1 AND ca.athlete_id = ? " +
                                "ORDER BY cws.week_number ASC"
                )
        ) {
            weeklyStatement.setString(1, athleteId);
            try (ResultSet resultSet = weeklyStatement.executeQuery()) {
                while (resultSet.next()) {
                    int weekNumber = resultSet.getInt("week_number");
                    WeeklyPerformance week = new WeeklyPerformance(
                            "Week" + weekNumber,
                            0L,
                            resultSet.getDouble("week_goal"),
                            -1.0
                    );
                    week.setPersistedValues(
                            resultSet.getDouble("total_distance"),
                            resultSet.getDouble("total_percent_of_goal"),
                            resultSet.getDouble("week_goal_achievement_score"),
                            resultSet.getDouble("week_progression_bonus"),
                            resultSet.getDouble("week_score"),
                            resultSet.getDouble("average_weekly_score"),
                            resultSet.getBoolean("is_sick")
                    );
                    history.put(weekNumber, week);
                    weeklyIds.put(weekNumber, resultSet.getLong("id"));
                }
            }

            try (PreparedStatement sportStatement = connection.prepareStatement(
                    "SELECT cwss.competition_weekly_stats_id, cwss.sport_type, cwss.activity_count, cwss.calculated_distance_total, cwss.original_distance_total, cwss.original_duration_total " +
                            "FROM competition_weekly_sport_stats cwss " +
                            "JOIN competition_weekly_stats cws ON cws.id = cwss.competition_weekly_stats_id " +
                            "JOIN competition_athlete ca ON ca.id = cws.competition_athlete_id " +
                            "WHERE ca.competition_id = 1 AND ca.athlete_id = ?"
            )) {
                sportStatement.setString(1, athleteId);
                try (ResultSet resultSet = sportStatement.executeQuery()) {
                    while (resultSet.next()) {
                        long weeklyStatsId = resultSet.getLong("competition_weekly_stats_id");
                        Integer weekNumber = weeklyIds.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(weeklyStatsId))
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse(null);
                        if (weekNumber == null) {
                            continue;
                        }

                        WeeklyPerformance week = history.get(weekNumber);
                        if (week == null) {
                            continue;
                        }

                        week.setPersistedSportTotals(
                                resultSet.getString("sport_type"),
                                resultSet.getInt("activity_count"),
                                resultSet.getDouble("calculated_distance_total"),
                                getNullableDouble(resultSet, "original_distance_total"),
                                getNullableDouble(resultSet, "original_duration_total")
                        );
                    }
                }
            }
        }

        return history;
    }

    public PersistentAthleteSummarySnapshot getPersistentAthleteSummarySnapshot(String athleteId) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT a.firstname, cs.distance_to_date, cs.score_to_date, cs.original_weekly_goal " +
                                "FROM competition_summary cs " +
                                "JOIN competition_athlete ca ON ca.id = cs.competition_athlete_id " +
                                "JOIN athletes a ON a.id = ca.athlete_id " +
                                "WHERE ca.competition_id = 1 AND ca.athlete_id = ?"
                )
        ) {
            statement.setString(1, athleteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new PersistentAthleteSummarySnapshot(
                            resultSet.getString("firstname"),
                            resultSet.getDouble("distance_to_date"),
                            resultSet.getDouble("score_to_date"),
                            resultSet.getDouble("original_weekly_goal")
                    );
                }
            }
        }

        return null;
    }

    public record PersistentAthleteSummarySnapshot(
            String athleteFirstName,
            double distanceToDate,
            double scoreToDate,
            double originalWeeklyGoal
    ) {
    }

    public void replacePersistentCompetitionStateWithActivities(long competitionAthleteId,
                                                                List<PersistentActivityProcessService.PersistentActivityDetailRow> activityRows,
                                                                List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows,
                                                                PersistentActivityProcessService.PersistentSummaryRow summaryRow,
                                                                Map<String, PersistentActivityProcessService.PersistentSummarySportRow> summarySportRows) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            deletePersistentCompetitionDerivedState(connection, competitionAthleteId);
            insertActivityRows(connection, competitionAthleteId, activityRows);
            Map<Integer, Long> weeklyIds = insertWeeklyRows(connection, competitionAthleteId, weeklyRows);
            insertWeeklySportRows(connection, weeklyRows, weeklyIds);
            long summaryId = insertSummaryRow(connection, competitionAthleteId, summaryRow);
            insertSummarySportRows(connection, summaryId, summarySportRows);
        }
    }

    public void addPersistentActivityRow(String athleteId,
                                         PersistentActivityProcessService.PersistentActivityDetailRow row) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO competition_activity_detail (competition_athlete_id, week_number, strava_activity_id, sport_type, original_distance, original_duration, calculated_distance) " +
                                "SELECT ca.id, ?, ?, ?, ?, ?, ? FROM competition_athlete ca WHERE ca.competition_id = 1 AND ca.athlete_id = ? " +
                                "ON DUPLICATE KEY UPDATE week_number = VALUES(week_number), sport_type = VALUES(sport_type), original_distance = VALUES(original_distance), original_duration = VALUES(original_duration), calculated_distance = VALUES(calculated_distance)"
                )
        ) {
            statement.setInt(1, row.weekNumber());
            statement.setLong(2, row.stravaActivityId());
            statement.setString(3, row.sportType());
            if (row.originalDistance() == null) statement.setNull(4, java.sql.Types.DOUBLE); else statement.setDouble(4, row.originalDistance());
            if (row.originalDuration() == null) statement.setNull(5, java.sql.Types.DOUBLE); else statement.setDouble(5, row.originalDuration());
            statement.setDouble(6, row.calculatedDistance());
            statement.setString(7, athleteId);
            statement.executeUpdate();
        }
    }

    public void deletePersistentActivityRow(String athleteId, long activityId) throws SQLException {
        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE cad FROM competition_activity_detail cad " +
                                "JOIN competition_athlete ca ON ca.id = cad.competition_athlete_id " +
                                "WHERE ca.competition_id = 1 AND ca.athlete_id = ? AND cad.strava_activity_id = ?"
                )
        ) {
            statement.setString(1, athleteId);
            statement.setLong(2, activityId);
            statement.executeUpdate();
        }
    }

    private HashMap<Integer, HashMap<String, Double>> getPersistentHonourRollMap(long competitionId, boolean distance) throws SQLException {
        HashMap<Integer, HashMap<String, Double>> results = new HashMap<>();
        String nameColumn = distance ? "distance_winner_name" : "percent_winner_name";
        String valueColumn = distance ? "distance_winner_value" : "percent_winner_value";

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT week_number, " + nameColumn + " AS winner_name, " + valueColumn + " AS winner_value " +
                                "FROM competition_honour_roll WHERE competition_id = ? ORDER BY week_number ASC"
                )
        ) {
            statement.setLong(1, competitionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    HashMap<String, Double> winner = new LinkedHashMap<>();
                    winner.put(resultSet.getString("winner_name"), resultSet.getDouble("winner_value"));
                    results.put(resultSet.getInt("week_number"), winner);
                }
            }
        }
        return results;
    }

    private HashMap<String, Double> sortByValue(HashMap<String, Double> map) {
        return ActivityProcessService.entriesSortedByValues(map).stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
    }

    private Double getNullableDouble(ResultSet resultSet, String columnName) throws SQLException {
        double value = resultSet.getDouble(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private void deletePersistentCompetitionDerivedState(Connection connection, long competitionAthleteId) throws SQLException {
        try (PreparedStatement deleteSummary = connection.prepareStatement(
                "DELETE css FROM competition_summary_sport_stats css JOIN competition_summary cs ON css.competition_summary_id = cs.id WHERE cs.competition_athlete_id = ?")) {
            deleteSummary.setLong(1, competitionAthleteId);
            deleteSummary.executeUpdate();
        }
        try (PreparedStatement deleteSummary = connection.prepareStatement("DELETE FROM competition_summary WHERE competition_athlete_id = ?")) {
            deleteSummary.setLong(1, competitionAthleteId);
            deleteSummary.executeUpdate();
        }
        try (PreparedStatement deleteWeeklySport = connection.prepareStatement(
                "DELETE cwss FROM competition_weekly_sport_stats cwss JOIN competition_weekly_stats cws ON cwss.competition_weekly_stats_id = cws.id WHERE cws.competition_athlete_id = ?")) {
            deleteWeeklySport.setLong(1, competitionAthleteId);
            deleteWeeklySport.executeUpdate();
        }
        try (PreparedStatement deleteWeekly = connection.prepareStatement("DELETE FROM competition_weekly_stats WHERE competition_athlete_id = ?")) {
            deleteWeekly.setLong(1, competitionAthleteId);
            deleteWeekly.executeUpdate();
        }
    }

    private void deletePersistentCompetitionState(Connection connection, long competitionAthleteId) throws SQLException {
        deletePersistentCompetitionDerivedState(connection, competitionAthleteId);
        try (PreparedStatement deleteActivity = connection.prepareStatement("DELETE FROM competition_activity_detail WHERE competition_athlete_id = ?")) {
            deleteActivity.setLong(1, competitionAthleteId);
            deleteActivity.executeUpdate();
        }
    }

    private void insertActivityRows(Connection connection,
                                    long competitionAthleteId,
                                    List<PersistentActivityProcessService.PersistentActivityDetailRow> activityRows) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO competition_activity_detail (competition_athlete_id, week_number, strava_activity_id, sport_type, original_distance, original_duration, calculated_distance) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (PersistentActivityProcessService.PersistentActivityDetailRow row : activityRows) {
                statement.setLong(1, competitionAthleteId);
                statement.setInt(2, row.weekNumber());
                statement.setLong(3, row.stravaActivityId());
                statement.setString(4, row.sportType());
                if (row.originalDistance() == null) statement.setNull(5, java.sql.Types.DOUBLE); else statement.setDouble(5, row.originalDistance());
                if (row.originalDuration() == null) statement.setNull(6, java.sql.Types.DOUBLE); else statement.setDouble(6, row.originalDuration());
                statement.setDouble(7, row.calculatedDistance());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Map<Integer, Long> insertWeeklyRows(Connection connection,
                                                long competitionAthleteId,
                                                List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows) throws SQLException {
        Map<Integer, Long> ids = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO competition_weekly_stats (competition_athlete_id, week_number, week_start_date, week_end_date, week_goal, total_distance, total_percent_of_goal, distance_left, week_goal_achievement_score, week_progression_bonus, week_score, average_weekly_score, is_sick, activities_summary_text) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            for (PersistentActivityProcessService.PersistentWeeklyRow row : weeklyRows) {
                statement.setLong(1, competitionAthleteId);
                statement.setInt(2, row.weekNumber());
                statement.setDate(3, Date.valueOf(row.weekStartDate()));
                statement.setDate(4, Date.valueOf(row.weekEndDate()));
                statement.setDouble(5, row.weekGoal());
                statement.setDouble(6, row.totalDistance());
                statement.setDouble(7, row.totalPercentOfGoal());
                statement.setDouble(8, row.distanceLeft());
                statement.setDouble(9, row.weekGoalAchievementScore());
                statement.setDouble(10, row.weekProgressionBonus());
                statement.setDouble(11, row.weekScore());
                statement.setDouble(12, row.averageWeeklyScore());
                statement.setBoolean(13, row.isSick());
                statement.setString(14, row.activitiesSummaryText());
                statement.addBatch();
            }
            statement.executeBatch();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                int index = 0;
                while (keys.next() && index < weeklyRows.size()) {
                    ids.put(weeklyRows.get(index).weekNumber(), keys.getLong(1));
                    index++;
                }
            }
        }
        return ids;
    }

    private void insertWeeklySportRows(Connection connection,
                                       List<PersistentActivityProcessService.PersistentWeeklyRow> weeklyRows,
                                       Map<Integer, Long> weeklyIds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO competition_weekly_sport_stats (competition_weekly_stats_id, sport_type, activity_count, calculated_distance_total, original_distance_total, original_duration_total) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (PersistentActivityProcessService.PersistentWeeklyRow weeklyRow : weeklyRows) {
                Long weeklyId = weeklyIds.get(weeklyRow.weekNumber());
                if (weeklyId == null) continue;
                for (PersistentActivityProcessService.PersistentWeeklySportRow sportRow : weeklyRow.sportRows().values()) {
                    statement.setLong(1, weeklyId);
                    statement.setString(2, sportRow.sportType());
                    statement.setInt(3, sportRow.activityCount());
                    statement.setDouble(4, sportRow.calculatedDistanceTotal());
                    if (sportRow.originalDistanceTotal() == null) statement.setNull(5, java.sql.Types.DOUBLE); else statement.setDouble(5, sportRow.originalDistanceTotal());
                    if (sportRow.originalDurationTotal() == null) statement.setNull(6, java.sql.Types.DOUBLE); else statement.setDouble(6, sportRow.originalDurationTotal());
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private long insertSummaryRow(Connection connection,
                                  long competitionAthleteId,
                                  PersistentActivityProcessService.PersistentSummaryRow summaryRow) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO competition_summary (competition_athlete_id, distance_to_date, score_to_date, current_week, current_week_goal, last_completed_week, original_weekly_goal, latest_week_score, latest_week_percent_of_goal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setLong(1, competitionAthleteId);
            statement.setDouble(2, summaryRow.distanceToDate());
            statement.setDouble(3, summaryRow.scoreToDate());
            statement.setInt(4, summaryRow.currentWeek());
            statement.setDouble(5, summaryRow.currentWeekGoal());
            statement.setInt(6, summaryRow.lastCompletedWeek());
            statement.setDouble(7, summaryRow.originalWeeklyGoal());
            statement.setDouble(8, summaryRow.latestWeekScore());
            statement.setDouble(9, summaryRow.latestWeekPercentOfGoal());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Unable to insert competition_summary row");
    }

    private void insertSummarySportRows(Connection connection,
                                        long summaryId,
                                        Map<String, PersistentActivityProcessService.PersistentSummarySportRow> summarySportRows) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO competition_summary_sport_stats (competition_summary_id, sport_type, activity_count, calculated_distance_total, original_distance_total, original_duration_total) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (PersistentActivityProcessService.PersistentSummarySportRow row : summarySportRows.values()) {
                statement.setLong(1, summaryId);
                statement.setString(2, row.sportType());
                statement.setInt(3, row.activityCount());
                statement.setDouble(4, row.calculatedDistanceTotal());
                if (row.originalDistanceTotal() == null) statement.setNull(5, java.sql.Types.DOUBLE); else statement.setDouble(5, row.originalDistanceTotal());
                if (row.originalDurationTotal() == null) statement.setNull(6, java.sql.Types.DOUBLE); else statement.setDouble(6, row.originalDurationTotal());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
