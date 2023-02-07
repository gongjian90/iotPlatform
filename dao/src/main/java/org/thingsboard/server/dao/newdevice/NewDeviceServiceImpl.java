/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DefaultNewDeviceService.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/3
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.dao.newdevice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.newdevice.NewDeviceCacheEvictEvent;
import org.thingsboard.server.cache.newdevice.NewDeviceCacheKey;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.*;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class NewDeviceServiceImpl extends AbstractCachedEntityService<NewDeviceCacheKey, Device, NewDeviceCacheEvictEvent> implements NewDeviceService {

    @Autowired
    private DeviceDao deviceDao;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceProfileService deviceProfileService;


    @Override
    public PageData<Device> findDevicesByCustomerId(CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDeviceByCustomerId [{}][{}]", customerId, pageLink);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByCustomerId(customerId.getId(), pageLink);
    }

    @Transactional
    @Override
    public void assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId) {
        // 保存设备关联关系
        createRelationFromDevice(tenantId, customerId, deviceId);
    }
    private void createRelationFromDevice(TenantId tenantId, EntityId entityIdFrom, EntityId entityIdTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(entityIdFrom);
        relation.setTo(entityIdTo);
        relation.setTypeGroup(RelationTypeGroup.USER_DEVICE);
        relation.setType(EntityRelation.DEVICE_GUEST_TYPE);
        relationService.saveRelation(tenantId, relation);
    }
    @Transactional
    @Override
    public void unassignDeviceFromCustomer(TenantId tenantId, CustomerId customerId, DeviceId deviceId) {
        relationService.deleteRelation(tenantId, customerId, deviceId, EntityRelation.DEVICE_GUEST_TYPE, RelationTypeGroup.USER_DEVICE);
    }

    @Transactional
    @Override
    public void assignDeviceToTenant(TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        // NOTICE：由于使用通用方法，所以这里传TenantId.SYS_TENANT_ID
        Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
        if (device == null) {
            throw new ThingsboardException(INCORRECT_DEVICE_ID, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        // 更新deviceProfile表
        assignDeviceProfileToTenant(tenantId, device.getDeviceProfileId());
        // 更新device表
        if (TenantId.SYS_TENANT_ID.equals(device.getTenantId())) {
            device.setTenantId(tenantId);
        }
        deviceService.saveDevice(device);
        // 保存设备关联关系
        createRelationFromDevice(tenantId, tenantId, deviceId);
    }

    @Transactional
    @Override
    public void unassignDeviceFromTenant(TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        // 更新设备信息表，有查询不到的情况
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device != null) {
            device.setTenantId(TenantId.SYS_TENANT_ID); //TODO
            // 更新设备配置信息表
            unassignDeviceProfileFromTenant(tenantId, device.getDeviceProfileId());
        }
        // 删除对应设备关联信息
        relationService.deleteRelation(tenantId, tenantId, deviceId, EntityRelation.DEVICE_GUEST_TYPE, RelationTypeGroup.USER_DEVICE);
    }
    @Transactional
    public DeviceProfile assignDeviceProfileToTenant(TenantId tenantId, DeviceProfileId deviceProfileId) throws ThingsboardException {
        // 更新device_profile
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
        if (deviceProfile == null) {
            throw new ThingsboardException(INCORRECT_DEVICE_PROFILE_ID, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        deviceProfile.setTenantId(tenantId);
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Transactional
    public DeviceProfile unassignDeviceProfileFromTenant(TenantId tenantId, DeviceProfileId deviceProfileId) throws ThingsboardException {
        // 更新device_profile
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
        if (deviceProfile == null) {
            throw new ThingsboardException(INCORRECT_DEVICE_PROFILE_ID, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        deviceProfile.setTenantId(TenantId.SYS_TENANT_ID);
        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }
    @TransactionalEventListener(classes = NewDeviceCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(NewDeviceCacheEvictEvent event) {
        List<NewDeviceCacheKey> keys = new ArrayList<>(3);
        keys.add(new NewDeviceCacheKey(event.getCustomerId(), event.getNewName()));
        if (event.getDeviceId() != null) {
            keys.add(new NewDeviceCacheKey(event.getCustomerId(), event.getDeviceId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new NewDeviceCacheKey(event.getCustomerId(), event.getOldName()));
        }
        cache.evict(keys);
    }
}
