/*
* Copyright © 2016-2018 European Support Limited
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.openecomp.sdc.enrichment.impl.tosca;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.openecomp.core.utilities.orchestration.OnboardingTypesEnum;
import org.openecomp.sdc.datatypes.error.ErrorMessage;
import org.openecomp.sdc.enrichment.impl.tosca.model.PortMirroringConnectionPointDescription;
import org.openecomp.sdc.tosca.datatypes.ToscaNodeType;
import org.openecomp.sdc.tosca.datatypes.ToscaServiceModel;
import org.openecomp.sdc.tosca.datatypes.model.Import;
import org.openecomp.sdc.tosca.datatypes.model.NodeTemplate;
import org.openecomp.sdc.tosca.datatypes.model.RequirementAssignment;
import org.openecomp.sdc.tosca.datatypes.model.ServiceTemplate;
import org.openecomp.sdc.tosca.services.DataModelUtil;
import org.openecomp.sdc.tosca.services.ToscaConstants;
import org.openecomp.sdc.tosca.services.ToscaUtil;
import org.openecomp.sdc.translator.services.heattotosca.HeatToToscaUtil;
import org.openecomp.sdc.translator.services.heattotosca.globaltypes.GlobalTypesGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openecomp.sdc.tosca.services.DataModelUtil.getClonedObject;
import static org.openecomp.sdc.tosca.services.ToscaConstants.PORT_MIRRORING_CAPABILITY_CP_PROPERTY_NAME;
import static org.openecomp.sdc.tosca.services.ToscaConstants.PORT_MIRRORING_CAPABILITY_ID;

public class PortMirroringEnricher {
  //Map of service template file name and map of all port node template ids, node template
  private Map<String, Map<String, NodeTemplate>> portNodeTemplates = new HashMap<>();
  //Map of service template file name and map of external port node template ids, node template
  private Map<String, Map<String, NodeTemplate>> externalPortNodeTemplates = new HashMap<>();
  //Map of substitution service template name and the list of ports with link requirement from
  // the abstract
  private Map<String, List<String>> portNodeTemplateIdsFromAbstract = new HashMap<>();
  private Map<String, ServiceTemplate> globalTypesServiceTemplate =
      GlobalTypesGenerator.getGlobalTypesServiceTemplate(OnboardingTypesEnum.ZIP);

  /**
   * Enrich tosca for port mirroring.
   *
   * @param toscaServiceModel the tosca service model
   * @return the map          Error descriptor map
   */
  public Map<String, List<ErrorMessage>> enrich(ToscaServiceModel toscaServiceModel) {
    Map<String, List<ErrorMessage>> errors = new HashMap<>();
    Map<String, ServiceTemplate> serviceTemplates = toscaServiceModel.getServiceTemplates();
    serviceTemplates.entrySet().stream()
        //Skipping the service templates which do not contain topology template
        .filter(serviceTemplateEntry -> serviceTemplateEntry.getValue()
            .getTopology_template() != null)
        .forEach(serviceTemplateEntry ->
            //Collect all the ports across all the service templates
            collectPorts(serviceTemplateEntry.getValue()));
    //Collect External ports from the list of all ports collected above
    filterExternalPorts();
    //Handle external port changes
    handleExternalPorts(toscaServiceModel);
    return errors;
  }

  private void collectPorts(ServiceTemplate serviceTemplate) {
    Map<String, NodeTemplate> nodeTemplates =
        serviceTemplate.getTopology_template().getNode_templates();
    if (Objects.nonNull(nodeTemplates)) {
      //Get all concrete port node templates from the service template
      Map<String, NodeTemplate> serviceTemplatePortNodeTemplates = nodeTemplates.entrySet().stream()
          .filter(nodeTemplateEntry -> (Objects.nonNull(nodeTemplateEntry.getValue()))
              && (isPortNodeTemplate(nodeTemplateEntry.getValue().getType())))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      portNodeTemplates.put(ToscaUtil.getServiceTemplateFileName(serviceTemplate),
          serviceTemplatePortNodeTemplates);

      //Get all linked internal ports from abstract node template link requirements
      collectLinkedInternalPorts(nodeTemplates);
    }
  }

  private void collectLinkedInternalPorts(Map<String, NodeTemplate> nodeTemplates) {
    List<String> abstractLinkedPortNodeTemplates = new ArrayList<>();
    for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodeTemplates.entrySet()) {
      NodeTemplate nodeTemplate = nodeTemplateEntry.getValue();
      if (isSubstitutableNodeTemplate(nodeTemplate)) {
        List<Map<String, RequirementAssignment>> requirements = nodeTemplate.getRequirements();
        if (Objects.isNull(requirements)) {
          continue;
        }

        requirements.forEach(requirement -> addInternalPortToAbstractNode(requirement,
            abstractLinkedPortNodeTemplates));

        if (CollectionUtils.isNotEmpty(abstractLinkedPortNodeTemplates)) {
          //Populate a map of the substitution service templates and list of internal ports
          addCollectedPortsToAbstractServiceTemplatePortMap(nodeTemplate,
              abstractLinkedPortNodeTemplates);
        }
      }
    }
  }

  private void addInternalPortToAbstractNode(Map<String, RequirementAssignment> requirement,
                                             List<String> abstractLinkedPortNodeTemplates) {
    String requirementId = requirement.keySet().iterator().next();
    String abstractLinkRequirementIdPrefix = ToscaConstants.LINK_REQUIREMENT_ID + "_";
    if (requirementId.startsWith(abstractLinkRequirementIdPrefix)) {
      //Collect port node template ids from the link requirement ids in the abstract
      // node template
      abstractLinkedPortNodeTemplates.add(requirementId.substring(requirementId
          .indexOf("_") + 1));
    }
  }

  private void addCollectedPortsToAbstractServiceTemplatePortMap(NodeTemplate nodeTemplate,
                                              List<String> abstractLinkedPortNodeTemplates) {
    String substitutionServiceTemplateName;
    if (nodeTemplate.getProperties() != null) {
      Map serviceTemplateFilter = (Map<String, Object>) nodeTemplate.getProperties()
          .get(ToscaConstants.SERVICE_TEMPLATE_FILTER_PROPERTY_NAME);
      substitutionServiceTemplateName = (String)
          serviceTemplateFilter.get(ToscaConstants.SUBSTITUTE_SERVICE_TEMPLATE_PROPERTY_NAME);
      if (Objects.nonNull(substitutionServiceTemplateName)) {
        if (portNodeTemplateIdsFromAbstract.containsKey(substitutionServiceTemplateName)) {
          List<String> portList =
              portNodeTemplateIdsFromAbstract.get(substitutionServiceTemplateName);
          portList.addAll(abstractLinkedPortNodeTemplates);
          portNodeTemplateIdsFromAbstract.put(substitutionServiceTemplateName, portList);
        } else {
          portNodeTemplateIdsFromAbstract.put(substitutionServiceTemplateName,
              abstractLinkedPortNodeTemplates);
        }
      }
    }
  }

  private void filterExternalPorts() {
    for (Map.Entry<String, Map<String, NodeTemplate>> portNodeTemplateEntry : portNodeTemplates
        .entrySet()) {
      Map<String, NodeTemplate> externalPorts = new HashMap<>();
      String serviceTemplateFileName = portNodeTemplateEntry.getKey();
      Map<String, NodeTemplate> portNodeTemplateMap = portNodeTemplateEntry.getValue();
      for (Map.Entry<String, NodeTemplate> portNodeTemplate : portNodeTemplateMap.entrySet()) {
        String nodeTemplateId = portNodeTemplate.getKey();
        NodeTemplate nodeTemplate = portNodeTemplate.getValue();
        if (!isInternalPort(serviceTemplateFileName, nodeTemplateId, nodeTemplate)) {
          //External Port
          externalPorts.putIfAbsent(nodeTemplateId, nodeTemplate);
        }
      }
      externalPortNodeTemplates.putIfAbsent(serviceTemplateFileName, externalPorts);
    }
  }

  private void updateExternalPortNodeTemplate(NodeTemplate externalPortNodeTemplate) {
    String currentPortNodeType = externalPortNodeTemplate.getType();
    if (currentPortNodeType.equals(ToscaNodeType.CONTRAIL_PORT)) {
      //Set external contrail port node type
      externalPortNodeTemplate.setType(ToscaNodeType.EXTERNAL_CONTRAIL_PORT);
    } else if (currentPortNodeType.equals(ToscaNodeType.CONTRAILV2_VIRTUAL_MACHINE_INTERFACE)) {
      //Set external contrail VMI port node type
      externalPortNodeTemplate.setType(ToscaNodeType.EXTERNAL_VMI_PORT);
    } else if (currentPortNodeType.equals(ToscaNodeType.NEUTRON_PORT)) {
      //Set external neutron port node type
      externalPortNodeTemplate.setType(ToscaNodeType.EXTERNAL_NEUTRON_PORT);
    }

    addPortMirroringCapability(externalPortNodeTemplate);
  }

  private void handleExternalPorts(ToscaServiceModel toscaServiceModel) {

    for (Map.Entry<String, Map<String, NodeTemplate>> entry : externalPortNodeTemplates
        .entrySet()) {
      String serviceTemplateName = entry.getKey();
      ServiceTemplate serviceTemplate =
          toscaServiceModel.getServiceTemplates().get(serviceTemplateName);
      Map<String, NodeTemplate> externalNodeTemplates = entry.getValue();
      if (MapUtils.isEmpty(externalNodeTemplates)) {
        continue;
      }

      for (Map.Entry<String, NodeTemplate> externalNodeTemplate : externalNodeTemplates
          .entrySet()) {
        String externalPortNodeTemplateId = externalNodeTemplate.getKey();
        updateExternalPortNodeTemplate(externalNodeTemplate.getValue());
        if (serviceTemplate.getTopology_template().getSubstitution_mappings() != null) {
          //Add port mirroring capability to substitution mapping for external ports
          addPortMirroringSubstitutionMappingCapability(serviceTemplate,
              externalPortNodeTemplateId);
        }
      }
      addGlobalTypeImport(serviceTemplate);
    }
  }

  private void addPortMirroringSubstitutionMappingCapability(ServiceTemplate serviceTemplate,
                                                             String externalPortNodeTemplateId) {
    List<String> portMirroringCapability = new LinkedList<>();
    portMirroringCapability.add(externalPortNodeTemplateId);
    portMirroringCapability.add(PORT_MIRRORING_CAPABILITY_ID);
    String substitutionMappingCapabilityId = PORT_MIRRORING_CAPABILITY_ID + "_"
        + externalPortNodeTemplateId;
    DataModelUtil.addSubstitutionMappingCapability(serviceTemplate,
        substitutionMappingCapabilityId, portMirroringCapability);
  }

  private void addPortMirroringCapability(NodeTemplate portNodeTemplate) {
    Map<String, Object> portMirroringCapabilityProperties = new HashMap<>();
    PortMirroringConnectionPointDescription connectionPoint = new
        PortMirroringConnectionPointDescription();
    //Get Network role property
    if (Objects.nonNull(portNodeTemplate.getProperties())) {
      Object networkRolePropertyValue =
          portNodeTemplate.getProperties().get(ToscaConstants.PORT_NETWORK_ROLE_PROPERTY_NAME);
      if (Objects.nonNull(networkRolePropertyValue)) {
        Object portMirroringNetworkRolePropertyVal = getClonedObject(networkRolePropertyValue);
        connectionPoint.setNetwork_role(portMirroringNetworkRolePropertyVal);
      }
    }
    //Get NFC_Type from the binding requirement node
    if (Objects.nonNull(portNodeTemplate.getRequirements())) {
      Optional<List<RequirementAssignment>> requirementAssignment =
          DataModelUtil.getRequirementAssignment(portNodeTemplate.getRequirements(), ToscaConstants
              .BINDING_REQUIREMENT_ID);
      if (requirementAssignment.isPresent()) {
        RequirementAssignment bindingRequirementAssignment = requirementAssignment.get().get(0);
        String node = bindingRequirementAssignment.getNode();
        connectionPoint.setNfc_type(node);
      }
    }

    if (!connectionPoint.isEmpty()) {
      portMirroringCapabilityProperties.put(PORT_MIRRORING_CAPABILITY_CP_PROPERTY_NAME,
          connectionPoint);
      DataModelUtil.addNodeTemplateCapability(portNodeTemplate,
          PORT_MIRRORING_CAPABILITY_ID, portMirroringCapabilityProperties, null);
    }
  }

  private void addGlobalTypeImport(ServiceTemplate serviceTemplate) {
    List<Map<String, Import>> imports = serviceTemplate.getImports();
    Map<String, Import> openecompIndexImport = new HashMap<>();
    openecompIndexImport.put("openecomp_index",
        HeatToToscaUtil.createServiceTemplateImport(globalTypesServiceTemplate
            .get("openecomp/_index.yml")));
    imports.add(openecompIndexImport);
  }

  private boolean isPortNodeTemplate(String nodeType) {
    //Check if node corresponds to a concrete port node
    Set<String> portNodeTypes = getPortNodeTypes();
    return Objects.nonNull(nodeType)
        && portNodeTypes.contains(nodeType);
  }

  private Set<String> getPortNodeTypes() {
    return new HashSet<>(Arrays.asList(ToscaNodeType.NEUTRON_PORT,
        ToscaNodeType.CONTRAILV2_VIRTUAL_MACHINE_INTERFACE,
        ToscaNodeType.CONTRAIL_PORT));
  }

  private boolean isSubstitutableNodeTemplate(NodeTemplate nodeTemplate) {
    return Objects.nonNull(nodeTemplate.getDirectives())
        && nodeTemplate.getDirectives()
            .contains(ToscaConstants.NODE_TEMPLATE_DIRECTIVE_SUBSTITUTABLE);
  }

  private boolean isInternalPort(String serviceTemplateFileName, String nodeTemplateId,
                                 NodeTemplate nodeTemplate) {
    return isAbstractInternalPort(serviceTemplateFileName, nodeTemplateId)
        || isConcreteInternalPort(nodeTemplate);
  }

  private boolean isAbstractInternalPort(String serviceTemplateFileName, String nodeTemplateId) {
    //Check if port corresponds to an abstract internal port
    return portNodeTemplateIdsFromAbstract.containsKey(serviceTemplateFileName)
        && portNodeTemplateIdsFromAbstract.get(serviceTemplateFileName).contains(nodeTemplateId);
  }


  private boolean isConcreteInternalPort(NodeTemplate nodeTemplate) {
    //Check if node template contains a link requirement
    List<Map<String, RequirementAssignment>> requirements = nodeTemplate.getRequirements();
    if (Objects.nonNull(requirements)) {
      for (Map<String, RequirementAssignment> requirement : requirements) {
        String requirementId = requirement.keySet().iterator().next();
        if (requirementId.equals(ToscaConstants.LINK_REQUIREMENT_ID)) {
          return true;
        }
      }
    }
    return false;
  }
}
