/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DefaultRegisterService.java
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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;


@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultNewTbUserService extends AbstractTbEntityService implements NewTbUserService {

    @Override
    @Transactional
    public NewUser save(User user, Customer customer, UserCredentials userCredentials) throws ThingsboardException {
        NewUser result = new NewUser();
        try {
            // 保存用户信息表
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(TenantId.SYS_TENANT_ID);   // 同级关系，客户的租户id为系统默认
            user.setCustomerId(CustomerId.SYS_CUSTOMER_ID); // 先赋值，下面更新
            User saveUser = checkNotNull(userService.saveUserNoValidate(user));
            // 保存客户信息
            Customer savedCustomer = newTbCustomerService.savedCustomer(TenantId.SYS_TENANT_ID, saveUser, customer);
            result.setCustomer(savedCustomer);
            // 更新用户信息表
            saveUser.setCustomerId(savedCustomer.getId()); // 更新客户id
            User saveUserFinished = checkNotNull(userService.saveUserNoValidate(saveUser));
            result.setUser(saveUserFinished);
            // 保存登录信息表
            userCredentials.setUserId(saveUser.getId());
            userCredentials.setPassword(passwordEncoder.encode(userCredentials.getPassword()));
            userCredentials.setEnabled(true);
            UserCredentials saveUserCredentials = checkNotNull(userService.saveUserCredentialsNoAuth(TenantId.SYS_TENANT_ID, userCredentials));
            result.setUserCredentials(saveUserCredentials);
        } catch (ThingsboardException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch (DataIntegrityViolationException ex) {
            log.error(ex.getMessage(), ex);
            Throwable throwable = ex.getCause();
            while (throwable.getCause() != null) {
                throwable = throwable.getCause();
            }
            String localizedMessage = throwable.getMessage();
            if (StringUtils.isNotBlank(localizedMessage) && localizedMessage.contains("详细：")) {
                String[] split = localizedMessage.split("详细：");
                String msg = split[1];
                if (msg.contains("=")) {
                    msg = msg.split("=")[1];
                }
                throw new ThingsboardException(msg, ThingsboardErrorCode.GENERAL);
            }
            throw new ThingsboardException("系统维护中，请联系系统管理员！", ThingsboardErrorCode.GENERAL);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
        return result;
    }

    @Override
    @Transactional
    public NewUser update(User user, Customer customer) throws ThingsboardException {
        NewUser newUser = new NewUser();
        // 保存用户信息表
        User saveUser = checkNotNull(userService.saveUserNoValidate(user));
        newUser.setUser(saveUser);
        // 保存客户信息
        Customer savedCustomer = checkNotNull(newTbCustomerService.savedCustomer(user.getTenantId(), saveUser, customer));
        newUser.setTenant(null);
        Tenant tenant = tenantService.findTenantByEmail(user.getEmail()); // 修改邮箱的话，就产生一条冗余数据就好
        if (null != tenant) {
            newTbTenantService.updateTenant(tenant, customer);
            newUser.setTenant(checkNotNull(tenantService.saveTenant(tenant)));
        }
        newUser.setCustomer(savedCustomer);
        return newUser;
    }

    @Override
    @Transactional
    public NewUser update(User user, Tenant tenant) throws ThingsboardException {
        NewUser newUser = new NewUser();
        // 保存用户信息表
        User saveUser = checkNotNull(userService.saveUserNoValidate(user));
        newUser.setUser(saveUser);
        // 保存客户信息
        Tenant saveTenant = checkNotNull(tenantService.saveTenant(tenant));
        newUser.setCustomer(null);
        Customer customer = customerService.findCustomerByEmail(user.getEmail());
        if (null != customer) {
            newTbCustomerService.updateCustomer(customer, tenant);
            newUser.setCustomer(checkNotNull(newTbCustomerService.savedCustomer(user.getTenantId(), saveUser, customer)));
        }

        newUser.setTenant(saveTenant);
        return newUser;
    }

    @Override
    public void deleteTenant(Tenant tenant) throws ThingsboardException {
        try {
            tbTenantService.delete(tenant);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public void deleteCustomer(Customer customer, User user) throws ThingsboardException {
        try {
            tbCustomerService.delete(customer, user);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public void delete(TenantId tenantId, CustomerId customerId, User tbUser, User user) throws ThingsboardException {
        try {
            tbUserService.delete(tenantId, customerId, tbUser, user);
        } catch (ThingsboardException e) {
            log.error(e.getMessage(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public User turnOnAuthorityForUser(User user, TenantId tenantId) throws ThingsboardException {
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setCustomerId(CustomerId.SYS_CUSTOMER_ID); // 同级关系，租户的客户id为系统默认；后面意识到，customer和tenant没有既定的对应关系
        return checkNotNull(userService.saveUserNoValidate(user));
    }

    @Override
    public User turnOffAuthorityForUser(User user, CustomerId customerId) throws ThingsboardException {
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(TenantId.SYS_TENANT_ID);
        user.setCustomerId(customerId);
        return checkNotNull(userService.saveUserNoValidate(user));
    }
}
