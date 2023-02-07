/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   RRegisterService.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/2
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.service.entitiy.newuser;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.NewUser;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;

public interface NewTbUserService {
    NewUser save(User user, Customer customer, UserCredentials userCredentials) throws ThingsboardException;
    NewUser update(User user, Customer customer) throws ThingsboardException;
    NewUser update(User user, Tenant tenant) throws ThingsboardException;
    void deleteTenant(Tenant tenant) throws ThingsboardException;
    void deleteCustomer(Customer customer, User user) throws ThingsboardException;

    void delete(TenantId tenantId, CustomerId customerId, User tbUser, User user) throws ThingsboardException;

    /**
     * modify user information about authority on tenant
     * @param user request user information
     * @param tenantId the id of new tenant
     * @return new user
     * @throws ThingsboardException e
     */
    User turnOnAuthorityForUser(User user, TenantId tenantId) throws ThingsboardException;

    /**
     * modify user information about authority on customer
     * @param user request user information
     * @param customerId the id of resource customer
     * @return new user
     * @throws ThingsboardException e
     */
    User turnOffAuthorityForUser(User user, CustomerId customerId) throws ThingsboardException;
}
