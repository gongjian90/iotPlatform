/*
 * Copyright (c) Union Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   UserInfo.java
 * Author: gongjian   ID:     Version:    Date: 2023/1/12
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.common.data.http;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfo {
    private String idUser;
    private String idSite;
    private Integer accessLevel;
    private String email;
    private String name;
    private Integer receivesAlarmNotifications;
    private String avatar_url;
}
