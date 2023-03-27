/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   DataInitializeRunner.java
 * Author: gongjian   ID:     Version:    Date: 2023/3/2
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.action.RootAction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author ctj
 * @create 2020-06-11-14:32
 * @description 初始化系统
 * @RES https://blog.csdn.net/qq_38628241/article/details/126653252
 */
@Component
public class DataInitializeRunner implements ApplicationRunner {

    @Value("${local-file.port}")
    private int fileServerPort;
    @Value("${local-file.root}")
    private String uploadRoot;

    @Override
    public void run(ApplicationArguments args) {
        /**
         * 创建文件服务
         */
        HttpUtil.createServer(fileServerPort)
                .setRoot(uploadRoot)
                .start();
    }
}