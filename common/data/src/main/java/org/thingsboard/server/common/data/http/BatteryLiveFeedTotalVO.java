/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   BatteryTotalVO.java
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
public class BatteryLiveFeedTotalVO {
    private BigDecimal bs = new BigDecimal(0);
    private BigDecimal bv = new BigDecimal(0);
    private BigDecimal total_solar_yield = new BigDecimal(0);
    private BigDecimal total_consumption = new BigDecimal(0);
    private BigDecimal total_genset = new BigDecimal(0);
    private BigDecimal grid_history_to = new BigDecimal(0);
    private BigDecimal grid_history_from = new BigDecimal(0);
    private BigDecimal history_charged_energy = new BigDecimal(0);
    private BigDecimal history_discharged_energy = new BigDecimal(0);
    @JsonProperty(value = "Pdc")
    private Boolean pdc = false;
    @JsonProperty(value = "iOI1")
    private Boolean iOI1 = false;

    /*public static void main(String[] args) {
        BatteryLiveFeedTotalVO batteryLiveFeedTotalVO = new BatteryLiveFeedTotalVO();
        batteryLiveFeedTotalVO.setBs(new BigDecimal("1195.4087431693993"));
        batteryLiveFeedTotalVO.setBv(new BigDecimal("1195.4087431693993"));
        batteryLiveFeedTotalVO.setGrid_history_from(new BigDecimal("1195.4087431693993"));
        batteryLiveFeedTotalVO.setGrid_history_to(new BigDecimal("1195.4087431693993"));
        batteryLiveFeedTotalVO.setTotal_consumption(new BigDecimal("1195.4087431693993"));
        batteryLiveFeedTotalVO.setTotal_solar_yield(new BigDecimal("1195.4087431693993"));

        System.out.println(JSONObject.toJSONString(batteryLiveFeedTotalVO));
    }*/
}
