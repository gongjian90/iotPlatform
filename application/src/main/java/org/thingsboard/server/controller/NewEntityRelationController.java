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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.http.R;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.TbUrlConstants.NEW_URL_PREFIX;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping(NEW_URL_PREFIX)
public class NewEntityRelationController extends BaseController {

    @ApiOperation(value = "Get User Device (getDeviceIdsByUser)",
            notes = "Requested device must be owned by user that the user belongs to. " +
                    "Device name is an unique property of device. So it can be used to identify the device." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/deviceIds", method = RequestMethod.GET)
    @ResponseBody
    public R<List<EntityRelation>> getDeviceIdsByUser() throws ThingsboardException {
        try {
            RelationTypeGroup typeGroup = parseRelationTypeGroup("USER_DEVICE", RelationTypeGroup.COMMON);
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                return R.success(checkNotNull(relationService.findByFrom(getTenantId(), getTenantId(), typeGroup)));
            } else {
                return R.success(checkNotNull(relationService.findByFrom(getTenantId(), getCurrentUser().getCustomerId(), typeGroup)));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
