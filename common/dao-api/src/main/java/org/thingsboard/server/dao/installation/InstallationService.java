/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   InstallationService.java
 * Author: gongjian   ID:     Version:    Date: 2022/11/25
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.dao.installation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Installation;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface InstallationService {

    Installation saveInstallation(Installation installation);
    void deleteInstallation(TenantId tenantId, InstallationId installationId);
    void deleteInstallationsByTenantId(TenantId tenantId);
    void deleteInstallationsByCustomerId(CustomerId customerId);

    PageData<Installation> findInstallations(TenantId tenantId, CustomerId customerId, PageLink pageLink);
    /**
     * find the Installation of tenant or customer
     *
     * usages at BaseController.java checkInstallationId() method
     *
     * @param tenantId tenantId
     * @param installationId installationId
     * @return Installation
     */
    Installation findInstallationById(TenantId tenantId, InstallationId installationId);
    Installation findInstallationByPortalId(TenantId tenantId, String portalId);
    ListenableFuture<Installation> findInstallationByIdAsync(TenantId tenantId, InstallationId installationId);
    List<Installation> findInstallationsByTenantIdCustomerId(TenantId tenantId, CustomerId customerId);
    void assignInstallationToCustomer(TenantId tenantId, InstallationId installationId, CustomerId customerId);
    void assignInstallationToTenant(TenantId tenantId, InstallationId installationId) throws ThingsboardException;
    void unassignInstallationFromCustomer(TenantId tenantId, CustomerId customerId, InstallationId installationId);
    void unassignInstallationFromTenant(TenantId tenantId, InstallationId installationId) throws ThingsboardException;
    void createRelationFromInstallation(TenantId tenantId, EntityId entityIdFrom, EntityId entityIdTo);
}
