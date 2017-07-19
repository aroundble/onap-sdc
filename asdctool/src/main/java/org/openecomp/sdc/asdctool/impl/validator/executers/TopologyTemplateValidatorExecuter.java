package org.openecomp.sdc.asdctool.impl.validator.executers;

import fj.data.Either;
import org.openecomp.sdc.asdctool.impl.validator.tasks.TopologyTemplateValidationTask;
import org.openecomp.sdc.asdctool.impl.validator.utils.ReportManager;
import org.openecomp.sdc.be.dao.jsongraph.GraphVertex;
import org.openecomp.sdc.be.dao.jsongraph.TitanDao;
import org.openecomp.sdc.be.dao.jsongraph.types.VertexTypeEnum;
import org.openecomp.sdc.be.dao.titan.TitanOperationStatus;
import org.openecomp.sdc.be.datatypes.enums.ComponentTypeEnum;
import org.openecomp.sdc.be.datatypes.enums.GraphPropertyEnum;
import org.openecomp.sdc.be.datatypes.enums.ResourceTypeEnum;
import org.openecomp.sdc.be.model.jsontitan.operations.TopologyTemplateOperation;
import org.openecomp.sdc.be.model.jsontitan.operations.ToscaOperationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by chaya on 7/3/2017.
 */
public class TopologyTemplateValidatorExecuter {

    private static Logger log = LoggerFactory.getLogger(VfValidatorExecuter.class.getName());

    @Autowired
    protected TitanDao titanDao;

    @Autowired
    protected ToscaOperationFacade toscaOperationFacade;

    @Autowired
    protected TopologyTemplateOperation topologyTemplateOperation;

    protected String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void reportValidateTaskStatus(TopologyTemplateValidationTask validationTask, boolean success, GraphVertex vertexScanned) {
        ReportManager.reportValidationTaskStatus(vertexScanned, validationTask.getTaskName(), validationTask.getTaskResultStatus(), success);
    }

    protected List<GraphVertex> getVerticesToValidate(ComponentTypeEnum type) {
        Map<GraphPropertyEnum, Object> props = new EnumMap<>(GraphPropertyEnum.class);
        props.put(GraphPropertyEnum.COMPONENT_TYPE, type.name());
        if(type.equals(ComponentTypeEnum.RESOURCE)) {
            props.put(GraphPropertyEnum.RESOURCE_TYPE, ResourceTypeEnum.VF);
        }

        Either<List<GraphVertex>, TitanOperationStatus> results = titanDao.getByCriteria(VertexTypeEnum.TOPOLOGY_TEMPLATE, props);
        if (results.isRight()) {
            System.out.println("getVerticesToValidate failed "+ results.right().value());
            return new ArrayList<GraphVertex>();
        }
        System.out.println("getVerticesToValidate: "+results.left().value().size()+" vertices to scan");
        return results.left().value();
    }

    protected boolean validate(List<? extends TopologyTemplateValidationTask> tasks, List<GraphVertex> vertices) {
        ReportManager.reportStartValidatorRun(getName(), vertices.size());
        Set<String> failedTasks = new HashSet<>();
        Set<String> successTasks = new HashSet<>();
        boolean successAllVertices = true;
        for (GraphVertex vertex: vertices) {
            boolean successAllTasks = true;
            for (TopologyTemplateValidationTask task: tasks) {
                ReportManager.reportStartTaskRun(vertex, task.getTaskName());
                boolean success = task.validate(vertex);
                if (!success) {
                    failedTasks.add(task.getTaskName());
                    successAllVertices = false;
                    successAllTasks = false;
                } else {
                    successTasks.add(task.getTaskName());
                }
                reportValidateTaskStatus(task, success, vertex);
            }
            String componentScanStatus = successAllTasks? "success" : "failed";
            System.out.println("Topology Template "+vertex.getUniqueId()+" Validation finished with "+componentScanStatus);
        }
        ReportManager.reportValidatorTypeSummary(getName(), failedTasks, successTasks);
        return successAllVertices;
    }
}