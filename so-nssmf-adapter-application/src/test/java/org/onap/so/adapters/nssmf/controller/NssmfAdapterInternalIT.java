/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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
package org.onap.so.adapters.nssmf.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.so.adapters.nssmf.extclients.aai.AaiServiceProvider;
import org.onap.so.beans.nsmf.ActDeActNssi;
import org.onap.so.beans.nsmf.AllocateCnNssi;
import org.onap.so.beans.nsmf.CnSliceProfile;
import org.onap.so.beans.nsmf.DeAllocateNssi;
import org.onap.so.beans.nsmf.EsrInfo;
import org.onap.so.beans.nsmf.NetworkType;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.onap.so.beans.nsmf.QuerySubnetCapability;
import org.onap.so.beans.nsmf.ServiceInfo;
import org.onap.so.db.request.beans.ResourceOperationStatus;
import org.onap.so.db.request.data.repository.ResourceOperationStatusRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
        }
)
@ActiveProfiles("it")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
public class NssmfAdapterInternalIT {

    private static final WireMockServer infraWireMock;

    static {
        infraWireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        infraWireMock.start();
    }

    @DynamicPropertySource
    static void overrideInfraEndpoint(DynamicPropertyRegistry registry) {
        registry.add("mso.infra.endpoint", () -> "http://localhost:" + infraWireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (infraWireMock != null && infraWireMock.isRunning()) {
            infraWireMock.stop();
        }
    }

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate;

    @MockBean
    private AaiServiceProvider aaiServiceProvider;

    private final ResourceOperationStatusRepository repository;

    @BeforeEach
    public void setup() {
        infraWireMock.resetAll();
        setupInfraMocks();
    }

    @AfterEach
    public void cleanup() {
        repository.deleteAll();
    }

    private void setupInfraMocks() {
        infraWireMock.stubFor(WireMock.post(urlPathMatching(".*/3gppservices/.*/allocate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nssiId\":\"NSSI-C-001-HDBNJ-NSSMF-01-A-ZX\","
                                + "\"jobId\":\"internal-job-id-001\"}")
                        .withStatus(200)));

        infraWireMock.stubFor(WireMock.delete(urlPathMatching(".*/3gppservices/.*/deAllocate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":\"internal-job-id-001\"}")
                        .withStatus(202)));

        infraWireMock.stubFor(WireMock.post(urlPathMatching(".*/3gppservices/.*/activate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":\"internal-job-id-001\"}")
                        .withStatus(200)));

        infraWireMock.stubFor(WireMock.post(urlPathMatching(".*/3gppservices/.*/deActivate"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":\"internal-job-id-001\"}")
                        .withStatus(200)));

        infraWireMock.stubFor(WireMock.get(urlPathMatching(".*/3gppservices/.*/subnetCapabilityQuery"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"subnetTypes\":[\"CN\"]}")
                        .withStatus(200)));
    }

    @Test
    public void allocateCnNssi_viaInternalNssmf_shouldForwardToInfraAndReturnResponse() {
        NssmfAdapterNBIRequest request = createInternalAllocateRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/SliceProfiles", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("nssiId");
        assertThat(response.getBody()).contains("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        assertThat(response.getBody()).contains("jobId");
        assertThat(response.getBody()).contains("internal-job-id-001");

        infraWireMock.verify(postRequestedFor(urlPathMatching(".*/3gppservices/.*/allocate")));
    }

    @Test
    public void deAllocateCnNssi_viaInternalNssmf_shouldForwardToInfraDeleteEndpoint() {
        NssmfAdapterNBIRequest request = createInternalDeAllocateRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/SliceProfiles/ab9af40f13f721b5f13539d87484098", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).contains("jobId");

        infraWireMock.verify(deleteRequestedFor(urlPathMatching(".*/3gppservices/.*/deAllocate")));
    }

    @Test
    public void activateCnNssi_viaInternalNssmf_shouldForwardToInfraActivateEndpoint() {
        NssmfAdapterNBIRequest request = createInternalActDeActRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/001-100001/activation", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jobId");

        infraWireMock.verify(postRequestedFor(urlPathMatching(".*/3gppservices/.*/activate")));
    }

    @Test
    public void deactivateCnNssi_viaInternalNssmf_shouldForwardToInfraDeactivateEndpoint() {
        NssmfAdapterNBIRequest request = createInternalActDeActRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/001-100001/deactivation", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jobId");

        infraWireMock.verify(postRequestedFor(urlPathMatching(".*/3gppservices/.*/deActivate")));
    }

