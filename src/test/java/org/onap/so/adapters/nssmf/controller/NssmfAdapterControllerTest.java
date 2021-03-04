/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 # Copyright (c) 2020, CMCC Technologies Co., Ltd.
 #
 # Licensed under the Apache License, Version 2.0 (the "License")
 # you may not use this file except in compliance with the License.
 # You may obtain a copy of the License at
 #
 #       http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.so.adapters.nssmf.controller;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.onap.so.adapters.nssmf.controller.NssmfAdapterController;
import org.onap.so.adapters.nssmf.service.NssmfManagerService;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.onap.so.beans.nsmf.NetworkType.CORE;

@RunWith(SpringRunner.class)
public class NssmfAdapterControllerTest {

    @Mock
    private NssmfManagerService nssmfManagerService;

    private NssmfAdapterController controller;

    @Mock
    private ResponseEntity entity;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        controller = new NssmfAdapterController();

        Field nssmfManagerService = controller.getClass().getDeclaredField("nssmfManagerService");
        nssmfManagerService.setAccessible(true);
        nssmfManagerService.set(controller, this.nssmfManagerService);
    }


    @Test
    public void allocateNssiTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        ResponseEntity entity = controller.allocateNssi(request);
        assertNotNull(entity);
    }

    @Test
    public void deAllocateNssiTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        String profileId = "7516eb33-e8d3-4805-b91a-96de1bb6d8bb";
        ResponseEntity entity = controller.deAllocateNssi(request, profileId);
        assertNotNull(entity);

    }

    @Test
    public void activateNssiTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        String snssai = "01-772523CD";
        ResponseEntity entity = controller.activateNssi(request, snssai);
        assertNotNull(entity);
    }

    @Test
    public void deactivateNssiTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        String snssai = "01-772523CD";
        ResponseEntity entity = controller.deactivateNssi(request, snssai);
        assertNotNull(entity);
    }

    @Test
    public void queryJobStatusTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        String jobId = "b86aa515-b285-487b-8a1e-01f3cc871b88";
        ResponseEntity entity = controller.queryJobStatus(request, jobId);
        assertNotNull(entity);
    }

    @Test
    public void queryNSSISelectionCapabilityTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        ResponseEntity entity = controller.queryNSSISelectionCapability(request);
        assertNotNull(entity);
    }

    @Test
    public void querySubnetCapabilityTest() throws Exception {
        commonMock();
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        ResponseEntity entity = controller.querySubnetCapability(request);
        assertNotNull(entity);
    }

    private void commonMock() {
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        when(nssmfManagerService.allocateNssi(any())).thenReturn(entity);
        when(nssmfManagerService.deAllocateNssi(any(), anyString())).thenReturn(entity);
        when(nssmfManagerService.deActivateNssi(any(), anyString())).thenReturn(entity);
        when(nssmfManagerService.activateNssi(any(), anyString())).thenReturn(entity);
        when(nssmfManagerService.queryJobStatus(any(), anyString())).thenReturn(entity);
        when(nssmfManagerService.querySubnetCapability(any())).thenReturn(entity);
        when(nssmfManagerService.queryNSSISelectionCapability(any())).thenReturn(entity);
    }
}
