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
package org.onap.so.adapters.nssmf.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.hibernate.jdbc.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.so.adapters.nssmf.entity.NssmfInfo;
import org.onap.so.adapters.nssmf.entity.RestResponse;
import org.onap.so.adapters.nssmf.enums.HttpMethod;
import org.onap.so.adapters.nssmf.exceptions.ApplicationException;
import org.onap.so.adapters.nssmf.extclients.aai.AaiServiceProvider;
import org.onap.so.beans.nsmf.EsrInfo;
import org.onap.so.beans.nsmf.ServiceInfo;
import org.springframework.test.context.junit4.SpringRunner;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.onap.so.beans.nsmf.NetworkType.CORE;

@RunWith(SpringRunner.class)
public class RestUtilTest {


    @Mock
    public HttpClient httpClient;
    @Mock
    private HttpResponse tokenResponse;

    @Mock
    private HttpEntity tokenEntity;

    private InputStream tokenStream;

    @Mock
    private StatusLine statusLine;

    @Mock
    private AaiServiceProvider aaiSvcProv;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Field nssmfManagerService = controller.getClass().getDeclaredField("nssmfManagerService");
        // nssmfManagerService.setAccessible(true);
        // nssmfManagerService.set(controller, this.nssmfManagerService);
    }

    private void commonMock() throws IOException, ApplicationException {

        when(tokenResponse.getEntity()).thenReturn(tokenEntity);
        when(tokenResponse.getStatusLine()).thenReturn(statusLine);

        when(tokenEntity.getContent()).thenReturn(tokenStream);

        when(httpClient.execute(any())).thenReturn(tokenResponse);

        // Mockito.doReturn(httpClient).when(restUtil).getHttpsClient();
    }

    @Test
    public void sendTest() throws Exception {
        String url = "http://127.0.0.1:8080";
        HttpMethod method = HttpMethod.PUT;
        String content = "body content";
        commonMock();

        RestUtil restUtil = new RestUtil();
        RestUtil util = Mockito.spy(restUtil);
        doReturn(httpClient).when(util).getHttpsClient();

        RestResponse restResponse = util.send(url, method, content, null);
        assertNotNull(restResponse);
    }

    @Test
    public void serviceInstanceOperationTest() throws NoSuchFieldException, IllegalAccessException {

        RestUtil restUtil = new RestUtil();

        Field aaiSvcProv = restUtil.getClass().getDeclaredField("aaiSvcProv");
        aaiSvcProv.setAccessible(true);
        aaiSvcProv.set(restUtil, this.aaiSvcProv);

        ServiceInstance instance = new ServiceInstance();
        ServiceInfo info = getServiceInfo();
        restUtil.createServiceInstance(instance, info);
        restUtil.getServiceInstance(info);
        restUtil.deleteServiceInstance(info);
    }

    @Test
    public void getNssmfHostTest() throws NoSuchFieldException, IllegalAccessException {
        RestUtil restUtil = new RestUtil();

        Field aaiSvcProv = restUtil.getClass().getDeclaredField("aaiSvcProv");
        aaiSvcProv.setAccessible(true);
        aaiSvcProv.set(restUtil, this.aaiSvcProv);
        try {
            restUtil.getNssmfHost(getEsrInfo());
        } catch (ApplicationException ex) {
            System.out.println(ex.getErrorMsg());
        }
    }


    private EsrInfo getEsrInfo() {
        EsrInfo esrInfo = new EsrInfo();
        esrInfo.setVendor("huawei");
        esrInfo.setNetworkType(CORE);
        return esrInfo;
    }

    private ServiceInfo getServiceInfo() {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceUuid("8ee5926d-720b-4bb2-86f9-d20e921c143b");
        serviceInfo.setServiceInvariantUuid("e75698d9-925a-4cdd-a6c0-edacbe6a0b51");
        serviceInfo.setGlobalSubscriberId("5GCustomer");
        serviceInfo.setServiceType("5G");
        serviceInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        return serviceInfo;
    }

}