    @Test
    public void queryNssiSelectionCapability_viaInternalNssmf_shouldReturnNssmfWithoutHttpCall() {
        NssmfAdapterNBIRequest request = createBaseInternalRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/NSSISelectionCapability", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("selection");
        assertThat(response.getBody()).contains("NSSMF");

        infraWireMock.verify(0, postRequestedFor(urlPathMatching(".*")));
    }

    @Test
    public void queryJobStatus_viaInternalNssmf_shouldReadStatusFromDb() {
        ResourceOperationStatus seededStatus = new ResourceOperationStatus();
        seededStatus.setOperationId("internal-job-id-001");
        seededStatus.setServiceId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        seededStatus.setResourceTemplateUUID("8ee5926d-720b-4bb2-86f9-d20e921c143b");
        seededStatus.setOperType("ALLOCATE");
        seededStatus.setStatus("processing");
        seededStatus.setProgress("60");
        seededStatus.setStatusDescription("Slice instance creation in progress");
        repository.save(seededStatus);

        NssmfAdapterNBIRequest request = createBaseInternalRequest();
        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/jobs/internal-job-id-001", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("responseDescriptor");
        assertThat(response.getBody()).contains("60");
        assertThat(response.getBody()).contains("processing");

        infraWireMock.verify(0, postRequestedFor(urlPathMatching(".*")));
        infraWireMock.verify(0, WireMock.getRequestedFor(urlPathMatching(".*")));
    }

    @Test
    public void querySubnetCapability_viaInternalNssmf_shouldForwardToInfraSubnetEndpoint() {
        NssmfAdapterNBIRequest request = createBaseInternalRequest();
        QuerySubnetCapability subnetCapabilityQuery = new QuerySubnetCapability();
        subnetCapabilityQuery.setSubnetTypes(Arrays.asList("CN"));
        request.setSubnetCapabilityQuery(subnetCapabilityQuery);

        HttpEntity<NssmfAdapterNBIRequest> entity = createHttpEntity(request);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/rest/provMns/v1/NSS/subnetCapabilityQuery", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        infraWireMock.verify(WireMock.getRequestedFor(urlPathMatching(".*/subnetCapabilityQuery")));
    }

    private NssmfAdapterNBIRequest createBaseInternalRequest() {
        NssmfAdapterNBIRequest request = new NssmfAdapterNBIRequest();

        EsrInfo esrInfo = new EsrInfo();
        esrInfo.setVendor("ONAP_internal");
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

    private NssmfAdapterNBIRequest createInternalAllocateRequest() {
        NssmfAdapterNBIRequest request = createBaseInternalRequest();

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

        AllocateCnNssi allocateCnNssi = new AllocateCnNssi();
        allocateCnNssi.setNssiName("eMBB-001-internal");
        allocateCnNssi.setScriptName("CN1");
        allocateCnNssi.setSliceProfile(sliceProfile);

        request.setAllocateCnNssi(allocateCnNssi);
        return request;
    }

    private NssmfAdapterNBIRequest createInternalDeAllocateRequest() {
        NssmfAdapterNBIRequest request = createBaseInternalRequest();

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

    private NssmfAdapterNBIRequest createInternalActDeActRequest() {
        NssmfAdapterNBIRequest request = createBaseInternalRequest();

        ActDeActNssi actDeActNssi = new ActDeActNssi();
        actDeActNssi.setNsiId("NSI-M-001-HDBNJ-NSMF-01-A-ZX");
        actDeActNssi.setNssiId("NSSI-C-001-HDBNJ-NSSMF-01-A-ZX");
        request.setActDeActNssi(actDeActNssi);

        return request;
    }

    private HttpEntity<NssmfAdapterNBIRequest> createHttpEntity(NssmfAdapterNBIRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }
}
