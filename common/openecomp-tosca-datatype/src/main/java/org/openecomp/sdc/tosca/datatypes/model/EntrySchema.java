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

package org.openecomp.sdc.tosca.datatypes.model;

import org.openecomp.sdc.tosca.services.DataModelCloneUtil;

import java.util.List;

public class EntrySchema {

  private String description;
  private String type;
  private List<Constraint> constraints;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<Constraint> getConstraints() {
    return constraints;
  }

  public void setConstraints(List<Constraint> constraints) {
    this.constraints = constraints;
  }

  @Override
  public EntrySchema clone() {
    EntrySchema entrySchema = new EntrySchema();
    entrySchema.setDescription(this.getDescription());
    entrySchema.setType(this.getType());
    entrySchema.setConstraints(DataModelCloneUtil.cloneConstraints(this.getConstraints()));
    return entrySchema;
  }
}
