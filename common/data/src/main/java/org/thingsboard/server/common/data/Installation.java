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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.installation.data.InstallationData;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

@ApiModel
@ToString(exclude = {"image", "installationDataBytes"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Installation extends SearchTextBasedWithAdditionalInfo<InstallationId> implements HasName, HasTenantId, ExportableEntity<InstallationId> {

    @ApiModelProperty(position = 1, value = "JSON object with the Device Id. " +
            "Specify this field to update the Device. " +
            "Referencing non-existing Device Id will cause error. " +
            "Omit this field to create new Device." )
    @Override
    public InstallationId getId() {
        return super.getId();
    }
    @ApiModelProperty(position = 2, value = "Timestamp of the user creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
    @ApiModelProperty(position = 3, required = true, value = "Portal ID", example = "123456789")
    private String portalId;
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 4, required = true, value = "Installation Name (Optional)")
    private String name;
    @ApiModelProperty(position = 5, required = true, value = "The type of product", example = "COLOR_CONTROL_GX")
    private InstallationTypeEnum type;
    @ApiModelProperty(position = 6, value = "JSON object with Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private CustomerId customerId;
    @ApiModelProperty(position = 7, required = true, value = "GSM number")
    private String gsmNumber;
    @ApiModelProperty(position = 8, required = true, value = "image")
    private String image;
    @ApiModelProperty(position = 9, value = "Additional parameters of the installation", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }
    @ApiModelProperty(position = 10, value = "Complex JSON object that includes addition installation configuration (information, params, etc).")
    private transient InstallationData installationData;
    @JsonIgnore
    private byte[] installationDataBytes;
    @NoXss
    @ApiModelProperty(position = 11, value = "Installation description. ")
    private String description;
    @Override
    public String getSearchText() {
        return getName();
    }
    @ApiModelProperty(position = 12, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Getter
    @Setter
    private InstallationId externalId;

    public Installation() {
        super();
    }

    public Installation(InstallationId id) {
        super(id);
    }
    @Override
    public void setId(InstallationId id) {
        this.id = id;
    }
    @Override
    public void setExternalId(InstallationId externalId) {
        this.externalId = externalId;
    }
    @Override
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }
    public TenantId getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPortalId() {
        return portalId;
    }

    public void setPortalId(String portalId) {
        this.portalId = portalId;
    }

    public InstallationTypeEnum getType() {
        return type;
    }

    public void setType(InstallationTypeEnum type) {
        this.type = type;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    public String getGsmNumber() {
        return gsmNumber;
    }

    public void setGsmNumber(String gsmNumber) {
        this.gsmNumber = gsmNumber;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    public InstallationData getInstallationData() {
        if (installationData != null) {
            return installationData;
        } else {
            if (installationDataBytes != null) {
                try {
                    installationData = mapper.readValue(new ByteArrayInputStream(installationDataBytes), InstallationData.class);
                } catch (IOException e) {
                    log.warn("Can't deserialize installation data: ", e);
                    return null;
                }
                return installationData;
            } else {
                return null;
            }
        }
    }

    public void setInstallationData(InstallationData data) {
        this.installationData = data;
        try {
            this.installationDataBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize installation data: ", e);
        }
    }
    public byte[] getInstallationDataBytes() {
        return installationDataBytes;
    }

    public void setInstallationDataBytes(byte[] installationDataBytes) {
        this.installationDataBytes = installationDataBytes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InstallationId getExternalId() {
        return externalId;
    }

    public Installation(Installation installation) {
        super(installation);
        this.portalId = installation.getPortalId();
        this.name = installation.getName();
        this.type = installation.getType();
        this.customerId = installation.getCustomerId();
        this.gsmNumber = installation.getGsmNumber();
        this.image = installation.getImage();
        Optional.ofNullable(installation.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        this.setInstallationData(installation.getInstallationData());
        this.description = installation.getDescription();
        this.tenantId = installation.getTenantId();
        this.externalId = installation.getExternalId();
    }

    public Installation updateInstallation(Installation installation) {
        this.id = installation.getId();
        this.portalId = installation.getPortalId();
        this.name = installation.getName();
        this.type = installation.getType();
        this.customerId = installation.getCustomerId();
        this.gsmNumber = installation.getGsmNumber();
        this.image = installation.getImage();
        Optional.ofNullable(installation.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        this.setInstallationData(installation.getInstallationData());
        this.description = installation.getDescription();
        this.tenantId = installation.getTenantId();
        this.externalId = installation.getExternalId();
        return this;
    }


}
