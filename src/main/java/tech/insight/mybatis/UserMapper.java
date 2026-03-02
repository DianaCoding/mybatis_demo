package tech.insight.mybatis;

/**
 * @author: pengminwan
 * @CreateTime: 2026-03-02  19:15
 */
public interface UserMapper {
    // 通过解析这个注解的参数最终得知是要查询id的
    User selectById(@Param("id") int id);
}
