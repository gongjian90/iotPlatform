/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Installation;
import org.thingsboard.server.common.data.InstallationTypeEnum;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.InstallationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.installation.data.InstallationData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonStringType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Table(name = ModelConstants.INSTALLATION_COLUMN_FAMILY_NAME)
public class InstallationEntity extends BaseSqlEntity<Installation> implements SearchTextEntity<Installation> {

    private static final ObjectMapper mapper = new ObjectMapper();
    @Column(name = ModelConstants.INSTALLATION_PORTAL_ID_PROPERTY)
    private String portalId;
    @Column(name = ModelConstants.INSTALLATION_NAME_PROPERTY)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.INSTALLATION_TYPE_PROPERTY)
    private InstallationTypeEnum type;
    @Column(name = ModelConstants.CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;
    @Column(name = ModelConstants.INSTALLATION_GSM_NUMBER_PROPERTY)
    private String gsmNumber;
    @Column(name = ModelConstants.INSTALLATION_IMAGE_PROPERTY)
    private String image;
    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;
    @Type(type = "jsonb")
    @Column(name = ModelConstants.INSTALLATION_INSTALLATION_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode installationData;
    @Column(name = ModelConstants.INSTALLATION_DESCRIPTION_PROPERTY)
    private String description;
    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;
    @Column(name = ModelConstants.TENANT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantId;
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY, columnDefinition = "uuid")
    private UUID externalId;

    public InstallationEntity() {
    }
    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    public String getSearchText() {
        return searchText;
    }
    @Override
    public String getSearchTextSource() {
        return name;
    }
    public InstallationEntity(Installation installation) {
        if (installation.getId() != null) {
            this.setId(installation.getId().getId());
        }
        this.createdTime = installation.getCreatedTime();
        this.portalId = installation.getPortalId();
        this.name = installation.getName();
        this.type = installation.getType();
        this.customerId = DaoUtil.getId(installation.getCustomerId());
        this.gsmNumber = installation.getGsmNumber();
        this.image = installation.getImage();
        this.additionalInfo = installation.getAdditionalInfo();
        this.installationData = JacksonUtil.convertValue(installation.getInstallationData(), ObjectNode.class);
        this.description = installation.getDescription();
        this.tenantId = DaoUtil.getId(installation.getTenantId());
        if (installation.getExternalId() != null) {
            this.externalId = installation.getExternalId().getId();
        }
    }

    @Override
    public Installation toData() {
        Installation installation = new Installation(new InstallationId(getUuid()));
        installation.setCreatedTime(createdTime);
        installation.setPortalId(portalId);
        installation.setName(name);
        if (type != null) {
            installation.setType(type);
        }
        installation.setCustomerId(new CustomerId(customerId));
        if (gsmNumber != null) {
            installation.setGsmNumber(gsmNumber);
        }
        if (image != null) {
            installation.setImage(image);
        }
        if (additionalInfo != null) {
            installation.setAdditionalInfo(additionalInfo);
        }
        if (installationData != null) {
            installation.setInstallationData(JacksonUtil.convertValue(installationData, InstallationData.class));
        }
        installation.setDescription(description);
        installation.setTenantId(new TenantId(tenantId));
        if (externalId != null) {
            installation.setExternalId(new InstallationId(externalId));
        }
        return installation;
    }


}