package org.openecomp.sdc.be.auditing.impl.externalapi;

import org.openecomp.sdc.be.model.User;
import org.openecomp.sdc.be.resources.data.auditing.AuditingActionEnum;
import org.openecomp.sdc.be.resources.data.auditing.model.CommonAuditData;
import org.openecomp.sdc.be.resources.data.auditing.model.ResourceAuditData;

public class AuditUploadArtByApiCrudExternalApiEventFactory extends AuditCrudExternalApiArtifactEventFactory {

    public AuditUploadArtByApiCrudExternalApiEventFactory(CommonAuditData commonFields, String resourceType, String resourceName,
                                                          String consumerId, String resourceUrl, ResourceAuditData prevParams, ResourceAuditData currParams,
                                                          String invariantUuid, User modifier, String artifactData) {
        super(AuditingActionEnum.ARTIFACT_UPLOAD_BY_API, commonFields, resourceType, resourceName, consumerId, resourceUrl, prevParams, currParams,
                invariantUuid, modifier, artifactData);
    }
}
