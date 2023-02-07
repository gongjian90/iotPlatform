/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   InstallationAttributeVO.java
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
public class InstallationAttributeVO {
    private Integer idDataAttribute;
    private String code;
    private String description;
    private String formatWithUnit;
    private String dataType;
    private String idDeviceType;
    private String textValue;
    private Object instance;
    private String timestamp;
    private String dbusServiceType;
    private String dbusPath;
    private String rawValue;
    private String formattedValue;
    private List<DataAttributeEnumValue> dataAttributeEnumValues = null;
    private List<InstallationDataAttributeVO> dataAttributes = null;
}
