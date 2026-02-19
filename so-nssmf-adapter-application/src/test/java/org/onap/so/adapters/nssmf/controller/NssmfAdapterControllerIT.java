/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (c) 2020, CMCC Technologies Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.so.adapters.nssmf.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.domain.yang.EsrSystemInfo;
import org.onap.aai.domain.yang.EsrSystemInfoList;
import org.onap.aai.domain.yang.EsrThirdpartySdnc;
import org.onap.aai.domain.yang.EsrThirdpartySdncList;
import org.onap.aai.domain.yang.ServiceInstance;
import org.onap.so.adapters.nssmf.extclients.aai.AaiServiceProvider;
import org.onap.so.beans.nsmf.ActDeActNssi;
import org.onap.so.beans.nsmf.AllocateCnNssi;
import org.onap.so.beans.nsmf.CnSliceProfile;
import org.onap.so.beans.nsmf.DeAllocateNssi;
import org.onap.so.beans.nsmf.EsrInfo;
import org.onap.so.beans.nsmf.NetworkType;
import org.onap.so.beans.nsmf.NsiInfo;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.onap.so.beans.nsmf.PerfReq;
import org.onap.so.beans.nsmf.PerfReqEmbb;
import org.onap.so.beans.nsmf.QuerySubnetCapability;
import org.onap.so.beans.nsmf.ResourceSharingLevel;
import org.onap.so.beans.nsmf.ServiceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
    }
)
@ActiveProfiles("it")
public class NssmfAdapterControllerIT {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private AaiServiceProvider aaiServiceProvider;

