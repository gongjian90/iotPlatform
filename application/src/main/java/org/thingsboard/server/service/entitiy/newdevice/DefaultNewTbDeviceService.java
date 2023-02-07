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
package org.thingsboard.server.service.entitiy.newdevice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultNewTbDeviceService extends AbstractTbEntityService implements NewTbDeviceService {

    @Override
    public void assignDeviceToUser(DeviceId deviceId, User user, User currentUser) throws ThingsboardException {
        // 当前用户可能是tenant，可能是customer，邀请的可能是tenant，可能是customer
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = user.getCustomerId();
        // 这里TenantId用的是用户租户还是设备租户？
        // 1、用户租户：CUSTOMER_USER为默认值；TENANT_ADMIN为实际tenantId
        // 2、设备租户：新增加设备的tenantId与设备管理员一致；
        TenantId tenantId = user.getTenantId();
        try {
            // 不能重复共享
            boolean exist = false;
            List<EntityRelation> byTo = relationService.findByTo(tenantId, deviceId, RelationTypeGroup.USER_DEVICE);
            for (EntityRelation entity: byTo) {
                if (Authority.CUSTOMER_USER.equals(user.getAuthority()) && customerId.equals(entity.getFrom())) {
                    exist = true;
                    break;
                } else if (Authority.TENANT_ADMIN.equals(user.getAuthority()) && tenantId.equals(entity.getFrom())) {
                    exist = true;
                    break;
                }
            }
            if (exist) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            // 修改表数据
            if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                newDeviceService.assignDeviceToCustomer(tenantId, deviceId, customerId);
            } else if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
                newDeviceService.assignDeviceToTenant(tenantId, deviceId);
            }
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(currentUser.getTenantId(), deviceId, customerId, null,
                    actionType, user, true, deviceId.toString(), customerId.toString(), user.getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            notificationEntityService.logEntityAction(currentUser.getTenantId(), emptyId(EntityType.DEVICE), actionType, user,
                    e, deviceId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public void unassignDeviceFromUser(Device device, CustomerId customerId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            // 不能无效取消共享
            boolean exist = false;
            List<EntityRelation> byTo = relationService.findByTo(tenantId, deviceId, RelationTypeGroup.USER_DEVICE);
            for (EntityRelation entity: byTo) {
                if (Authority.CUSTOMER_USER.equals(user.getAuthority()) && customerId.equals(entity.getFrom())) {
                    exist = true;
                    break;
                } else if (Authority.TENANT_ADMIN.equals(user.getAuthority()) && tenantId.equals(entity.getFrom())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            // 取消共享，修改表数据
            if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                newDeviceService.unassignDeviceFromCustomer(tenantId, customerId, deviceId);
                notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, customerId, device,
                        actionType, user, true, deviceId.toString(), customerId.toString(), user.getName());
            } else if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
                newDeviceService.unassignDeviceFromTenant(tenantId, deviceId);
                notificationEntityService.notifyAssignOrUnassignEntityToTenant(tenantId, deviceId, device,
                        actionType, user, deviceId.toString(), user.getName());
            }
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public Device save(Device device, Device oldDevice, String accessToken, User user) throws Exception {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));
            // 保存一下用户拥有设备信息关联关系
            createRelationFromDevice(tenantId, user.getCustomerId(), savedDevice.getId());
            autoCommit(user, savedDevice.getId());
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, oldDevice, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, User user) throws ThingsboardException {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials));
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, device, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device,
                    actionType, user, e);
            throw e;
        }
    }
}
