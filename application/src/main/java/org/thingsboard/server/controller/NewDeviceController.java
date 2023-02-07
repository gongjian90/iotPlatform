/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   NewDeviceController.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/3
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.R;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.TbUrlConstants.NEW_URL_PREFIX;

@RestController
@TbCoreComponent
@RequestMapping(NEW_URL_PREFIX)
@RequiredArgsConstructor
@Slf4j
public class NewDeviceController extends BaseController {

    @ApiOperation(value = "Create Or Update Device (saveDevice)",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + UUID_WIKI_LINK +
                    "Device credentials are also generated if not provided in the 'accessToken' request parameter. " +
                    "The newly created device id will be present in the response. " +
                    "Specify existing Device id to update the device. " +
                    "Referencing non-existing device Id will cause 'Not Found' error." +
                    "\n\nDevice name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the device names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public R<Device> saveDevice(@ApiParam(value = "A JSON value representing the device.") @RequestBody Device device,
                                @ApiParam(value = "Optional value of the device credentials to be used during device creation. " +
                                     "If omitted, access token will be auto-generated.") @RequestParam(name = "accessToken", required = false) String accessToken) throws Exception {
        // 回填租户id TENANT_ADMIN：具体的UUID，CUSTOMER_USER：默认值
        device.setTenantId(getCurrentUser().getTenantId());
        // 回填客户id TENANT_ADMIN：默认值，CUSTOMER_USER：具体的UUID
        device.setCustomerId(getCurrentUser().getCustomerId());
        // 回填附加信息，设备管理人员id
        JsonNode additionalInfo = device.getAdditionalInfo();
        additionalInfo = JacksonUtil.addJsonNode(additionalInfo, MAIN_USER_ID, getCurrentUser().getId().toString());
        device.setAdditionalInfo(additionalInfo);
        // 判断设备新增还是修改
        Device oldDevice = null;
        if (device.getId() != null) {
            oldDevice = checkDeviceId(device.getId(), Operation.WRITE);
        } else {
            checkEntity(null, device, Resource.DEVICE);
        }
        // 更新设备
        return R.success(newTbDeviceService.save(device, oldDevice, accessToken, getCurrentUser()));
    }

    @ApiOperation(value = "Delete device (deleteDevice)",
            notes = "Deletes the device, it's credentials and all the relations (from and to the device). " +
                    "Referencing non-existing device Id will cause an error." + CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public R<Integer> deleteDevice(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                             @PathVariable(DEVICE_ID) String strDeviceId) throws Exception {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.DELETE);
        // 设备管理员才有权限删除
        JsonNode additionalInfo = device.getAdditionalInfo();
        if (additionalInfo.get(MAIN_USER_ID) == null) {
            throw new ThingsboardException("main user id is null.",
                    ThingsboardErrorCode.INVALID_ARGUMENTS);
        } else if (!String.valueOf(getCurrentUser().getId()).equals(String.valueOf(additionalInfo.get(MAIN_USER_ID).asText()))) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        // 删除设备源代码中，有删除deviceId在relation表中数据的逻辑，所以不用改造，直接使用tbDeviceService接口
        tbDeviceService.delete(device, getCurrentUser()).get();
        return R.success(1);
    }

    @ApiOperation(value = "Get Customer Device (getUserDevice)",
            notes = "Requested device must be owned by customer that the user belongs to. " +
                    "Device name is an unique property of device. So it can be used to identify the device." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public R<PageData<Device>> getUserDevice(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageData<Device> devices;
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.CUSTOMER_USER.equals(getCurrentUser().getAuthority())) {
                CustomerId customerId = getCurrentUser().getCustomerId();
                devices = newDeviceService.findDevicesByCustomerId(customerId, pageLink);
            } else {
                TenantId tenantId = getCurrentUser().getTenantId();
                devices = deviceService.findDevicesByTenantId(tenantId, pageLink);
            }
            return R.success(devices);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device (getDeviceById)",
            notes = "Fetch the Device object based on the provided Device Id. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the device is assigned to the same customer." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public R<Device> getDeviceById(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return R.success(checkDeviceId(deviceId, Operation.READ));
    }

    @ApiOperation(value = "Get Devices By Ids (getDevicesByIds)",
            notes = "Requested devices must be owned by tenant or assigned to customer which user is performing the request. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    public R<List<Device>> getDevicesByIds(
            @ApiParam(value = "A list of devices ids, separated by comma ','")
            @RequestParam("deviceIds") String[] strDeviceIds) throws ThingsboardException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            List<DeviceId> deviceIds = new ArrayList<>();
            for (String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            ListenableFuture<List<Device>> devices;
            if (customerId == null || customerId.isNullUid()) {
                devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
            } else {
                devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, deviceIds);
            }
            return R.success(checkNotNull(devices.get()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign device to user (assignDeviceToUser)",
            notes = "Creates assignment of the device to user. User will be able to query device afterwards." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public R<Object> assignDeviceToUser(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                         @PathVariable(USER_ID) String strUserId,
                                                      @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(DEVICE_ID) String strDeviceId, HttpServletResponse response) {
        try {
            checkParameter(USER_ID, strUserId);
            if (String.valueOf(getCurrentUser().getId()).equals(strUserId)) { //不能把设备共享给自己
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            checkParameter(DEVICE_ID, strDeviceId);
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.ASSIGN_TO_CUSTOMER);
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
            newTbDeviceService.assignDeviceToUser(deviceId, user, getCurrentUser());
            return R.success(null);
        } catch (ThingsboardException e) {
            handleControllerException(e, response);
        }
        return R.error(null);
    }

    @ApiOperation(value = "Unassign device from user (unassignDeviceFromUser)",
            notes = "Clears assignment of the device to user. User will not be able to query device afterwards." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public R<Object> unassignDeviceFromUser(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                             @PathVariable(USER_ID) String strUserId,
                                         @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                             @PathVariable(DEVICE_ID) String strDeviceId, HttpServletResponse response) throws ThingsboardException {
        try {
            // 不能把自己的设备取消共享
            checkParameter(USER_ID, strUserId);
            if (String.valueOf(getCurrentUser().getId()).equals(strUserId)) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            checkParameter(DEVICE_ID, strDeviceId);
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.UNASSIGN_FROM_CUSTOMER);
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (user.getCustomerId() == null || user.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            newTbDeviceService.unassignDeviceFromUser(device, user.getCustomerId(), getCurrentUser());
            return R.success(null);
        } catch (ThingsboardException | IncorrectParameterException e) {
            handleControllerException(e, response);
        }
        return R.error(null);
    }
}
