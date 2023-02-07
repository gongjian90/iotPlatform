/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   NewCustomerController.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/6
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.R;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.TbUrlConstants.NEW_URL_PREFIX;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping(NEW_URL_PREFIX)
public class NewTenantController extends BaseController {

    @ApiOperation(value = "Create Or update Tenant (saveTenantAndUpdateTenantId)",
            notes = "Create or update the Tenant. When creating tenant, platform generates Tenant Id as " + UUID_WIKI_LINK +
                    "Default Rule Chain and Device profile are also generated for the new tenants automatically. " +
                    "The newly created Tenant Id will be present in the response. " +
                    "Specify existing Tenant Id id to update the Tenant. " +
                    "Referencing non-existing Tenant Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Tenant entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{userId}", method = RequestMethod.POST, name = "升级用户")
    @ResponseBody
    @Transactional
    public R<Tenant> saveTenantAndUpdateTenantId(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                              @PathVariable(USER_ID) String strUserId) throws Exception {
        // 根据userId查询User
        checkParameter(USER_ID, strUserId); // validateId(userId, "Incorrect userId " + userId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.READ);
        if (!Authority.CUSTOMER_USER.equals(user.getAuthority())) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        // 根据customerId查询customer
        Customer customer = checkCustomerId(user.getCustomerId(), Operation.READ);
        // 查询租户表，是否存在对应记录
        Tenant tenant = checkTenantId(user.getEmail());
        Tenant saveTenant;
        if (tenant == null) {
            // 保存租户信息
            saveTenant = newTbTenantService.saveTenant(customer);
        } else {
            saveTenant = tenant;
        }

        // 更新TenantId ------------------------------------------------------------------------------------------------
        User saveUser = newTbUserService.turnOnAuthorityForUser(user, saveTenant.getTenantId());
        // ☆ 这里需要特别强调一下，因为客户和租户的同级关系，这里不更新客户信息
        // Customer savedCustomer = newCustomerService.savedCustomer(saveTenant.getTenantId(), saveUser, customer);
        List<Installation> installations = checkNotNull(installationService.findInstallationsByTenantIdCustomerId(user.getTenantId(), user.getCustomerId()));
        for (Installation installation: installations) {
            installation.setTenantId(saveTenant.getTenantId());
            installation.setCustomerId(CustomerId.SYS_CUSTOMER_ID);
            installationService.saveInstallation(installation);
        }
        // 查询用户设备列表（只查自己是管理员的设备）,更新customerId和tenantId
        List<Device> devices = checkNotNull(deviceService.findDevicesByTenantIdCustomerId(TenantId.SYS_TENANT_ID, customer.getId()));
        for (Device device: devices) {
            // 更新device_profile
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(TenantId.SYS_TENANT_ID, device.getDeviceProfileId());
            if (deviceProfile == null) {
                throw new ThingsboardException(INCORRECT_DEVICE_PROFILE_ID, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            deviceProfile.setTenantId(saveTenant.getTenantId());
            deviceProfileService.saveDeviceProfile(deviceProfile);

            device.setCustomerId(CustomerId.SYS_CUSTOMER_ID);
            device.setTenantId(saveTenant.getTenantId());
            deviceService.saveDevice(device);
        }
        return R.success(saveTenant);
    }


    @ApiOperation(value = "Create Or update Tenant (deleteTenantAndUpdateTenantId)",
            notes = "Create or update the Tenant. When creating tenant, platform generates Tenant Id as " + UUID_WIKI_LINK +
                    "Default Rule Chain and Device profile are also generated for the new tenants automatically. " +
                    "The newly created Tenant Id will be present in the response. " +
                    "Specify existing Tenant Id id to update the Tenant. " +
                    "Referencing non-existing Tenant Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Tenant entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{userId}", method = RequestMethod.DELETE, name = "用户降级")
    @ResponseBody
    @Transactional
    public R<Object> deleteTenantAndUpdateTenantId(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                                 @PathVariable(USER_ID) String strUserId) throws Exception {
        // STEP-1 查询tb_user表获取tenantId信息
        // STEP-2 查询tenant表对应信息，关联表也要删除
        // STEP-3 更新各个表中的tenantId，更新为SYSTEM_ID
        // 根据userId查询User
        checkParameter(USER_ID, strUserId); // validateId(userId, "Incorrect userId " + userId);
        UserId userId = new UserId(toUUID(strUserId));
        // 查询传入用户信息
        User user = checkUserId(userId, Operation.READ);
        if (Authority.CUSTOMER_USER.equals(user.getAuthority())) { // 查询用户的权限不满足降级
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (TenantId.SYS_TENANT_ID.equals(user.getTenantId())) { // 不是正常的租户，无法降级
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        // 不能根据user.customerId查询customer,需要根据email查询
        Customer customer = checkCustomerId(user.getEmail());
        // 查询租户
        Tenant tenant = checkTenantId(user.getTenantId(), Operation.DELETE);
        // 删除租户信息，但删租户会把设备也都删了 。。。只更新tenantId
        // newTbTenantService.deleteTenant(tenant);

        // 更新TenantId ------------------------------------------------------------------------------------------------
        User saveUser = newTbUserService.turnOffAuthorityForUser(user, customer.getId());
        // ☆ 这里需要特别强调一下，因为客户和租户的同级关系，这里不更新客户信息
        // Customer savedCustomer = newCustomerService.savedCustomer(TenantId.SYS_TENANT_ID, saveUser, customer);
        List<Installation> installations = checkNotNull(installationService.findInstallationsByTenantIdCustomerId(user.getTenantId(), user.getCustomerId()));
        for (Installation installation: installations) {
            installation.setTenantId(TenantId.SYS_TENANT_ID);
            installation.setCustomerId(customer.getId());
            installationService.saveInstallation(installation);
        }
        // 查询用户设备列表（只查自己是管理员的设备）,更新customerId和tenantId
        List<Device> devices = checkNotNull(deviceService.findDevicesByTenantIdCustomerId(tenant.getTenantId(), CustomerId.SYS_CUSTOMER_ID));
        for (Device device: devices) {
            // 更新device_profile
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenant.getTenantId(), device.getDeviceProfileId());
            if (deviceProfile == null) {
                throw new ThingsboardException(INCORRECT_DEVICE_PROFILE_ID, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            deviceProfile.setTenantId(TenantId.SYS_TENANT_ID);
            deviceProfileService.saveDeviceProfile(deviceProfile);

            device.setCustomerId(customer.getId());
            device.setTenantId(TenantId.SYS_TENANT_ID);
            deviceService.saveDevice(device);
        }
        return R.success(devices.size());
    }
}
