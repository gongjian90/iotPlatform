/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   BatteryVenusVO.java
 * Author: gongjian   ID:     Version:    Date: 2023/1/3
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.common.data.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BatteryVenusTotalVO {
    private BigDecimal bs = new BigDecimal(0);
    private BigDecimal ac_loads = new BigDecimal(0);
    private BigDecimal consumption = new BigDecimal(0);
    private BigDecimal consumption_input = new BigDecimal(0);
    private BigDecimal consumption_output = new BigDecimal(0);
    private BigDecimal solar_yield = new BigDecimal(0);
    private BigDecimal hub_inverter = new BigDecimal(0);
    private BigDecimal from_to_grid = new BigDecimal(0);
    private BigDecimal ac_in = new BigDecimal(0);
    private BigDecimal ac_out = new BigDecimal(0);
    private BigDecimal ac_genset = new BigDecimal(0);
    private BigDecimal total_solar_yield = new BigDecimal(0);
    @JsonProperty(value = "Pdc")
    private Boolean pdc = false;
    @JsonProperty(value = "dc")
    private Boolean dc = false;
    @JsonProperty(value = "tsT")
    private Boolean tsT = false;
    @JsonProperty(value = "evp")
    private Boolean evp = false;


    /*public static void main(String[] args) {
        BatteryVenusTotalVO batteryVenusTotalVO = new BatteryVenusTotalVO();
        batteryVenusTotalVO.setBs(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setAc_loads(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setConsumption_output(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setConsumption_input(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setConsumption(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setSolar_yield(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setHub_inverter(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setFrom_to_grid(new BigDecimal("1195.4087431693993"));
        batteryVenusTotalVO.setAc_out(new BigDecimal("50871.54131409214"));
        batteryVenusTotalVO.setTotal_solar_yield(new BigDecimal("12.380000000819564"));

        System.out.println(JSONObject.toJSONString(batteryVenusTotalVO));
    }*/
}
