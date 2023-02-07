/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   ParamController.java
 * Author: gongjian   ID:     Version:    Date: 2022/11/26
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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Installation;
import org.thingsboard.server.common.data.InstallationTypeEnum;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.InstallationInfoVO;
import org.thingsboard.server.common.data.http.InstallationRequestVO;
import org.thingsboard.server.common.data.http.ListDataDeviceVO;
import org.thingsboard.server.common.data.http.R;
import org.thingsboard.server.common.data.id.InstallationId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.installation.TbInstallationService;
import org.thingsboard.server.service.security.permission.Operation;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InstallationController extends BaseController {
    @javax.annotation.Resource
    private final TbInstallationService tbInstallationService;

    @ApiOperation(value = "Get installations (getInstallations)",
            notes = "Returns a page of installations owned by user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installations", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public R<PageData<Installation>> getInstallations(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INSTALLATION_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = INSTALLATION_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return R.success(checkNotNull(installationService.findInstallations(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), pageLink)));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get installations (getInstallations)",
            notes = "Returns a page of installations owned by user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/all-installations", method = RequestMethod.GET)
    @ResponseBody
    public R<List<InstallationInfoVO>> getInstallations() throws ThingsboardException {
        try {
            List<Installation> installations = checkNotNull(
                    installationService.findInstallationsByTenantIdCustomerId(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId()));
            return R.success(tbInstallationService.getInstallationInfoVOList(installations, getCurrentUser()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Create Or Update Installation (saveInstallation)",
            notes = "Create or update the Installation. When creating Installation, platform generates Installation Id as " + UUID_WIKI_LINK +
                    "Installation credentials are also generated if not provided in the 'accessToken' request parameter. " +
                    "The newly created Installation id will be present in the response. " +
                    "Specify existing Installation id to update the Installation. " +
                    "Referencing non-existing Installation Id will cause 'Not Found' error." +
                    "\n\nInstallation name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the Installation names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'deviceId' from the request body example (below) to create new Installation entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installation", method = RequestMethod.POST)
    @ResponseBody
    public R<InstallationInfoVO> saveInstallation(@ApiParam(value = "A JSON value representing the Installation.") @RequestBody InstallationRequestVO reqVO) throws Exception {
        checkParameter("Portal Id", reqVO.getInstallationId());
        Installation installation1 = installationService.findInstallationByPortalId(getCurrentUser().getTenantId(), reqVO.getInstallationId());
        if (installation1 != null) {
            return R.error("Portal Id [" + reqVO.getInstallationId() + "] is exist!");
        }
        Installation installation = new Installation();
        installation.setTenantId(getCurrentUser().getTenantId());
        installation.setCustomerId(getCurrentUser().getCustomerId());
        installation.setPortalId(reqVO.getInstallationId());
        installation.setName(reqVO.getInstallationName());
        installation.setGsmNumber(reqVO.getGsmNumber());
        installation.setType(InstallationTypeEnum.COLOR_CONTROL_GX);// DEFAULT
        ObjectMapper mapper = new ObjectMapper();
        JsonNode additionalInfo = mapper.readTree("{\"type\":\"DEFAULT\"}");
        // 回填附加信息，管理人员id
        additionalInfo = JacksonUtil.addJsonNode(additionalInfo, MAIN_USER_ID, getCurrentUser().getId().toString());
        installation.setAdditionalInfo(additionalInfo);
        installation.setDescription(reqVO.getInstallationName());
        String str = "{\"configuration\":\"DEFAULT\"}";
        installation.setInstallationDataBytes(str.getBytes(StandardCharsets.UTF_8));
        Installation saveInstallation = checkNotNull(tbInstallationService.save(installation, null, getCurrentUser()));
        return R.success(checkNotNull(tbInstallationService.changeToInstallationInfoVO(saveInstallation, getCurrentUser())));
    }

    @ApiOperation(value = "Update Installation (updateInstallation)",
            notes = "Update the Installation. When creating Installation, platform generates Installation Id as " + UUID_WIKI_LINK +
                    "The newly created Installation id will be present in the response. " +
                    "Specify existing Installation id to update the Installation. " +
                    "Referencing non-existing Installation Id will cause 'Not Found' error." +
                    "\n\nInstallation name is unique in the scope of tenant. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installation/{idSite}", method = RequestMethod.POST)
    @ResponseBody
    public R<Installation> updateInstallation(
            @ApiParam(value = INSTALLATION_ID_PARAM_DESCRIPTION, required = true) @PathVariable(INSTALLATION_ID_ALIAS) String idSite,
            @ApiParam(value = "A JSON value representing the Installation.") @RequestBody InstallationRequestVO reqVO) throws Exception {
        checkParameter(INSTALLATION_ID_ALIAS, idSite);
        Installation installation = new Installation();
        Installation oldInstallation = checkInstallationId(new InstallationId(toUUID(idSite)), Operation.WRITE);
        installation.updateInstallation(oldInstallation);
        if (StringUtils.isNotBlank(reqVO.getInstallationId())) {
            installation.setPortalId(reqVO.getInstallationId());
        }
        if (StringUtils.isNotBlank(reqVO.getInstallationName())) {
            installation.setName(reqVO.getInstallationName());
        }
        if (StringUtils.isNotBlank(reqVO.getGsmNumber())) {
            installation.setGsmNumber(reqVO.getGsmNumber());
        }
        return R.success(tbInstallationService.save(installation, oldInstallation, getCurrentUser()));
    }
    @ApiOperation(value = "Delete Installation (deleteInstallation)", notes = TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installation/{idSite}", method = RequestMethod.DELETE)
    @ResponseBody
    public R<Integer> deleteInstallation(
            @ApiParam(value = INSTALLATION_ID_PARAM_DESCRIPTION, required = true) @PathVariable(INSTALLATION_ID_ALIAS) String idSite) throws Exception {
        checkParameter(INSTALLATION_ID_ALIAS, idSite);
        Installation installation = checkInstallationId(new InstallationId(toUUID(idSite)), Operation.DELETE);
        // 设备管理员才有权限删除
        JsonNode additionalInfo = installation.getAdditionalInfo();
        if (additionalInfo.get(MAIN_USER_ID) == null) {
            throw new ThingsboardException("main user id is null.", ThingsboardErrorCode.INVALID_ARGUMENTS);
        } else if (!String.valueOf(getCurrentUser().getId()).equals(String.valueOf(additionalInfo.get(MAIN_USER_ID).asText()))) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        tbInstallationService.delete(installation, getCurrentUser());
        return R.success(1);
    }

    @ApiOperation(value = "Get Installation (getInstallationById)",
            notes = "Fetch the Installation object based on the provided Installation Id. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installation/{idSite}", method = RequestMethod.GET)
    @ResponseBody
    public R<Installation> getInstallationById(@ApiParam(value = INSTALLATION_ID_PARAM_DESCRIPTION)
                                   @PathVariable(INSTALLATION_ID_ALIAS) String idSite) throws ThingsboardException {
        checkParameter(INSTALLATION_ID_ALIAS, idSite);
        InstallationId installationId = new InstallationId(toUUID(idSite));
        return R.success(checkInstallationId(installationId, Operation.READ));
    }


    @ApiOperation(value = "Get device list by Installation (getDevicesByInstallationId)",
            notes = "Fetch the Installation object based on the provided Installation Id. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/installation/{idSite}/system-overview", method = RequestMethod.GET)
    @ResponseBody
    public R<ListDataDeviceVO> getDevicesByInstallationId(@ApiParam(value = INSTALLATION_ID_PARAM_DESCRIPTION)
                                               @PathVariable(INSTALLATION_ID_ALIAS) String idSite) throws ThingsboardException {
        checkParameter(INSTALLATION_ID_ALIAS, idSite);
        // 根据installationId查询设备列表
        return R.success(tbInstallationService.findDataDevicesByInstallationId(new InstallationId(toUUID(idSite))));
    }

    @ApiOperation(value = "Assign device to user (assignInstallationToUser)",
            notes = "Creates assignment of the device to user. User will be able to query device afterwards." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/installation/{idSite}", method = RequestMethod.POST, name = "分享设备")
    @ResponseBody
    public R<Object> assignInstallationToUser(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                        @PathVariable(USER_ID) String strUserId,
                                        @ApiParam(value = INSTALLATION_ID_PARAM_DESCRIPTION)
                                        @PathVariable(INSTALLATION_ID_ALIAS) String idSite,
                                              HttpServletResponse response) {
        try {
            checkParameter(USER_ID, strUserId);
            if (String.valueOf(getCurrentUser().getId()).equals(strUserId)) { //不能把设备共享给自己
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            checkParameter(INSTALLATION_ID_ALIAS, idSite);
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.ASSIGN_TO_CUSTOMER);
            InstallationId installationId = new InstallationId(toUUID(idSite));
            checkInstallationId(installationId, Operation.ASSIGN_TO_CUSTOMER);
            tbInstallationService.assignInstallationToUser(installationId, user, getCurrentUser());
            return R.success(null);
        } catch (ThingsboardException e) {
            handleControllerException(e, response);
        }
        return R.error(null);
    }

    @ApiOperation(value = "Unassign installation from user (unassignInstallationFromUser)",
            notes = "Clears assignment of the installation to user. User will not be able to query installation afterwards." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}/installation/{idSite}", method = RequestMethod.DELETE, name = "取消分享设备")
    @ResponseBody
    public R<Object> unassignInstallationFromUser(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                              @PathVariable(USER_ID) String strUserId,
                                              @ApiParam(value = INSTALLATION_ID_PARAM_DESCRIPTION)
                                              @PathVariable(INSTALLATION_ID_ALIAS) String idSite,
                                              HttpServletResponse response) {
        try {
            // 不能把自己的设备取消共享
            checkParameter(USER_ID, strUserId);
            if (String.valueOf(getCurrentUser().getId()).equals(strUserId)) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            checkParameter(INSTALLATION_ID_ALIAS, idSite);
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId, Operation.UNASSIGN_FROM_CUSTOMER);
            InstallationId installationId = new InstallationId(toUUID(idSite));
            Installation installation = checkInstallationId(installationId, Operation.UNASSIGN_FROM_CUSTOMER);
            if (user.getCustomerId() == null || user.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            if (installation.getCustomerId() == null || installation.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                throw new IncorrectParameterException("Device isn't assigned to any customer!");
            }
            tbInstallationService.unassignInstallationFromUser(installation, user.getCustomerId(), getCurrentUser());
            return R.success(null);
        } catch (ThingsboardException e) {
            handleControllerException(e, response);
        }
        return R.error(null);
    }
}
