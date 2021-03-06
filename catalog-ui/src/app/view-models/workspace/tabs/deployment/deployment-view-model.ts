/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

'use strict';
import {IWorkspaceViewModelScope} from "app/view-models/workspace/workspace-view-model";
import {ComponentFactory, MenuHandler, ChangeLifecycleStateHandler, ModalsHandler} from "app/utils";
import {LeftPaletteLoaderService, CacheService, SharingService} from "app/services";
import {Component, IAppMenu, Tab, ComponentInstance} from "app/models";
import {GRAPH_EVENTS} from "../../../../utils/constants";
import {ComponentGenericResponse} from "../../../../ng2/services/responses/component-generic-response";
import {EventListenerService} from "../../../../services/event-listener-service";
import {ComponentServiceNg2} from "../../../../ng2/services/component-services/component.service";

export interface IDeploymentViewModelScope extends IWorkspaceViewModelScope {

    currentComponent:Component;
    selectedComponent:Component;
    isLoading:boolean;
    sharingService:SharingService;
    sdcMenu:IAppMenu;
    version:string;
    isViewOnly:boolean;
    tabs:Array<Tab>;
    selectedTab: Tab;
    isComponentInstanceSelected():boolean;
    updateSelectedComponent():void
    openUpdateModal();
    deleteSelectedComponentInstance():void;
    onBackgroundClick():void;
    setSelectedInstance(componentInstance:ComponentInstance):void;
    printScreen():void;

}

export class DeploymentViewModel {

    static '$inject' = [
        '$scope',
        '$templateCache',
        'sdcMenu',
        'MenuHandler',
        '$state',
        'Sdc.Services.SharingService',
        '$filter',
        'Sdc.Services.CacheService',
        'ComponentFactory',
        'ChangeLifecycleStateHandler',
        'LeftPaletteLoaderService',
        'ModalsHandler',
        'EventListenerService',
        'ComponentServiceNg2'
    ];

    constructor(private $scope:IDeploymentViewModelScope,
                private $templateCache:ng.ITemplateCacheService,
                private sdcMenu:IAppMenu,
                private MenuHandler:MenuHandler,
                private $state:ng.ui.IStateService,
                private sharingService:SharingService,
                private $filter:ng.IFilterService,
                private cacheService:CacheService,
                private ComponentFactory:ComponentFactory,
                private ChangeLifecycleStateHandler:ChangeLifecycleStateHandler,
                private LeftPaletteLoaderService:LeftPaletteLoaderService,
                private ModalsHandler:ModalsHandler,
                private eventListenerService: EventListenerService,
                private ComponentServiceNg2: ComponentServiceNg2) {

        this.$scope.setValidState(true);
        this.initScope();
        this.initGraphData();
    }


    private initComponent = ():void => {

        this.$scope.currentComponent = this.$scope.component;
        this.$scope.selectedComponent = this.$scope.currentComponent;
        this.updateUuidMap();
        this.$scope.isViewOnly = this.$scope.isViewMode();
    };


    private updateUuidMap = ():void => {
        /**
         * In case user press F5, the page is refreshed and this.sharingService.currentEntity will be undefined,
         * but after loadService or loadResource this.sharingService.currentEntity will be defined.
         * Need to update the uuidMap with the new resource or service.
         */
        this.sharingService.addUuidValue(this.$scope.currentComponent.uniqueId, this.$scope.currentComponent.uuid);
    };

    private initRightTabs = ()=> {
        if (this.$scope.currentComponent.groups) {
            this.$templateCache.put("hierarchy-view.html", require('app/view-models/tabs/hierarchy/hierarchy-view.html'));
            let hierarchyTab = new Tab("hierarchy-view.html", 'Sdc.ViewModels.HierarchyViewModel', 'hierarchy', this.$scope.isViewMode(), this.$scope.currentComponent, 'hierarchy');
            this.$scope.tabs.push(hierarchyTab)
        }
    }

    private initGraphData = ():void => {
        if(!this.$scope.component.componentInstances || !this.$scope.component.componentInstancesRelations || !this.$scope.component.groups) {
            this.ComponentServiceNg2.getDeploymentGraphData(this.$scope.component).subscribe((response:ComponentGenericResponse) => {
                this.$scope.component.componentInstances = response.componentInstances;
                this.$scope.component.componentInstancesRelations = response.componentInstancesRelations;
                this.$scope.component.groups = response.groups;
                this.$scope.isLoading = false;
                this.initComponent();
                this.initRightTabs();
                this.eventListenerService.notifyObservers(GRAPH_EVENTS.ON_DEPLOYMENT_GRAPH_DATA_LOADED);
                this.$scope.selectedTab = this.$scope.tabs[0];
            });
        } else {
            this.$scope.isLoading = false;
            this.initRightTabs();
            this.eventListenerService.notifyObservers(GRAPH_EVENTS.ON_DEPLOYMENT_GRAPH_DATA_LOADED);

        }
    };

    private initScope = ():void => {
        this.$scope.isLoading = true;
        this.$scope.sharingService = this.sharingService;
        this.$scope.sdcMenu = this.sdcMenu;
        this.$scope.version = this.cacheService.get('version');
        this.initComponent();
        this.$scope.tabs = Array<Tab>();
    }
}
