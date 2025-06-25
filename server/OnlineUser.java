package com.Frank.server;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlineUser {
    private String threadId;
    private String username;
    private String IP;
    private LocalDateTime loginTime;
}
