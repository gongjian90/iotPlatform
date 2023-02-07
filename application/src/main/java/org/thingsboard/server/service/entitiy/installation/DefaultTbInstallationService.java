/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DefaultTbParamService.java
 * Author: gongjian   ID:     Version:    Date: 2022/11/27
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.service.entitiy.installation;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.*;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.installation.InstallationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION;


@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbInstallationService extends AbstractTbEntityService implements TbInstallationService {

    private final InstallationService installationService;

    @Override
    public Installation save(Installation installation, Installation oldInstallation, User user) throws Exception {
        ActionType actionType = installation.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = installation.getTenantId();
        try {
            Installation savedInstallation = checkNotNull(installationService.saveInstallation(installation));
            if (oldInstallation == null) {
                // 保存一下用户拥有设备信息关联关系
                if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                    installationService.createRelationFromInstallation(tenantId, user.getCustomerId(), savedInstallation.getId());
                } else {
                    installationService.createRelationFromInstallation(tenantId, tenantId, savedInstallation.getId());
                }
            }
            autoCommit(user, savedInstallation.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedInstallation.getId(), savedInstallation,
                    savedInstallation.getCustomerId(), actionType, user);
            return savedInstallation;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.INSTALLATION), installation, actionType, user, e);
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> delete(Installation installation, User user) {
        TenantId tenantId = installation.getTenantId();
        InstallationId installationId = installation.getId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, installationId);
            installationService.deleteInstallation(tenantId, installationId);
            notificationEntityService.notifyDeleteEntity(tenantId, installationId, installation, installation.getCustomerId(),
                    ActionType.DELETED, relatedEdgeIds, user, installationId.toString());
            return removeAlarmsByEntityId(tenantId, installationId);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.INSTALLATION), ActionType.DELETED,
                    user, e, installationId.toString());
            throw e;
        }
    }

    @Override
    public ListDataDeviceVO findDataDevicesByInstallationId(InstallationId installationId) {
        ListDataDeviceVO result = new ListDataDeviceVO();
        List<AbstractDeviceVO> devices = new ArrayList<>();
        List<Object> unconfigured_devices = new ArrayList<>();
        List<Device> deviceList = deviceService.findDevicesByInstallationId(installationId);
        for (Device device: deviceList) {
            if (InstanceEnum.COLOR_CONTROL_GX.getInstance_id().equals(device.getType())) {
                DeviceBatteryVO deviceBatteryVO = new DeviceBatteryVO();
                deviceBatteryVO.setName("Battery Monitor");
                deviceBatteryVO.setCustomName("");
                deviceBatteryVO.setProductCode("");
                deviceBatteryVO.setIdSite(device.getId().toString());
                deviceBatteryVO.setProductName("Generic Can-bus BMS battery");
                deviceBatteryVO.setFirmwareVersion("");
                deviceBatteryVO.setLastConnection(0L);
                deviceBatteryVO.setClass_list(null);
                deviceBatteryVO.setInstance(device.getType());
                deviceBatteryVO.setIdDeviceType("2");
                deviceBatteryVO.setSettings(new ArrayList<>());
                devices.add(deviceBatteryVO);
            } else {
                // TODO add other Instance
            }
        }
        result.setDevices(devices);
        result.setUnconfigured_devices(unconfigured_devices);
        return result;
    }

    @Override
    public List<InstallationInfoVO> getInstallationInfoVOList(List<Installation> installations, User user) {
        List<InstallationInfoVO> result = new ArrayList<>();
        for (Installation installation: installations) {
            InstallationInfoVO installationInfoVO = changeToInstallationInfoVO(installation, user);
            result.add(installationInfoVO);
        }
        return result;
    }

    @Override
    public InstallationInfoVO changeToInstallationInfoVO(Installation installation, User user) {
        InstallationInfoVO installationInfoVO = new InstallationInfoVO();
        installationInfoVO.setIdSite(installation.getId().getId().toString());
        installationInfoVO.setName(installation.getName());
        installationInfoVO.setAccessLevel(1);
        installationInfoVO.setOwner(true);
        installationInfoVO.setIs_admin(true);
        installationInfoVO.setIdentifier(""); // TODO 未知逻辑
        installationInfoVO.setIdUser(user.getId().getId().toString());
        installationInfoVO.setPvMax(0L); // TODO
        installationInfoVO.setTimezone("Europe/Paris"); // TODO 从哪里获取
        installationInfoVO.setPhonenumber("");
        installationInfoVO.setNotes(null);
        installationInfoVO.setGeofence(null);
        installationInfoVO.setGeofenceEnabled(false);
        installationInfoVO.setRealtimeUpdates(true);
        installationInfoVO.setHasMains(0);
        installationInfoVO.setHasGenerator(0);
        installationInfoVO.setNoDataAlarmTimeout(null);
        installationInfoVO.setAlarmMonitoring(1);
        installationInfoVO.setInvalidVRMAuthTokenUsedInLogRequest(0);
        installationInfoVO.setSyscreated(installation.getCreatedTime());
        installationInfoVO.setGrafanaEnabled(0);
        installationInfoVO.setIsPaygo(0);
        installationInfoVO.setPaygoCurrency(null);
        installationInfoVO.setPaygoTotalAmount(null);
        installationInfoVO.setInverterChargerControl(0);
        installationInfoVO.setShared(false);
        installationInfoVO.setDevice_icon("solar"); // TODO 未知逻辑
        installationInfoVO.setAlarm(false);
        installationInfoVO.setLast_timestamp(0L); // TODO 最后更新时间 应该去ts_kv
        List<TagVO> tags = new ArrayList<>();
        TagVO tagVO = new TagVO();
        tagVO.setIdTag("8788");
        tagVO.setName("turbo-energy");
        tagVO.setAutomatic(false);
        installationInfoVO.setTags(tags);   // 查询tag
        installationInfoVO.setCurrent_time("09:43");  // TODO 获取当前时分
        installationInfoVO.setTimezone_offset(28800L); // TODO 获取时区偏移
        installationInfoVO.setImages(false);
        ViewPermission viewPermission = new ViewPermission();
        viewPermission.setUpdate_settings(true);
        viewPermission.setSettings(true);
        viewPermission.setDiagnostics(false);
        viewPermission.setShare(true);
        viewPermission.setVnc(true);
        viewPermission.setMqtt_rpc(true);
        viewPermission.setVebus(true);
        viewPermission.setTwoway(true);
        viewPermission.setExact_location(true);
        viewPermission.setNodered(false);
        viewPermission.setNodered_dash(false);
        viewPermission.setSignalk(false);
        viewPermission.setPaygo(false);
        installationInfoVO.setView_permissions(viewPermission);
        List<InstallationAttributeVO> list = new ArrayList<>();
        InstallationAttributeVO voltageAttributeVO = new InstallationAttributeVO();
        voltageAttributeVO.setIdDataAttribute(InstallationEnum.VOLTAGE.getIdDataAttribute());
        voltageAttributeVO.setCode(InstallationEnum.VOLTAGE.getCode());
        voltageAttributeVO.setDescription(InstallationEnum.VOLTAGE.getDescription());
        voltageAttributeVO.setFormatWithUnit(InstallationEnum.VOLTAGE.getFormatWithUnit());
        voltageAttributeVO.setDataType(InstallationEnum.VOLTAGE.getDataType());
        voltageAttributeVO.setIdDeviceType(InstallationEnum.VOLTAGE.getIdDeviceType());
        voltageAttributeVO.setTextValue(InstallationEnum.VOLTAGE.getTextValue());
        voltageAttributeVO.setInstance(InstallationEnum.VOLTAGE.getInstance());
        voltageAttributeVO.setTimestamp((System.currentTimeMillis() / 1000) + "");
        voltageAttributeVO.setDbusServiceType(InstallationEnum.VOLTAGE.getDbusServiceType());
        voltageAttributeVO.setDbusPath(InstallationEnum.VOLTAGE.getDbusPath());
        voltageAttributeVO.setRawValue("4.3");
        voltageAttributeVO.setFormattedValue("4.3 V");
        list.add(voltageAttributeVO);

        InstallationAttributeVO statusAttributeVO = new InstallationAttributeVO();
        statusAttributeVO.setIdDataAttribute(InstallationEnum.BATTERY_STATE.getIdDataAttribute());
        statusAttributeVO.setCode(InstallationEnum.BATTERY_STATE.getCode());
        statusAttributeVO.setDescription(InstallationEnum.BATTERY_STATE.getDescription());
        statusAttributeVO.setFormatWithUnit(InstallationEnum.BATTERY_STATE.getFormatWithUnit());
        statusAttributeVO.setDataType(InstallationEnum.BATTERY_STATE.getDataType());
        statusAttributeVO.setIdDeviceType(InstallationEnum.BATTERY_STATE.getIdDeviceType());
        statusAttributeVO.setTextValue(InstallationEnum.BATTERY_STATE.getTextValue());
        statusAttributeVO.setInstance(InstallationEnum.BATTERY_STATE.getInstance());
        statusAttributeVO.setTimestamp((System.currentTimeMillis() / 1000) + "");
        statusAttributeVO.setDbusServiceType(InstallationEnum.BATTERY_STATE.getDbusServiceType());
        statusAttributeVO.setDbusPath(InstallationEnum.BATTERY_STATE.getDbusPath());
        statusAttributeVO.setRawValue("0");
        statusAttributeVO.setFormattedValue("Idle");
        statusAttributeVO.setDataAttributeEnumValues(Arrays.asList(InstallationEnum.BATTERY_STATE.getDataAttributeEnumValues()));
        list.add(statusAttributeVO);
        installationInfoVO.setExtended(list);
        return installationInfoVO;
    }

    @Override
    public void assignInstallationToUser(InstallationId installationId, User user, User currentUser) throws ThingsboardException {
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
            List<EntityRelation> byTo = relationService.findByTo(tenantId, installationId, RelationTypeGroup.USER_INSTALLATION);
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
                // 保存relation
                installationService.assignInstallationToCustomer(tenantId, installationId, customerId);
            } else if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
                // 更新Installation和relation
                installationService.assignInstallationToTenant(tenantId, installationId);
            }
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(currentUser.getTenantId(), installationId, customerId, null,
                    actionType, user, true, installationId.toString(), customerId.toString(), user.getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            notificationEntityService.logEntityAction(currentUser.getTenantId(), emptyId(EntityType.INSTALLATION), actionType, user,
                    e, installationId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public void unassignInstallationFromUser(Installation installation, CustomerId customerId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = installation.getTenantId();
        InstallationId installationId = installation.getId();
        try {
            // 不能无效取消共享
            boolean exist = false;
            List<EntityRelation> byTo = relationService.findByTo(tenantId, installationId, RelationTypeGroup.USER_INSTALLATION);
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
                installationService.unassignInstallationFromCustomer(tenantId, customerId, installationId);
                notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, installationId, customerId, installation,
                        actionType, user, true, installationId.toString(), customerId.toString(), user.getName());
            } else if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
                installationService.unassignInstallationFromTenant(tenantId, installationId);
                notificationEntityService.notifyAssignOrUnassignEntityToTenant(tenantId, installationId, installation,
                        actionType, user, installationId.toString(), user.getName());
            }
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, installationId.toString());
            throw e;
        }
    }
}
