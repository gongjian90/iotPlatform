/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   JpaParamDao.java
 * Author: gongjian   ID:     Version:    Date: 2022/11/26
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.dao.sql.installaion;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Installation;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.InstallationEntity;
import org.thingsboard.server.dao.installation.InstallationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class JpaInstallationDao extends JpaAbstractDao<InstallationEntity, Installation> implements InstallationDao {

    @Autowired
    private InstallationRepository installationRepository;

    @Override
    public Installation saveAndFlush(TenantId tenantId, Installation installation) {
        Installation result = this.save(tenantId, installation);
        installationRepository.flush();
        return result;
    }

    @Override
    public PageData<Installation> findInstallationsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                installationRepository.findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Installation> findInstallationsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                installationRepository.findByTenantIdAndCustomerId(
                        tenantId, customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<Installation> findInstallationsByCustomerId(UUID customerId) {
        return DaoUtil.convertDataList(installationRepository.findByCustomerId(customerId));
    }
    @Override
    public List<Installation> findInstallationsByTenantIdCustomerId(UUID tenantId, UUID customerId) {
        return DaoUtil.convertDataList(installationRepository.findInstallationEntitiesByTenantIdAndCustomerId(tenantId, customerId));
    }

    @Override
    public EntityType getEntityType() {
        return super.getEntityType();
    }

    @Override
    public Installation findInstallationByTenantIdAndId(TenantId tenantId, UUID id) {
        return DaoUtil.getData(installationRepository.findByTenantIdAndId(tenantId.getId() ,id));
    }
    @Override
    public Optional<Installation> findInstallationByPortalId(TenantId tenantId, String portalId) {
        return Optional.ofNullable(DaoUtil.getData(installationRepository.findByPortalId(portalId)));
    }

    @Override
    protected Class<InstallationEntity> getEntityClass() {
        return InstallationEntity.class;
    }

    @Override
    protected JpaRepository<InstallationEntity, UUID> getRepository() {
        return installationRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return null;
    }

    @Override
    public ListenableFuture<Installation> findInstallationByTenantIdAndIdAsync(TenantId tenantId, UUID id) {
        return service.submit(() -> DaoUtil.getData(installationRepository.findByTenantIdAndId(tenantId.getId(), id)));
    }
}
