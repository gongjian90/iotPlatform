/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   InstallationInfoVO.java
 * Author: gongjian   ID:     Version:    Date: 2023/1/19
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.common.data.http;

import lombok.Data;

import java.util.List;

@Data
public class InstallationInfoVO {

    private String idSite;
    private Integer accessLevel;
    private Boolean owner;
    private Boolean is_admin;
    private String name;
    private String identifier;
    private String idUser;
    private Long pvMax;
    private String timezone;
    private String phonenumber;
    private String notes;
    private String geofence;
    private Boolean geofenceEnabled;
    private Boolean realtimeUpdates;
    private Integer hasMains;
    private Integer hasGenerator;
    private String noDataAlarmTimeout;
    private Integer alarmMonitoring;
    private Integer invalidVRMAuthTokenUsedInLogRequest;
    private Long syscreated;
    private Integer grafanaEnabled;
    private Integer isPaygo;
    private String paygoCurrency;
    private String paygoTotalAmount;
    private Integer inverterChargerControl;
    private Boolean shared;
    private String device_icon;
    private Boolean alarm;
    private Long last_timestamp;
    private List<TagVO> tags;
    private String current_time;
    private Long timezone_offset;
    private Boolean images;
    private ViewPermission view_permissions;
    private List<InstallationAttributeVO> extended;
}
