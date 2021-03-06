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
/**
 * Created by obarda on 7/5/2016.
 */

'use strict';
import * as _ from "lodash";
import {ComponentType} from "../../utils/constants";
import {ComponentMetadata} from "../component-metadata";
import {PolicyMetadata} from "../policy-metadata";
import {GroupMetadata} from "../group-metadata";
import {RequirementsGroup} from "../requirement";
import {CapabilitiesGroup} from "../capability";

export enum LeftPaletteMetadataTypes {
    Component,
    Group,
    Policy
}

export class LeftPaletteComponent {
    uniqueId:string;
    type:string;
    displayName:string;
    version:string;
    mainCategory:string;
    subCategory:string;
    iconClass:string;
    componentSubType:string;
    searchFilterTerms:string;
    certifiedIconClass:string;
    icon:string;
    isRequirmentAndCapabilitiesLoaded:boolean;

    uuid:string;
    name:string;
    lifecycleState:string;
    allVersions:any;
    componentType:string;
    systemName:string;

    invariantUUID:string;

    capabilities:CapabilitiesGroup;
    requirements:RequirementsGroup;

    categoryType:LeftPaletteMetadataTypes;

    constructor(metadataType: LeftPaletteMetadataTypes, item: ComponentMetadata | PolicyMetadata) {
        if (metadataType === LeftPaletteMetadataTypes.Policy) {
            this.initPolicy(item as PolicyMetadata);
            return;
        }

        if (metadataType === LeftPaletteMetadataTypes.Group) {
            this.initGroup(item as GroupMetadata);
            return;
        }

        if (metadataType === LeftPaletteMetadataTypes.Component) {
            this.initComponent(item as ComponentMetadata);
            return;
        }
    }

    private initComponent(component:ComponentMetadata): void {
        this.categoryType = LeftPaletteMetadataTypes.Component;

        this.icon = component.icon;
        this.version = component.version;
        this.uniqueId = component.uniqueId;
        this.isRequirmentAndCapabilitiesLoaded = false;
        this.uuid = component.uuid;
        this.name = component.name;
        this.allVersions = component.allVersions;
        this.componentType = component.componentType;
        this.systemName = component.systemName;
        this.invariantUUID = component.invariantUUID;

        if (component.categories && component.categories[0] && component.categories[0].subcategories && component.categories[0].subcategories[0]) {
            this.mainCategory = component.categories[0].name;
            this.subCategory = component.categories[0].subcategories[0].name;
        } else {
            this.mainCategory = 'Generic';
            this.subCategory = 'Generic';
        }

        this.componentSubType = component.resourceType ? component.resourceType: 'SERVICE';

        this.initDisplayName(component.name);
        this.searchFilterTerms = (this.displayName + ' ' + component.description + ' ' + component.tags.join(' ')).toLowerCase() + ' ' + component.version;
        this.initIconSprite(component.icon);
        this.certifiedIconClass = component.lifecycleState != 'CERTIFIED' ? 'non-certified' : '';
        if (component.icon === 'vl' || component.icon === 'cp') {
            this.certifiedIconClass = this.certifiedIconClass + " " + 'smaller-icon';
        }
    }

    private initGroup(group:GroupMetadata): void {
        this.categoryType = LeftPaletteMetadataTypes.Group;

        this.uniqueId = group.uniqueId;
        this.displayName = group.type;
        this.mainCategory = "Groups";
        this.subCategory = "Groups";
        this.iconClass = "sprite-group-icons group";
        this.version = group.version;

        this.type = group.type;
        this.componentSubType = 'GROUP';

        this.searchFilterTerms = this.displayName + ' ' + group.description + ' ' + group.version;
    }

    private initPolicy(policy:PolicyMetadata): void {
        this.categoryType = LeftPaletteMetadataTypes.Policy;

        this.uniqueId = policy.uniqueId;
        this.displayName = policy.type;
        this.mainCategory = "Policies";
        this.subCategory = "Policies";
        this.iconClass = "sprite-policy-icons policy";
        this.version = policy.version;

        this.type = policy.type;
        this.componentSubType = 'POLICY';

        this.searchFilterTerms = this.displayName + ' ' + policy.description + ' ' + policy.version;
    }

    public initDisplayName = (name:string):void => {
        let newName =
            _.last(_.last(_.last(_.last(_.last(_.last(_.last(_.last(name.split('tosca.nodes.'))
                .split('network.')).split('relationships.')).split('org.openecomp.')).split('resource.nfv.'))
                .split('nodes.module.')).split('cp.')).split('vl.'));
        if (newName) {
            this.displayName = newName;
        } else {
            this.displayName = name;
        }
    };

    public initIconSprite = (icon:string):void => {
        switch (this.componentSubType) {
            case ComponentType.SERVICE:
                this.iconClass = "sprite-services-icons " + icon;
                break;
            default:
                this.iconClass = "sprite-resource-icons " + icon;
        }
    }

    public getComponentSubType = ():string => {
        return this.componentSubType;
    };
}
