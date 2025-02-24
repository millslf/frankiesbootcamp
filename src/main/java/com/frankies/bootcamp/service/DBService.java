package com.frankies.bootcamp.service;

import com.frankies.bootcamp.model.BootcampAthlete;
import com.frankies.bootcamp.model.EmailAccess;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBService {
    DataSource ds;
    {
        try {
            ds = (DataSource) new InitialContext().lookup("java:jboss/strava");
        } catch (NamingException e) {
            throw new RuntimeException("Failed to initialize DataSource", e);
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

    private void setAthlete(List<BootcampAthlete> user, PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                BootcampAthlete athlete = new BootcampAthlete();
                athlete.setId(resultSet.getString("id"));
                athlete.setAccessToken(resultSet.getString("access_token"));
                athlete.setRefreshToken(resultSet.getString("refresh_token"));
                athlete.setExpiresAt(resultSet.getLong("expires_at"));
                athlete.setTokenType(resultSet.getString("token_type"));
                athlete.setExpiresIn(resultSet.getInt("expires_in"));
                athlete.setLastname(resultSet.getString("lastname"));
                athlete.setFirstname(resultSet.getString("firstname"));
                athlete.setEmail(resultSet.getString("email"));
                athlete.setGoal(resultSet.getDouble("start_goal"));
                user.add(athlete);
            }
        }
    }

    public void saveAthlete(BootcampAthlete athlete) throws SQLException {

        try (
                Connection connection = ds.getConnection();
                PreparedStatement statement = connection.prepareStatement("insert into athletes " +
                        "(access_token, refresh_token, expires_at, token_type, expires_in, lastname, firstname, id)" +
                        "values (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "access_token=?, refresh_token=?, expires_at=?, token_type=?, expires_in=?, lastname=?, firstname=?")
        ) {
            statement.setString(1, athlete.getAccessToken());
            statement.setString(2, athlete.getRefreshToken());
            statement.setLong(3, athlete.getExpiresAt());
            statement.setString(4, athlete.getTokenType());
            statement.setInt(5, athlete.getExpiresIn());
            statement.setString(6, athlete.getLastname());
            statement.setString(7, athlete.getFirstname());
            statement.setString(8, athlete.getId());
            statement.setString(9, athlete.getAccessToken());
            statement.setString(10, athlete.getRefreshToken());
            statement.setLong(11, athlete.getExpiresAt());
            statement.setString(12, athlete.getTokenType());
            statement.setInt(13, athlete.getExpiresIn());
            statement.setString(14, athlete.getLastname());
            statement.setString(15, athlete.getFirstname());

            statement.execute();
        }
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
}
