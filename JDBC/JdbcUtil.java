package com.Frank.JDBC;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcUtil {
    // 数据库连接信息
    private static final String URL = "jdbc:mysql://localhost:3306/ftp_user?allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    // 加载数据库驱动
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 关闭资源
    public static void closeResources(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 执行查询操作
    public static List<Object> executeQuery(String sql, Object[] params, Class<?> clazz) {
        List<Object> resultList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);

            // 设置参数
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    preparedStatement.setObject(i + 1, params[i]);
                }
            }

            resultSet = preparedStatement.executeQuery();

            // 获取结果集元数据
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Object obj = clazz.getDeclaredConstructor().newInstance();
                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();

                    // 检查字段名是否在结果集中
                    boolean found = false;
                    for (int i = 1; i <= columnCount; i++) {
                        if (metaData.getColumnName(i).equalsIgnoreCase(fieldName)) {
                            Object value = resultSet.getObject(i);
                            if (field.getType() == Boolean.class && value instanceof Integer) {
                                int intValue = (Integer) value;
                                field.set(obj, intValue == 1);
                            } else {
                                field.set(obj, value);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Warning: Column '" + fieldName + "' not found in result set.");
                    }
                }

                resultList.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }

        return resultList;
    }

    // 执行更新操作（插入、更新、删除）
    public static int executeUpdate(String sql, Object[] params) {
        int affectedRows = 0;
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(sql);

            // 设置参数
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    preparedStatement.setObject(i + 1, params[i]);
                }
            }

            affectedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, null);
        }

        return affectedRows;
    }

    // 测试方法
    public static void main(String[] args) {
        // 示例：查询操作
        String querySql = "SELECT * FROM user WHERE username = ?";
        Object[] queryParams = {"Jack"};
        List<Object> result = executeQuery(querySql, queryParams, User.class);
        System.out.println(result);

        // 新增示例3：插入用户（重点新增部分）
//        String insertSql = "INSERT INTO user(username, password) VALUES (?, ?)";
//        User user = new User("Frank", "123456");
//        Object[] params = {user.getUsername(), user.getPassword()};  // 提取字段值
//        int insertResult = executeUpdate(insertSql, params);
//        System.out.println(insertResult > 0 ? "插入成功" : "插入失败");
//
//        // 示例：更新操作
//        String updateSql = "UPDATE user SET username = ? WHERE id = ?";
//        Object[] updateParams = {"NewName", 1};
//        int affectedRows = executeUpdate(updateSql, updateParams);
//        System.out.println("Affected Rows: " + affectedRows);
    }
}