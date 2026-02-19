package org.onap.so.adapters.nssmf.controller;

import lombok.RequiredArgsConstructor;
import org.onap.so.adapters.nssmf.annotation.RequestLogger;
import org.onap.so.adapters.nssmf.service.NssmfManagerService;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping(value = "/api/rest/provMns/v1", produces = {APPLICATION_JSON}, consumes = {APPLICATION_JSON})
@RequestLogger
@RequiredArgsConstructor
public class NssmfAdapterController {

    private final NssmfManagerService nssmfManagerService;

    @PostMapping(value = "/NSS/SliceProfiles")
    public ResponseEntity<String> allocateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest) {
        return nssmfManagerService.allocateNssi(nbiRequest);
    }

    @PostMapping(value = "/NSS/SliceProfiles/{sliceProfileId}")
    public ResponseEntity<String> deAllocateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("sliceProfileId") final String sliceProfileId) {
        return nssmfManagerService.deAllocateNssi(nbiRequest, sliceProfileId);
    }


    @PostMapping(value = "/NSS/{snssai}/activation")
    public ResponseEntity<String> activateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("snssai") String snssai) {
        return nssmfManagerService.activateNssi(nbiRequest, snssai);
    }

    @PostMapping(value = "/NSS/{snssai}/deactivation")
    public ResponseEntity<String> deactivateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("snssai") String snssai) {
        return nssmfManagerService.deActivateNssi(nbiRequest, snssai);
    }

    @PostMapping(value = "/NSS/jobs/{jobId}")
    public ResponseEntity<String> queryJobStatus(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("jobId") String jobId) {
        return nssmfManagerService.queryJobStatus(nbiRequest, jobId);
    }

    @PostMapping(value = "/NSS/NSSISelectionCapability")
    public ResponseEntity<String> queryNSSISelectionCapability(@RequestBody NssmfAdapterNBIRequest nbiRequest) {
        return nssmfManagerService.queryNSSISelectionCapability(nbiRequest);
    }

    @PostMapping(value = "/NSS/subnetCapabilityQuery")
    public ResponseEntity<String> querySubnetCapability(@RequestBody NssmfAdapterNBIRequest nbiRequest) {
        return nssmfManagerService.querySubnetCapability(nbiRequest);
    }

}
