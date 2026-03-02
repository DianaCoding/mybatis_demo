package tech.insight.mybatis;

import lombok.Data;

/**
 * @author: pengminwan
 * @CreateTime: 2026-03-02  19:26
 */
@Data
@Table(tableName = "user")
public class User {
    private Integer id;
    private String name;
    private Integer age;
}