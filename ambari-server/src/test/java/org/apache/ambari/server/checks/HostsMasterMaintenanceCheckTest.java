/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.checks;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradeCheckResult;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

/**
 * Unit tests for HostsMasterMaintenanceCheck
 *
 */
@Ignore
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
@RunWith(MockitoJUnitRunner.class)
public class HostsMasterMaintenanceCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final RepositoryVersionHelper repositoryVersionHelper = Mockito.mock(RepositoryVersionHelper.class);
  private final AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_services.clear();

    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  @Test
  public void testIsApplicable() throws Exception {
    UpgradePlanEntity upgradePlan = Mockito.mock(UpgradePlanEntity.class);
    PrereqCheckRequest checkRequest = new PrereqCheckRequest(upgradePlan);
    HostsMasterMaintenanceCheck hmmc = new HostsMasterMaintenanceCheck();
    Configuration config = Mockito.mock(Configuration.class);
    hmmc.config = config;
    Assert.assertTrue(hmmc.isApplicable(checkRequest));
    Assert.assertTrue(new HostsMasterMaintenanceCheck().isApplicable(checkRequest));

    HostsMasterMaintenanceCheck hmmc2 = new HostsMasterMaintenanceCheck();
    hmmc2.config = config;
    Assert.assertTrue(hmmc2.isApplicable(checkRequest));
  }

  @Test
  public void testPerform() throws Exception {
    final String upgradePackName = "upgrade_pack";
    final HostsMasterMaintenanceCheck hostsMasterMaintenanceCheck = new HostsMasterMaintenanceCheck();
    hostsMasterMaintenanceCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsMasterMaintenanceCheck.repositoryVersionHelper = new Provider<RepositoryVersionHelper>() {
      @Override
      public RepositoryVersionHelper get() {
        return repositoryVersionHelper;
      }
    };
    hostsMasterMaintenanceCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ambariMetaInfo;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getDesiredStackVersion()).thenReturn(new StackId("HDP", "1.0"));

    UpgradePlanEntity upgradePlan = Mockito.mock(UpgradePlanEntity.class);
    PrereqCheckRequest checkRequest = new PrereqCheckRequest(upgradePlan);

    UpgradeCheckResult result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());

    Mockito.when(ambariMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenReturn(new HashMap<>());

    checkRequest = new PrereqCheckRequest(upgradePlan);
    result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(PrereqCheckStatus.FAIL, result.getStatus());

    final Map<String, UpgradePack> upgradePacks = new HashMap<>();
    final UpgradePack upgradePack = Mockito.mock(UpgradePack.class);
    Mockito.when(upgradePack.getName()).thenReturn(upgradePackName);
    upgradePacks.put(upgradePack.getName(), upgradePack);
    Mockito.when(ambariMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenReturn(upgradePacks);
    Mockito.when(upgradePack.getTasks()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getServicesByName()).thenReturn(new HashMap<>());
    Mockito.when(clusters.getHostsForCluster(Mockito.anyString())).thenReturn(new HashMap<>());

    checkRequest = new PrereqCheckRequest(upgradePlan);
    result = hostsMasterMaintenanceCheck.perform(checkRequest);

    hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(PrereqCheckStatus.PASS, result.getStatus());
  }
}
