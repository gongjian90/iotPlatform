/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   InstallationServiceImpl.java
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.installation.InstallationCacheEvictEvent;
import org.thingsboard.server.cache.installation.InstallationCacheKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Installation;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.*;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;


@Slf4j
@Service
public class InstallationServiceImpl extends AbstractCachedEntityService<InstallationCacheKey, Installation, InstallationCacheEvictEvent> implements InstallationService {

    @Autowired
    private InstallationDao installationDao;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Override
    public Installation saveInstallation(Installation installation) {
        return installationDao.saveAndFlush(installation.getTenantId(), installation);
    }
    @Transactional
    @Override
    public void deleteInstallation(final TenantId tenantId, final InstallationId installationId) {
        log.trace("Executing deleteDevice [{}]", installationId);
        validateId(installationId, INCORRECT_INSTALLATION_ID + installationId);

        Installation installation = installationDao.findById(tenantId, installationId.getId());
        InstallationCacheEvictEvent installationCacheEvictEvent
                = new InstallationCacheEvictEvent(installation.getTenantId(), installation.getCustomerId(), installationId, installation.getPortalId());
        // entity_view
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(installation.getTenantId(), installationId);
        if (entityViews != null && !entityViews.isEmpty()) {
            throw new DataValidationException("Can't delete installation that has entity views!");
        }
        // device_credential
        List<Device> deviceList = deviceService.findDevicesByInstallationId(installationId);
        for (Device device: deviceList) {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
            if (deviceCredentials != null) {
                deviceCredentialsService.deleteDeviceCredentials(tenantId, deviceCredentials);
            }
            deleteEntityRelations(tenantId, device.getId());
        }
        // device
        deleteEntityRelations(tenantId, installationId);
        deviceService.deleteDevicesByInstallationId(tenantId, installationId);
        // installation
        installationDao.removeById(tenantId, installationId.getId());
        publishEvictEvent(installationCacheEvictEvent);
    }
    @Transactional
    @Override
    public void deleteInstallationsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteInstallationsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantInstallationsRemover.removeEntities(tenantId, tenantId);
    }
    @Transactional
    @Override
    public void deleteInstallationsByCustomerId(CustomerId customerId) {
        log.trace("Executing deleteInstallationsByTenantId, customerId [{}]", customerId);
        validateId(customerId, INCORRECT_TENANT_ID + customerId);
        customerInstallationsRemover.removeEntities(TenantId.SYS_TENANT_ID, customerId);
    }

    @Override
    public PageData<Installation> findInstallations(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findInstallations, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validatePageLink(pageLink);
        return installationDao.findInstallationsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }
    @Override
    public Installation findInstallationById(TenantId tenantId, InstallationId installationId) {
        log.trace("Executing findInstallationById [{}]", installationId);
        validateId(installationId, INCORRECT_INSTALLATION_ID + installationId);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return installationDao.findById(tenantId, installationId.getId());
        } else {
            return installationDao.findInstallationByTenantIdAndId(tenantId, installationId.getId());
        }
    }
    @Override
    public Installation findInstallationByPortalId(TenantId tenantId, String portalId) {
        log.trace("Executing findInstallationByPortalId [{}][{}]", tenantId, portalId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        /*return cache.getAndPutInTransaction(new InstallationCacheKey(tenantId, portalId),
                () -> installationDao.findInstallationByPortalId(tenantId, portalId).orElse(null), true);*/
        return installationDao.findInstallationByPortalId(tenantId, portalId).orElse(null);
    }
    @Override
    public ListenableFuture<Installation> findInstallationByIdAsync(TenantId tenantId, InstallationId installationId) {
        log.trace("Executing findInstallationByIdAsync [{}]", installationId);
        validateId(installationId, INCORRECT_INSTALLATION_ID + installationId);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return installationDao.findByIdAsync(tenantId, installationId.getId());
        } else {
            return installationDao.findInstallationByTenantIdAndIdAsync(tenantId, installationId.getId());
        }
    }
    @Override
    public List<Installation> findInstallationsByTenantIdCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findInstallationsByTenantIdCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return installationDao.findInstallationsByCustomerId(customerId.getId());
        } else {
            return installationDao.findInstallationsByTenantIdCustomerId(tenantId.getId(), customerId.getId());
        }
    }
    @Transactional
    @Override
    public void assignInstallationToCustomer(TenantId tenantId, InstallationId installationId, CustomerId customerId) {
        // 保存设备关联关系
        createRelationFromInstallation(tenantId, customerId, installationId);
    }
    @Transactional
    @Override
    public void assignInstallationToTenant(TenantId tenantId, InstallationId installationId) throws ThingsboardException {
        // NOTICE：由于使用通用方法，所以这里传TenantId.SYS_TENANT_ID
        Installation installation = installationDao.findInstallationByTenantIdAndId(TenantId.SYS_TENANT_ID, installationId.getId());
        if (installation == null) {
            throw new ThingsboardException(INCORRECT_INSTALLATION_ID, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        // 更新Installation表
        if (TenantId.SYS_TENANT_ID.equals(installation.getTenantId())) {
            installation.setTenantId(tenantId);
        }
        installationDao.save(tenantId, installation);

        // 保存设备关联关系
        createRelationFromInstallation(tenantId, tenantId, installationId);
    }
    @Transactional
    @Override
    public void unassignInstallationFromCustomer(TenantId tenantId, CustomerId customerId, InstallationId installationId) {
        relationService.deleteRelation(tenantId, customerId, installationId, EntityRelation.INSTALLATION_GUEST_TYPE, RelationTypeGroup.USER_INSTALLATION);
    }
    @Transactional
    @Override
    public void unassignInstallationFromTenant(TenantId tenantId, InstallationId installationId) throws ThingsboardException {
        relationService.deleteRelation(tenantId, tenantId, installationId, EntityRelation.INSTALLATION_GUEST_TYPE, RelationTypeGroup.USER_INSTALLATION);
    }

    @Override
    public void createRelationFromInstallation(TenantId tenantId, EntityId entityIdFrom, EntityId entityIdTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(entityIdFrom);
        relation.setTo(entityIdTo);
        relation.setTypeGroup(RelationTypeGroup.USER_INSTALLATION);
        relation.setType(EntityRelation.INSTALLATION_GUEST_TYPE);
        relationService.saveRelation(tenantId, relation);
    }
    @TransactionalEventListener(classes = InstallationCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(InstallationCacheEvictEvent event) {
        List<InstallationCacheKey> keys = new ArrayList<>(2);
        if (event.getInstallationId() != null) {
            keys.add(new InstallationCacheKey(event.getTenantId(), event.getCustomerId(), event.getInstallationId()));
        }
        if (StringUtils.isNotBlank(event.getPortalId())) {
            keys.add(new InstallationCacheKey(event.getTenantId(), event.getCustomerId(), event.getPortalId()));
        }
        cache.evict(keys);
    }

    private PaginatedRemover<TenantId, Installation> tenantInstallationsRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Installation> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return installationDao.findInstallationsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Installation entity) {
                    deleteInstallation(tenantId, new InstallationId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Installation> customerInstallationsRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Installation> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
                    return installationDao.findInstallationsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Installation entity) {
                    deleteInstallation(tenantId, new InstallationId(entity.getUuidId()));
                }
            };
}
