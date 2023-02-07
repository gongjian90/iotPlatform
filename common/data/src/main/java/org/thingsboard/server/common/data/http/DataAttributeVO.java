/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DataAttributeVO.java
 * Author: gongjian   ID:     Version:    Date: 2022/12/7
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
public class DataAttributeVO {
    private String code;
    private String idDataAttribute;
    private String secondsAgo;
    private String secondsToNextLog;
    private String value;                       //"52.56"
    private String valueFloat;                  //"52.56 V"
    private String dataType;                    //"float"
    private String dbusServiceType;
    private String dbusPath;
    private Integer instance;
    private String valueString;
    private String valueEnum;
    private String nameEnum;
    private String isValid;                     //"1"
    private String isKeyIdDataAttribute;        //"1"
    private String formatValueOnly;             //"%.2F"
    private String formatWithUnit;              //"%.2F V"
    private String dataAttributeName;
    private String rawValue;                    //"52.56"
    private String formattedValue;              //"52.56 V"
    private String valueFormattedValueOnly;     //"52.56"
    private String valueFormattedWithUnit;      //"52.56 V"
    private Boolean hasOldData;
    private List<DataAttributeEnumValue> dataAttributeEnumValues = null;
}
