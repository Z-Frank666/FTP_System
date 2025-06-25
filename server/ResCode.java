package com.Frank.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResCode {
//    public ResCode(Integer code, String message) {
//        this.code = code;
//        this.msg = message;
//    }

    private Integer code;
    private String msg;
}
