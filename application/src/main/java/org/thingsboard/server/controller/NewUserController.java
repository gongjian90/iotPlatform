/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   NewUserController.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/7
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
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.R;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.servlet.http.HttpServletRequest;

import static org.thingsboard.server.controller.ControllerConstants.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION;
import static org.thingsboard.server.controller.TbUrlConstants.NEW_URL_PREFIX;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping(NEW_URL_PREFIX)
public class NewUserController extends BaseController {

    @ApiOperation(value = "update User (updateUser)",
            notes = "Modify FirstName/LastName is support by User Id. " +
                    "\n\nAvailable for users with 'SYS_ADMIN', 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public R<User> updateUser(
            @ApiParam(value = "A JSON value representing the User.", required = true)
            @RequestBody User user,
            @ApiParam(value = "Send activation email (or use activation link)", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail, HttpServletRequest request) throws ThingsboardException {
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            user.setTenantId(getCurrentUser().getTenantId());
        }
        // 权限校验
        checkEntity(user.getId(), user, Resource.USER);
        User queryUser = checkUserId(user.getId(), Operation.WRITE);
        if (queryUser == null) {
            throw new ThingsboardException("Incorrect userId ", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (user.equals(queryUser)) {
            throw new ThingsboardException("Nothing changed ", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (user.getAdditionalInfo() != null) {
            queryUser.setAdditionalInfo(user.getAdditionalInfo());
        }
        if (user.getFirstName() != null) {
            queryUser.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            queryUser.setLastName(user.getLastName());
        }
        if (user.getAuthority() != null && !user.getAuthority().equals(queryUser.getAuthority())) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (user.getName() != null && !user.getName().equals(queryUser.getName())) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (user.getEmail() != null && !user.getEmail().equals(queryUser.getEmail())) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (user.getSearchText() != null && !user.getSearchText().equals(queryUser.getSearchText())) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (user.getAuthority() != null && !user.getAuthority().equals(queryUser.getAuthority())) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        User saveUser = tbUserService.save(getTenantId(), getCurrentUser().getCustomerId(), queryUser, sendActivationMail, request, getCurrentUser());
        return R.success(saveUser);
    }
}
