/*
 * Copyright 2012-2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.quickstart.service;

import com.marklogic.hub.flow.FlowType;
import com.marklogic.hub.scaffold.Scaffolding;
import com.marklogic.quickstart.auth.ConnectionAuthenticationToken;
import com.marklogic.quickstart.exception.NotFoundException;
import com.marklogic.quickstart.model.EntityModel;
import com.marklogic.quickstart.model.EnvironmentConfig;
import com.marklogic.quickstart.model.FlowModel;
import com.marklogic.quickstart.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class EntityManagerService {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EntityManagerService.class);

    @Autowired
    private FlowManagerService flowManagerService;

    private EnvironmentConfig envConfig() {
        ConnectionAuthenticationToken authenticationToken = (ConnectionAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return authenticationToken.getEnvironmentConfig();
    }

    public List<EntityModel> getEntities(String projectDir) {
        List<EntityModel> entities = new ArrayList<EntityModel>();
        Path entitiesPath = Paths.get(projectDir, "plugins", "entities");
        List<String> entityNames = FileUtil.listDirectFolders(entitiesPath.toFile());
        for (String entityName : entityNames) {
            LOGGER.debug("Entity : " + entityName);
            EntityModel entity = getEntity(projectDir, entityName);
            if (entity != null) {
                entities.add(entity);
            }
        }

        return entities;
    }

    public EntityModel getEntity(String projectDir, String entityName) {
        EntityModel entityModel = null;

        Path entityPath = Paths.get(projectDir, "plugins", "entities", entityName);
        File entityFile = entityPath.toFile();
        if (entityFile.exists() && entityFile.isDirectory()) {

            entityModel = new EntityModel(entityName);
            entityModel.inputFlows = flowManagerService.getFlows(projectDir, entityName, FlowType.INPUT);
            entityModel.harmonizeFlows = flowManagerService.getFlows(projectDir, entityName, FlowType.HARMONIZE);
        }

        if (entityModel == null) {
            throw new NotFoundException();
        }

        return entityModel;
    }

    public FlowModel getFlow(String projectDir, String entityName, FlowType flowType, String flowName) {
        EntityModel entity = getEntity(projectDir, entityName);

        List<FlowModel> flows;
        if (flowType.equals(FlowType.INPUT)) {
            flows = entity.inputFlows;
        }
        else {
            flows = entity.harmonizeFlows;
        }

        for (FlowModel flow : flows) {
            if (flow.flowName.equals(flowName)) {
                return flow;
            }
        }

        return null;
    }


    public EntityModel createEntity(String projectDir, EntityModel newEntity) throws IOException {
        Scaffolding scaffolding = new Scaffolding(projectDir);

        scaffolding.createEntity(newEntity.entityName);

        for (FlowModel flow : newEntity.inputFlows) {
            scaffolding.createFlow(newEntity.entityName, flow.flowName, FlowType.INPUT, flow.pluginFormat, flow.dataFormat);
        }

        for (FlowModel flow : newEntity.harmonizeFlows) {
            scaffolding.createFlow(newEntity.entityName, flow.flowName, FlowType.HARMONIZE, flow.pluginFormat, flow.dataFormat);
        }

        return getEntity(projectDir, newEntity.entityName);
    }

    public FlowModel createFlow(String projectDir, EntityModel entity, FlowType flowType, FlowModel newFlow) throws IOException {
        Scaffolding scaffolding = new Scaffolding(projectDir);
        newFlow.entityName = entity.entityName;
        LOGGER.info("data format: " + newFlow.dataFormat);
        scaffolding.createFlow(entity.entityName, newFlow.flowName, flowType, newFlow.pluginFormat, newFlow.dataFormat);
        return getFlow(projectDir, entity.entityName, flowType, newFlow.flowName);
    }
}
