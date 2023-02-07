/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   InstallationEnum.java
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

import lombok.Getter;

public enum InstallationEnum {

    FW_VERSION(2, "v","Fw Version","%s","string","0",null,"0",null,null, null),
    VE_BUS_STATE(40, "S","VE.Bus state","%s","enum","275","Ext. control","0","vebus","/State",
            new DataAttributeEnumValue[]{
                    new DataAttributeEnumValue("Off",0),
                    new DataAttributeEnumValue("Low power",1),
                    new DataAttributeEnumValue("Fault",2),
                    new DataAttributeEnumValue("Bulk",3),
                    new DataAttributeEnumValue("Absorption",4),
                    new DataAttributeEnumValue("Float",5),
                    new DataAttributeEnumValue("Storage",6),
                    new DataAttributeEnumValue("Equalize",7)
            }),
    VOLTAGE(143, "bv","Voltage","%.2F V","float","6",null,"0","system","/Dc/Battery/Voltage", null),
    SOC(144, "bs","Battery State of Charge","%.1F %%","float","6",null,"0","system","/Dc/Battery/Soc", null),
    CURRENT(147, "bc","Current","%.2F A","float","6",null,"0","system","/Dc/Battery/Current", null),
    BATTERY_STATE(215, "bs","Battery state","%s","enum","6","idle","0","system","/Dc/Battery/State",
            new DataAttributeEnumValue[]{
                    new DataAttributeEnumValue("Idle",0),
                    new DataAttributeEnumValue("Charging",1),
                    new DataAttributeEnumValue("Discharging",2)
            }),
    DEFAULT_(0, "","","","","","","","","",null);
    @Getter
    private final Integer idDataAttribute;
    @Getter
    private final String code;
    @Getter
    private final String description;
    @Getter
    private final String formatWithUnit;
    @Getter
    private final String dataType;
    @Getter
    private final String idDeviceType;
    @Getter
    private final String textValue;
    @Getter
    private final String instance;
    @Getter
    private final String dbusServiceType;
    @Getter
    private final String dbusPath;
    @Getter
    private final DataAttributeEnumValue[] dataAttributeEnumValues;

    InstallationEnum(Integer idDataAttribute,
                     String code,
                     String description,
                     String formatWithUnit,
                     String dataType,
                     String idDeviceType,
                     String textValue,
                     String instance,
                     String dbusServiceType,
                     String dbusPath,
                     DataAttributeEnumValue[] dataAttributeEnumValues) {
        this.idDataAttribute = idDataAttribute;
        this.code = code;
        this.description = description;
        this.formatWithUnit = formatWithUnit;
        this.dataType = dataType;
        this.idDeviceType = idDeviceType;
        this.textValue = textValue;
        this.instance = instance;
        this.dbusServiceType = dbusServiceType;
        this.dbusPath = dbusPath;
        this.dataAttributeEnumValues = dataAttributeEnumValues;
    }
}
