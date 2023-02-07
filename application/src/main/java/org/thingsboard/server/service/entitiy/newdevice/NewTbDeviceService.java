/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   NewDeviceService.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/3
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.service.entitiy.newdevice;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.List;


public interface NewTbDeviceService {

    void assignDeviceToUser(DeviceId deviceId, User user, User currentUser) throws ThingsboardException;
    void unassignDeviceFromUser(Device device, CustomerId customerId, User user) throws ThingsboardException;
    Device save(Device device, Device oldDevice, String accessToken, User user) throws Exception;
    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;
}
