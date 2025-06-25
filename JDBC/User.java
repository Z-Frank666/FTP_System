package com.Frank.JDBC;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
//    private static final long serialVersionUID = 1L;
    private Integer id;
    private String username;
    private String password;
    private String home;
    private Boolean download;
    private Boolean upload;
    private Boolean deleted;
    private Boolean created;
    private Boolean disabled;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
