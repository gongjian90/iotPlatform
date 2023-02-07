/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   R.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/7
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.common.data.http;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.security.Authority;

@Data
public class R<T> {
    private Boolean success = false;
    private String errors = null;
    private Integer error_code = null;
    private T records = null;

    @ApiModelProperty(position = 1, value = "The JWT Access Token. Used to perform API calls.", example = "AAB254FF67D..")
    private String token;
    @ApiModelProperty(position = 2, value = "The JWT Refresh Token. Used to get new JWT Access Token if old one has expired.", example = "AAB254FF67D..")
    private String refreshToken;
    private Authority scope = null;

    public static <T> R<T> success(T object) {
        R<T> r = new R<>();
        r.records = object;
        r.success = true;
        return r;
    }

    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.errors = msg;
        return r;
    }

    public static <T> R<T> auth(String token, String refreshToken, Authority scope) {
        R<T> r = new R<>();
        r.success = true;
        r.token = token;
        r.refreshToken = refreshToken;
        r.scope = scope;
        return r;
    }
}
