/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DefaultNewCustomerService.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/6
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.service.entitiy.newcustomer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultNewTbCustomerService extends AbstractTbEntityService implements NewTbCustomerService {
    /**
     * @param tenantId the tenantId
     * @param user the user has been saved
     * @param customer the customer request
     * @return the consumer has been saved
     * @throws ThingsboardException is self Exception
     */
    @Override
    public Customer savedCustomer(TenantId tenantId, User user, Customer customer) throws ThingsboardException {
        ActionType actionType = customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        Customer savedCustomer;
        try {
            customer.setTenantId(tenantId);
            customer.setEmail(user.getEmail());
            savedCustomer = checkNotNull(customerService.saveCustomer(customer));
            autoCommit(user, savedCustomer.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedCustomer.getId(), savedCustomer, null, actionType, user);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), customer, actionType, user, e);
            throw new ThingsboardException(e.getMessage(), e, ThingsboardErrorCode.GENERAL);
        }
        return savedCustomer;
    }

    @Override
    public void updateCustomer(Customer customer, Tenant tenant) {
        customer.setTitle(tenant.getTitle());
        customer.setEmail(tenant.getEmail());
        customer.setAddress(tenant.getAddress());
        customer.setAddress2(tenant.getAddress2());
        customer.setCity(tenant.getCity());
        customer.setCountry(tenant.getCountry());
        customer.setState(tenant.getState());
        customer.setPhone(tenant.getPhone());
        customer.setZip(tenant.getZip());
        customer.setAdditionalInfo(tenant.getAdditionalInfo());
    }
}
