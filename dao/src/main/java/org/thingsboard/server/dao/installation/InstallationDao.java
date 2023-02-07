/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   ParamDao.java
 * Author: gongjian   ID:     Version:    Date: 2022/11/26
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallationDao extends Dao<Installation>, TenantEntityDao{

    /**
     * Save or update param object
     *
     * @param installation the param object
     * @return saved param object
     */
    Installation save(TenantId tenantId, Installation installation);

    /**
     * Save or update param object
     *
     * @param installation the param object
     * @return saved param object
     */
    Installation saveAndFlush(TenantId tenantId, Installation installation);
    /**
     * Find installations by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of installation objects
     */
    PageData<Installation> findInstallationsByTenantId(UUID tenantId, PageLink pageLink);
    /**
     * Find installations by customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of param objects
     */
    PageData<Installation> findInstallationsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);
    List<Installation> findInstallationsByCustomerId(UUID customerId);
    List<Installation> findInstallationsByTenantIdCustomerId(UUID tenantId, UUID customerId);

    /**
     * Find installation by id, tenantId
     *
     * @param tenantId the tenantId
     * @param id the id
     * @return the param objects
     */
    Installation findInstallationByTenantIdAndId(TenantId tenantId, UUID id);

    /**
     *
     * @param portalId the portalId
     * @return
     */
    Optional<Installation> findInstallationByPortalId(TenantId tenantId, String portalId);
    /**
     * Find devices by tenantId and device id.
     * @param tenantId tenantId the tenantId
     * @param id the deviceId
     * @return the device object
     */
    ListenableFuture<Installation> findInstallationByTenantIdAndIdAsync(TenantId tenantId, UUID id);
}