    @BeforeAll
    public static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .dynamicHttpsPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    public static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        wireMockServer.resetAll();
        setupAaiMocks();
        setupNssmfMocks();
    }

    private void setupAaiMocks() {
        EsrThirdpartySdncList sdncList = new EsrThirdpartySdncList();
        EsrThirdpartySdnc sdnc = new EsrThirdpartySdnc();
        sdnc.setThirdpartySdncId("nssmf-001");
        sdncList.getEsrThirdpartySdnc().add(sdnc);

        EsrSystemInfoList systemInfoList = new EsrSystemInfoList();
        EsrSystemInfo systemInfo = new EsrSystemInfo();
        systemInfo.setEsrSystemInfoId("nssmf-system-001");
        systemInfo.setType("cn");
        systemInfo.setVendor("huawei");
        systemInfo.setIpAddress("localhost");
        systemInfo.setPort(String.valueOf(wireMockServer.httpsPort()));
        systemInfo.setUserName("nssmf-user");
        systemInfo.setPassword("nssmf-pass");
        systemInfoList.getEsrSystemInfo().add(systemInfo);

        when(aaiServiceProvider.invokeGetThirdPartySdncList()).thenReturn(sdncList);
        when(aaiServiceProvider.invokeGetThirdPartySdncEsrSystemInfo(anyString())).thenReturn(systemInfoList);
        when(aaiServiceProvider.invokeGetServiceInstance(anyString(), anyString(), anyString()))
                .thenReturn(new ServiceInstance());
        doNothing().when(aaiServiceProvider).invokeCreateServiceInstance(any(), anyString(), anyString(), anyString());
        doNothing().when(aaiServiceProvider).invokeDeleteServiceInstance(anyString(), anyString(), anyString());
    }

    private void setupNssmfMocks() throws Exception {
        wireMockServer.stubFor(post(urlPathMatching(".*/oauth/token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("token-response.json")
                        .withStatus(200)));

        wireMockServer.stubFor(post(urlPathMatching(".*/SliceProfiles"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("allocate-nssi-response.json")
                        .withStatus(200)));

        wireMockServer.stubFor(delete(urlPathMatching(".*/SliceProfiles/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("deallocate-nssi-response.json")
                        .withStatus(202)));

        wireMockServer.stubFor(put(urlPathMatching(".*/activation"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("activate-nssi-response.json")
                        .withStatus(200)));

        wireMockServer.stubFor(put(urlPathMatching(".*/deactivation"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("deactivate-nssi-response.json")
                        .withStatus(200)));

        wireMockServer.stubFor(post(urlPathMatching(".*/jobs/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("query-job-status-response.json")
                        .withStatus(200)));
    }

    @Test
    public void allocateNssi_shouldReturnSuccessResponse() {
        NssmfAdapterNBIRequest request = createAllocateRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/SliceProfiles", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("nssiId");
        assertThat(response.getBody()).contains("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        assertThat(response.getBody()).contains("jobId");
        assertThat(response.getBody()).contains("4b45d919816ccaa2b762df5120f72067");

        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/oauth/token")));
        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/SliceProfiles")));
    }

    @Test
    public void deAllocateNssi_shouldReturnSuccessResponse() {
        NssmfAdapterNBIRequest request = createDeAllocateRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/SliceProfiles/ab9af40f13f721b5f13539d87484098", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).contains("jobId");
        assertThat(response.getBody()).contains("4b45d919816ccaa2b762df5120f72067");

        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/oauth/token")));
    }

    @Test
    public void activateNssi_shouldReturnSuccessResponse() {
        NssmfAdapterNBIRequest request = createActivateRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/001-100001/activation", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jobId");

        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/oauth/token")));
    }

    @Test
    public void deactivateNssi_shouldReturnSuccessResponse() {
        NssmfAdapterNBIRequest request = createDeactivateRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/001-100001/deactivation", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jobId");

        wireMockServer.verify(postRequestedFor(urlPathMatching(".*/oauth/token")));
    }

    @Test
    public void queryNSSISelectionCapability_shouldReturnCapability() {
        NssmfAdapterNBIRequest request = createBaseRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/NSSISelectionCapability", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    public void querySubnetCapability_shouldReturnCapability() {
        NssmfAdapterNBIRequest request = createSubnetCapabilityRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/subnetCapabilityQuery", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    private HttpEntity<NssmfAdapterNBIRequest> createHttpEntity(NssmfAdapterNBIRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }

    private NssmfAdapterNBIRequest createBaseRequest() {
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();
        EsrInfo esrInfo = new EsrInfo();
        esrInfo.setVendor("huawei");
        esrInfo.setNetworkType(NetworkType.CORE);
        request.setEsrInfo(esrInfo);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceUuid("8ee5926d-720b-4bb2-86f9-d20e921c143b");
        serviceInfo.setServiceInvariantUuid("e75698d9-925a-4cdd-a6c0-edacbe6a0b51");
        serviceInfo.setGlobalSubscriberId("5GCustomer");
        serviceInfo.setServiceType("5G");
        serviceInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        request.setServiceInfo(serviceInfo);

        return request;
    }

    private NssmfAdapterNBIRequest createAllocateRequest() {
        NssmfAdapterNBIRequest request = createBaseRequest();

        CnSliceProfile sliceProfile = new CnSliceProfile();
        sliceProfile.setSliceProfileId("ab9af40f13f721b5f13539d87484098");
        List<String> snssaiList = new LinkedList<>();
        snssaiList.add("001-100001");
        sliceProfile.setSnssaiList(snssaiList);
        List<String> plmnList = new LinkedList<>();
        plmnList.add("460-00");
        sliceProfile.setPLMNIdList(plmnList);
        sliceProfile.setMaxNumberOfUEs(200);
        sliceProfile.setLatency(6);
        sliceProfile.setResourceSharingLevel(ResourceSharingLevel.NON_SHARED);

        PerfReqEmbb embb = new PerfReqEmbb();
        embb.setActivityFactor(50);
        List<PerfReqEmbb> embbList = new LinkedList<>();
        embbList.add(embb);
        PerfReq perfReq = new PerfReq();
        perfReq.setPerfReqEmbbList(embbList);
        sliceProfile.setPerfReq(perfReq);

        List<String> taList = new LinkedList<>();
        taList.add("1");
        taList.add("2");
        sliceProfile.setCoverageAreaTAList(taList);

        NsiInfo nsiInfo = new NsiInfo();
        nsiInfo.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        nsiInfo.setNsiName("eMBB-001");

        AllocateCnNssi allocateCnNssi = new AllocateCnNssi();
        allocateCnNssi.setNssiId("NSST-C-001-HDBNJ-NSSMF-01-A-ZX");
        allocateCnNssi.setNssiName("eMBB-001");
        allocateCnNssi.setScriptName("CN1");
        allocateCnNssi.setSliceProfile(sliceProfile);
        allocateCnNssi.setNsiInfo(nsiInfo);

        request.setAllocateCnNssi(allocateCnNssi);
        return request;
    }

    private NssmfAdapterNBIRequest createDeAllocateRequest() {
        NssmfAdapterNBIRequest request = createBaseRequest();

        DeAllocateNssi deAllocateNssi = new DeAllocateNssi();
        deAllocateNssi.setTerminateNssiOption(0);
        deAllocateNssi.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        deAllocateNssi.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        deAllocateNssi.setScriptName("CN1");
        List<String> snssaiList = new LinkedList<>();
        snssaiList.add("001-100001");
        deAllocateNssi.setSnssaiList(snssaiList);

        request.setDeAllocateNssi(deAllocateNssi);
        return request;
    }

    private NssmfAdapterNBIRequest createActivateRequest() {
        NssmfAdapterNBIRequest request = createBaseRequest();
        request.getServiceInfo().setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        ActDeActNssi actDeActNssi = new ActDeActNssi();
        actDeActNssi.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        actDeActNssi.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        request.setActDeActNssi(actDeActNssi);

        return request;
    }

    private NssmfAdapterNBIRequest createDeactivateRequest() {
        NssmfAdapterNBIRequest request = createBaseRequest();
        request.getServiceInfo().setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");

        ActDeActNssi actDeActNssi = new ActDeActNssi();
        actDeActNssi.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        actDeActNssi.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        request.setActDeActNssi(actDeActNssi);

        return request;
    }

    private NssmfAdapterNBIRequest createSubnetCapabilityRequest() {
        NssmfAdapterNBIRequest request = createBaseRequest();

        QuerySubnetCapability subnetCapabilityQuery = new QuerySubnetCapability();
        subnetCapabilityQuery.setSubnetTypes(Arrays.asList("CN"));
        request.setSubnetCapabilityQuery(subnetCapabilityQuery);

        return request;
    }
}
