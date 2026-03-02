package tech.insight.mybatis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author: pengminwan
 * @CreateTime: 2026-03-02  19:26
 */
public class Main {
    public static void main(String[] args) {
        MySqlSessionFactory mySqlSessionFactory = new MySqlSessionFactory();
        UserMapper mapper = mySqlSessionFactory.getMapper(UserMapper.class);
        User user = mapper.selecetById(1);
        System.out.println(user);
    }

    private static User jdbcSelectById(int id) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/mybatis_db";
        String username = "root";
        String password = "root";

        String sql = "select id,name,age from user where id = ?";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            try (java.sql.ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User();
                    user.setId(resultSet.getInt("id"));
                    user.setName(resultSet.getString("name"));
                    user.setAge(resultSet.getInt("age"));
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}