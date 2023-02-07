/*
 * Copyright (c) Daren Hi-Tech Electronics Co., Ltd. Development Team 2020-2022. All rights reserved.
 * File name:   ParamRepository.java
 * Author: gongjian   ID:     Version:    Date: 2022/11/26
 * Description: // 用于详细说明此程序文件完成的主要功能，与其他模块
 *              // 或函数的接口，输出值、取值范围、含义及参数间的控
 *              // 制、顺序、独立或依赖等关系
 * Others:      // 其它内容的说明
 * History:     // 修改历史记录列表，每条修改记录应包括修改日期、修改
 *              // 者及修改内容简述
 *              Date           Author       Notes
 */
package org.thingsboard.server.dao.sql.installaion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.InstallationEntity;

import java.util.List;
import java.util.UUID;

public interface InstallationRepository extends JpaRepository<InstallationEntity, UUID>, ExportableEntityRepository<InstallationEntity> {

    @Query("SELECT d FROM InstallationEntity d WHERE d.tenantId = :tenantId AND d.customerId = :customerId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<InstallationEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);
    @Query("SELECT d FROM InstallationEntity d WHERE d.portalId = :portalId")
    InstallationEntity findByPortalId(@Param("portalId") String portalId);
    @Query("SELECT d FROM InstallationEntity d WHERE d.tenantId = :tenantId")
    Page<InstallationEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
    @Query("SELECT d FROM InstallationEntity d WHERE d.tenantId = :tenantId " +
            "AND LOWER(d.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<InstallationEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                            @Param("textSearch") String textSearch,
                                            Pageable pageable);
    InstallationEntity findByTenantIdAndId(UUID tenantId, UUID id);

    List<InstallationEntity> findByCustomerId(UUID customerId);
    List<InstallationEntity> findInstallationEntitiesByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
