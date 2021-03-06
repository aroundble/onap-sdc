/*
 * Copyright © 2016-2018 European Support Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openecomp.sdc.model.impl;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.UDTMapper;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.openecomp.core.model.dao.ServiceArtifactDao;
import org.openecomp.core.model.types.ServiceArtifact;
import org.openecomp.core.model.types.ServiceArtifactEntity;
import org.openecomp.core.nosqldb.api.NoSqlDb;
import org.openecomp.core.nosqldb.factory.NoSqlDbFactory;
import org.openecomp.sdc.versioning.ActionVersioningManagerFactory;
import org.openecomp.sdc.versioning.dao.types.Version;
import org.openecomp.sdc.versioning.types.VersionableEntityMetadata;

public class ServiceArtifactDaoCassandraImpl implements ServiceArtifactDao {

  private static final NoSqlDb noSqlDb = NoSqlDbFactory.getInstance().createInterface();
  private static final Mapper<ServiceArtifactEntity> mapper = noSqlDb.getMappingManager().mapper(
      ServiceArtifactEntity.class);
  private static final VspServiceArtifactAccessor accessor =
      noSqlDb.getMappingManager().createAccessor(
          VspServiceArtifactAccessor.class);
  private static final UDTMapper<Version> versionMapper =
      noSqlDb.getMappingManager().udtMapper(Version.class);

  @Override
  public void registerVersioning(String versionableEntityType) {
    ActionVersioningManagerFactory.getInstance().createInterface().register(versionableEntityType,
        new VersionableEntityMetadata(mapper.getTableMetadata().getName(),
            mapper.getTableMetadata().getPartitionKey().get(0).getName(),
            mapper.getTableMetadata().getPartitionKey().get(1).getName()));
  }

  @Override
  public Collection<ServiceArtifact> list(String vspId, Version version) {
    List<ServiceArtifactEntity> entityList;
    if (vspId != null && version != null) {
      entityList = accessor.list(vspId, versionMapper.toUDT(version)).all();
    } else {
      entityList = accessor.listAll().all();
    }

    return entityList.stream().map(entity -> entity.getServiceArtifact())
        .collect(Collectors.toList());
  }

  @Override
  public void create(ServiceArtifact entity) {
    ServiceArtifactEntity vspServiceArtifactEntity = new ServiceArtifactEntity(entity);
    mapper.save(vspServiceArtifactEntity);
  }

  @Override
  public void update(ServiceArtifact entity) {
    ServiceArtifactEntity vspServiceArtifactEntity = new ServiceArtifactEntity(entity);
    mapper.save(vspServiceArtifactEntity);
  }

  @Override
  public ServiceArtifact get(String vspId, Version version) {
    return mapper.get(getKeys(vspId, version)).getServiceArtifact();
  }

  @Override
  public void delete(String vspId, Version version) {
    accessor.delete(vspId, versionMapper.toUDT(version));
  }

  // @Override
  // public void deleteArtifacts(String vspId, Version version){
  // accessor.delete(vspId, versionMapper.toUDT(version));
  // }

  @Override
  public Object[] getKeys(String vspId, Version version) {
    return new Object[]{vspId, versionMapper.toUDT(version)};
  }

  @Override
  public ServiceArtifact getArtifactInfo(String vspId, Version version, String name) {
    ServiceArtifactEntity serviceArtifactEntity =
        accessor.getArtifactInfo(vspId, versionMapper.toUDT(version),
            name).one();
    if (serviceArtifactEntity == null) {
      return null;
    }

    return serviceArtifactEntity.getServiceArtifact();
  }

  @Override
  public void deleteAll(String vspId, Version version) {
    accessor.deleteAll(vspId, versionMapper.toUDT(version));
  }

  @Accessor
  interface VspServiceArtifactAccessor {

    @Query("SELECT vsp_id, version, name ,content_data FROM vsp_service_artifact")
    Result<ServiceArtifactEntity> listAll();

    @Query(
        "SELECT vsp_id, version, name ,content_data "
            + "FROM vsp_service_artifact where vsp_id=? and version=? ")
    Result<ServiceArtifactEntity> list(String vspId, UDTValue version);

    @Query(
        "SELECT vsp_id,version,name,content_data FROM"
            + " vsp_service_artifact where vsp_id=? and version=? and name=?")
    Result<ServiceArtifactEntity> getArtifactInfo(String vspId, UDTValue version, String name);

    @Query("DELETE from vsp_service_artifact where vsp_id=? and version=?")
    ResultSet delete(String vspId, UDTValue version);

    @Query("DELETE FROM vsp_service_artifact where vsp_id=? and version=?")
    ResultSet deleteAll(String vspId, UDTValue version);
  }

}
