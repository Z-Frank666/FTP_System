package com.Frank.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Config {
    private Integer id;
    private Integer port;
    private Integer max_connections;
    private Integer timeout;
    private String welcome_msg;
    private String goodbye_msg;
}
