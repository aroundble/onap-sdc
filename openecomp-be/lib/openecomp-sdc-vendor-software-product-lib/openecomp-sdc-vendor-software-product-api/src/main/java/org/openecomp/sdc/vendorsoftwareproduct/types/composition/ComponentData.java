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

package org.openecomp.sdc.vendorsoftwareproduct.types.composition;

public class ComponentData implements CompositionDataEntity {
  private String name;
  private String description;
  private String displayName;
  private String vfcCode;
  private String nfcCode;
  private String nfcFunction;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getNfcCode() {
    return nfcCode;
  }

  public void setNfcCode(String nfcCode) {
    this.nfcCode = nfcCode;
  }

  public String getNfcFunction() {
    return nfcFunction;
  }

  public void setNfcFunction(String nfcFunction) {
    this.nfcFunction = nfcFunction;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    return result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof ComponentData)) {
      return false;
    }

    ComponentData that = (ComponentData) object;

    if (!name.equals(that.name)) {
      return false;
    }
    if (description != null ? !description.equals(that.description) : that.description != null) {
      return false;
    }
    return displayName != null ? displayName.equals(that.displayName) : that.displayName == null;

  }

  public String getVfcCode() {
    return vfcCode;
  }

  public void setVfcCode(String vfcCode) {
    this.vfcCode = vfcCode;
  }
}
