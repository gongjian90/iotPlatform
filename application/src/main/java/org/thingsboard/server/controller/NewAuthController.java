/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DRGKController.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/2
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.NewUser;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.R;
import org.thingsboard.server.common.data.http.UserRegisterVO;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.ChangePasswordRequest;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import javax.servlet.http.HttpServletRequest;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.ControllerConstants.USER_ID;
import static org.thingsboard.server.controller.TbUrlConstants.NEW_URL_PREFIX;

@RestController
@TbCoreComponent
@RequestMapping(NEW_URL_PREFIX)
@Slf4j
@RequiredArgsConstructor
public class NewAuthController extends BaseController {

    private static final String DEALER = "dealer";
    private static final String COMPANY = "company";
    @ApiOperation(value = "Save User (register)",
            notes = "Create the User. When creating user, platform generates User Id as " + UUID_WIKI_LINK +
                    "The newly created User Id will be present in the response. " +
                    "\n\nDevice email is unique for entire platform setup." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new User entity.")
    //@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/noauth/register", method = RequestMethod.POST)
    @ResponseBody
    public R<Object> register(
            @ApiParam(value = "A JSON value representing the User.", required = true)
            @RequestBody UserRegisterVO userRegisterVO) throws ThingsboardException {
        try {
            checkNotNull(userRegisterVO);
            ObjectMapper mapper = new ObjectMapper();
            User user = new User();
            user.setEmail(userRegisterVO.getEmail());
            user.setFirstName(userRegisterVO.getName());
            user.setLastName(userRegisterVO.getName());
            JsonNode userAdditionalInfo = mapper.readTree("{\"description\":\"\",\"defaultDashboardId\":null,\"defaultDashboardFullscreen\":false,\"homeDashboardId\":null,\"homeDashboardHideToolbar\":true}");
            user.setAdditionalInfo(userAdditionalInfo);

            Customer customer = new Customer();
            customer.setTitle(userRegisterVO.getName());
            customer.setCity(userRegisterVO.getCity());
            customer.setCountry(userRegisterVO.getCountry());
            customer.setPhone(userRegisterVO.getPhone());
            JsonNode customerAdditionalInfo = mapper.readTree("{\"description\":\"\",\"homeDashboardId\":null,\"homeDashboardHideToolbar\":true}");
            customerAdditionalInfo = JacksonUtil.addJsonNode(customerAdditionalInfo, COMPANY, userRegisterVO.getCompany());
            customerAdditionalInfo = JacksonUtil.addJsonNode(customerAdditionalInfo, DEALER, userRegisterVO.getDealer_name());
            customer.setAdditionalInfo(customerAdditionalInfo);

            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setPassword(userRegisterVO.getPassword());
            newTbUserService.save(user, customer, userCredentials);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return R.success(null);
    }

    @ApiOperation(value = "Logout (logout)",
            notes = "Special API call to record the 'logout' of the user to the Audit Logs. Since platform uses [JWT](https://jwt.io/), the actual logout is the procedure of clearing the [JWT](https://jwt.io/) token on the client side. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/logout", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public R<Object> logout(HttpServletRequest request) throws ThingsboardException {
        logLogoutAction(request);
        return R.success(null);
    }

    @ApiOperation(value = "Change password for current User (changePassword)",
            notes = "Change the password for the User which credentials are used to perform this REST API call. Be aware that previously generated [JWT](https://jwt.io/) tokens will be still valid until they expire.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/changePassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public R<Object> changePassword(
            @ApiParam(value = "Change Password Request")
            @RequestBody ChangePasswordRequest changePasswordRequest) throws ThingsboardException {
        try {
            String currentPassword = changePasswordRequest.getCurrentPassword();
            String newPassword = changePasswordRequest.getNewPassword();
            SecurityUser securityUser = getCurrentUser();
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, securityUser.getId());
            if (!passwordEncoder.matches(currentPassword, userCredentials.getPassword())) {
                throw new ThingsboardException("Current password doesn't match!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            systemSecurityService.validatePassword(securityUser.getTenantId(), newPassword, userCredentials);
            if (passwordEncoder.matches(newPassword, userCredentials.getPassword())) {
                throw new ThingsboardException("New password should be different from existing!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            userCredentials.setPassword(passwordEncoder.encode(newPassword));
            userService.replaceUserCredentials(securityUser.getTenantId(), userCredentials);

            sendEntityNotificationMsg(getTenantId(), userCredentials.getUserId(), EdgeEventActionType.CREDENTIALS_UPDATED);

            eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

            return R.auth(tokenFactory.createAccessJwtToken(securityUser).getToken(), tokenFactory.createRefreshToken(securityUser).getToken(), null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete User (deleteUser)",
            notes = "Deletes the User, it's credentials and all the relations (from and to the User). " +
                    "Referencing non-existing User Id will cause an error. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/user/{userId}", method = RequestMethod.DELETE, name = "删除用户")
    @ResponseStatus(value = HttpStatus.OK)
    @Transactional(rollbackFor = Exception.class)
    public R<Integer> deleteUserTenant(@ApiParam(value = USER_ID_PARAM_DESCRIPTION)
                                 @PathVariable(USER_ID) String strUserId) throws Exception {
        // 用户删除
        checkParameter(USER_ID, strUserId);
        UserId userId = new UserId(toUUID(strUserId));
        User user = checkUserId(userId, Operation.DELETE);
        TenantId tenantId = getCurrentUser().getTenantId();
        if (Authority.SYS_ADMIN.equals(user.getAuthority()) && getCurrentUser().getId().equals(userId)) {
            throw new ThingsboardException("Sysadmin is not allowed to delete himself", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        newTbUserService.delete(getTenantId(), getCurrentUser().getCustomerId(), user, getCurrentUser());
        // 租户删除
        if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
            Tenant tenant = checkTenantId(tenantId, Operation.DELETE);
            newTbUserService.deleteTenant(tenant);
        } else {
            Customer customer = checkCustomerId(getCurrentUser().getCustomerId(), Operation.DELETE);
            newTbUserService.deleteCustomer(customer, getCurrentUser());
        }

        return R.success(1);
    }

    @ApiOperation(value = "Get current User (getUser)",
            notes = "Get the information about the User which credentials are used to perform this REST API call.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/info", method = RequestMethod.GET, name = "查看用户详细信息")
    @ResponseBody
    public UserRegisterVO getUser() throws ThingsboardException {
        try {
            SecurityUser securityUser = getCurrentUser();
            if (Authority.TENANT_ADMIN.equals(securityUser.getAuthority())) {
                return getUserRegisterVO(checkNotNull(userService.findUserById(securityUser.getTenantId(), securityUser.getId())),
                        checkNotNull(tenantService.findTenantById(securityUser.getTenantId())));
            }
            return getUserRegisterVO(checkNotNull(userService.findUserById(securityUser.getTenantId(), securityUser.getId())),
                    checkNotNull(customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId())));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update current User (updateUser)",
            notes = "Update the information about the User which credentials are used to perform this REST API call.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/info", method = RequestMethod.POST, name = "修改用户详细信息")
    @ResponseBody
    public R<UserRegisterVO> updateUser(@ApiParam(value = "A JSON value representing the User.", required = true)
                                         @RequestBody UserRegisterVO userRegisterVO) throws ThingsboardException {
        try {
            checkNotNull(userRegisterVO);
            checkParameter("userId", userRegisterVO.getId());
            User user = checkUserId(new UserId(toUUID(userRegisterVO.getId())), Operation.WRITE);
            if (Authority.TENANT_ADMIN.equals(user.getAuthority())) {
                Tenant tenant = checkTenantId(user.getTenantId(), Operation.WRITE);

                if (StringUtils.isNotBlank(userRegisterVO.getEmail())) {
                    user.setEmail(userRegisterVO.getEmail());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getName())) {
                    user.setFirstName(userRegisterVO.getName());
                    user.setLastName(userRegisterVO.getName());
                    tenant.setTitle(userRegisterVO.getName());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getCity())) {
                    tenant.setCity(userRegisterVO.getCity());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getCountry())) {
                    tenant.setCountry(userRegisterVO.getCountry());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getPhone())) {
                    tenant.setPhone(userRegisterVO.getPhone());
                }
                JsonNode customerAdditionalInfo = tenant.getAdditionalInfo();
                if (StringUtils.isNotBlank(userRegisterVO.getCompany())) {
                    customerAdditionalInfo = JacksonUtil.addJsonNode(customerAdditionalInfo, COMPANY, userRegisterVO.getCompany());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getDealer_name())) {
                    customerAdditionalInfo = JacksonUtil.addJsonNode(customerAdditionalInfo, DEALER, userRegisterVO.getDealer_name());
                }
                tenant.setAdditionalInfo(customerAdditionalInfo);
                NewUser newUser = checkNotNull(newTbUserService.update(user, tenant));
                return R.success(getUserRegisterVO(newUser.getUser(), newUser.getTenant()));
            } else {
                Customer customer = checkCustomerId(user.getCustomerId(), Operation.WRITE);

                if (StringUtils.isNotBlank(userRegisterVO.getEmail())) {
                    user.setEmail(userRegisterVO.getEmail());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getName())) {
                    user.setFirstName(userRegisterVO.getName());
                    user.setLastName(userRegisterVO.getName());
                    customer.setTitle(userRegisterVO.getName());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getCity())) {
                    customer.setCity(userRegisterVO.getCity());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getCountry())) {
                    customer.setCountry(userRegisterVO.getCountry());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getPhone())) {
                    customer.setPhone(userRegisterVO.getPhone());
                }
                JsonNode customerAdditionalInfo = customer.getAdditionalInfo();
                if (StringUtils.isNotBlank(userRegisterVO.getCompany())) {
                    customerAdditionalInfo = JacksonUtil.addJsonNode(customerAdditionalInfo, COMPANY, userRegisterVO.getCompany());
                }
                if (StringUtils.isNotBlank(userRegisterVO.getDealer_name())) {
                    customerAdditionalInfo = JacksonUtil.addJsonNode(customerAdditionalInfo, DEALER, userRegisterVO.getDealer_name());
                }
                customer.setAdditionalInfo(customerAdditionalInfo);
                NewUser newUser = checkNotNull(newTbUserService.update(user, customer));
                return R.success(getUserRegisterVO(newUser.getUser(), newUser.getCustomer()));
            }

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private UserRegisterVO getUserRegisterVO(User user, Customer customer) {
        UserRegisterVO result = new UserRegisterVO();
        result.setId(user.getId().getId().toString());
        result.setEmail(user.getEmail());
        result.setName(user.getFirstName());
        result.setCity(customer.getCity());
        result.setCountry(customer.getCountry());
        result.setPhone(customer.getPhone());
        JsonNode additionalInfo = customer.getAdditionalInfo();
        result.setDealer_name(additionalInfo.get(DEALER) == null ? additionalInfo.get(DEALER).asText() : null);
        result.setCompany(additionalInfo.get(COMPANY) == null ? additionalInfo.get(COMPANY).asText() : null);
        result.setAllowLogin(true);
        result.setTwostepEnabled(false);
        result.setUseFahrenheit(false);
        result.setUseGallons(false);
        result.setWeekday("auto");
        result.setAlarmNotificationsMuted(false);
        result.setWhiteLabel(false);
        return result;
    }
    private UserRegisterVO getUserRegisterVO(User user, Tenant tenant) {
        UserRegisterVO result = new UserRegisterVO();
        result.setId(user.getId().getId().toString());
        result.setEmail(user.getEmail());
        result.setName(user.getFirstName());
        result.setCity(tenant.getCity());
        result.setCountry(tenant.getCountry());
        result.setPhone(tenant.getPhone());
        JsonNode additionalInfo = tenant.getAdditionalInfo();
        result.setDealer_name(additionalInfo.get(DEALER) == null ? additionalInfo.get(DEALER).asText() : null);
        result.setCompany(additionalInfo.get(COMPANY) == null ? additionalInfo.get(COMPANY).asText() : null);
        result.setAllowLogin(true);
        result.setTwostepEnabled(false);
        result.setUseFahrenheit(false);
        result.setUseGallons(false);
        result.setWeekday("auto");
        result.setAlarmNotificationsMuted(false);
        result.setWhiteLabel(false);
        return result;
    }
}
