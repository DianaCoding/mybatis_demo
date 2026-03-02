package tech.insight.mybatis;
import java.lang.reflect.*;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: pengminwan
 * @CreateTime: 2026-03-02  19:22
 */
public class MySqlSessionFactory {
    // todo 更改成通过配置文件加载
    public static final  String JDBCURL = "jdbc:mysql://localhost:3306/mybatis_db";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "root";
    @SuppressWarnings("all")
    public <T> T getMapper(Class<T> mapperClass) {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{mapperClass}, new MapperInvocationHandler());
    }

    static class MapperInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().startsWith("select")) {
                return invokeSelect(proxy, method, args);
            }
            return null;
        }

        private Object invokeSelect(Object proxy, Method method, Object[] args) {
            String sql = createSelectSql(method);
//            List<String> selectCols = getSelectCols(method.getReturnType());
//            // 通过反射拿到user的三列去确定selecet的列和需要返回的列
//            String sql = "select "+ String.join("," , selectCols)+ " from user where id = ?";
            System.out.println(sql);
            // 这里不应该每一次都拿到一个链接
            try (Connection connection = DriverManager.getConnection(JDBCURL, USERNAME, PASSWORD)) {
                PreparedStatement statement = connection.prepareStatement(sql);
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if(arg instanceof  Integer) {
                        statement.setInt(i + 1, (Integer) arg);
                    }else if(arg instanceof  String) {
                        statement.setString(i + 1, arg.toString());
                    }
                }
                try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
//                        这里的user不应该手动创建，应该动态创建一个接受结果的对象，更换成 解析返回值的函数

//                        User user = new User();
//                        user.setId(resultSet.getInt("id"));
//                        user.setName(resultSet.getString("name"));
//                        user.setAge(resultSet.getInt("age"));
//                        return user;
                        return parseResult(resultSet, method.getReturnType());
                    }
                }
            } catch (SQLException | NoSuchMethodException e) {
                e.printStackTrace();
            }
            return null;
        }

        private Object parseResult(ResultSet resultSet, Class<?> returnType) throws NoSuchMethodException {
            Constructor<?> constructor = returnType.getConstructor();
            try {
                Object instance = constructor.newInstance();
                Field[] declaredFields = returnType.getDeclaredFields();
                for (Field field : declaredFields) {
                    Object column = null;
                    String name = field.getName();
                    if(field.getType() == String.class) {
                        column = resultSet.getString(name);
                    } else if(field.getType() == Integer.class) {
                        column = resultSet.getInt(name);
                    }
                    field.setAccessible(true);
                    field.set(instance, column);
                }
                return instance;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        private String createSelectSql(Method method) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select ");
            List<String> selectCols = getSelectCols(method.getReturnType());
            sqlBuilder.append(String.join("," , selectCols));
            sqlBuilder.append(" from ");
            // 通过反射拿到user的三列去确定selecet的列和需要返回的列
//            String sql = "select "+ String.join("," , selectCols)+ " from user where id = ?";
            String tableName = getSelectTableName(method.getReturnType());
            sqlBuilder.append(tableName);
            sqlBuilder.append(" where ");
//            通过参数来判断查询条件
            String where =  getSelectWhere(method);
            sqlBuilder.append(where);
            return sqlBuilder.toString();

        }

        private String getSelectWhere(Method method) {
            return Arrays.stream(method.getParameters()).map((parameter) -> {
                Param param = parameter.getAnnotation(Param.class);
                String column = param.value();
                String condition = column + " = ?";
                return condition;
            }).collect(Collectors.joining(" and "));
        }

        private String getSelectTableName(Class<?> returnType) {
            Table table = returnType.getAnnotation(Table.class);
            if(table == null) {
                throw new RuntimeException("请为" + returnType.getName() + "添加@Table注解");
            }
            return table.tableName();
        }

        private List<String> getSelectCols(Class<?> returnType) {
            Field[] declaredFields = returnType.getDeclaredFields();
            return Arrays.stream(declaredFields).map(Field::getName).toList();
        }
    }
}