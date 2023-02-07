/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DefaultNewDeviceService.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/6
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.service.entitiy.newtenant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultNewTbTenantService extends AbstractTbEntityService implements NewTbTenantService {

    @Override
    public Tenant saveTenant(Customer customer) throws Exception {
        // 复制记录, 并落库
        Tenant tenant = createTenant(customer);
        return tbTenantService.save(tenant);
    }

    private Tenant createTenant(Customer customer) {
        Tenant tenant = new Tenant();
        updateTenant(tenant, customer);
        return tenant;
    }

    @Override
    public void updateTenant(Tenant tenant, Customer customer) {
        tenant.setTitle(customer.getTitle());
        tenant.setEmail(customer.getEmail());
        tenant.setAddress(customer.getAddress());
        tenant.setAddress2(customer.getAddress2());
        tenant.setCity(customer.getCity());
        tenant.setCountry(customer.getCountry());
        tenant.setState(customer.getState());
        tenant.setPhone(customer.getPhone());
        tenant.setZip(customer.getZip());
        tenant.setAdditionalInfo(customer.getAdditionalInfo());
    }
}
