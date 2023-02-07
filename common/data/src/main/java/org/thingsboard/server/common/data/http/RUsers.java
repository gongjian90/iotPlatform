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

import java.util.ArrayList;
import java.util.List;

@Data
public class RUsers<T, E> {
    private Boolean success = false;
    private String errors = null;
    private Integer error_code = null;
    private T users = null;
    private List<E> userGroups = null;
    private List<T> invites = new ArrayList<>();
    private List<T> pending = new ArrayList<>();
    private List<E> siteGroups = new ArrayList<>();

    public static <T, E> RUsers<T, E> success(T users, List<E> userGroups) {
        RUsers<T, E> r = new RUsers<>();
        r.users = users;
        r.userGroups = userGroups;
        r.success = true;
        return r;
    }
    public static <T, E> RUsers<T, E> success(T users, List<E> userGroups, List<T> invites) {
        RUsers<T, E> r = new RUsers<>();
        r.users = users;
        r.userGroups = userGroups;
        r.invites = invites;
        r.success = true;
        return r;
    }
    public static <T, E> RUsers<T, E> success(T users, List<E> userGroups, List<T> invites, List<T> pending) {
        RUsers<T, E> r = new RUsers<>();
        r.users = users;
        r.userGroups = userGroups;
        r.invites = invites;
        r.pending = pending;
        r.success = true;
        return r;
    }
    public static <T, E> RUsers<T, E> error(String msg) {
        RUsers<T, E> r = new RUsers<>();
        r.errors = msg;
        return r;
    }
}
