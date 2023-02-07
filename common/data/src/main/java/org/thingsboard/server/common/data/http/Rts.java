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

import lombok.Data;

@Data
public class Rts<T, E> {
    private Boolean success = false;
    private String errors = null;
    private Integer error_code = null;
    private T records = null;
    private E totals = null;

    public static <T, E> Rts<T, E> success(T records, E totals) {
        Rts<T, E> r = new Rts<>();
        r.records = records;
        r.totals = totals;
        r.success = true;
        return r;
    }

    public static <T, E> Rts<T, E> error(String msg) {
        Rts<T, E> r = new Rts<>();
        r.errors = msg;
        return r;
    }
}
