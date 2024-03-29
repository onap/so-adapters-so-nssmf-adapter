package org.onap.so.adapters.nssmf.controller;

import org.onap.so.adapters.nssmf.annotation.RequestLogger;
import org.onap.so.adapters.nssmf.service.NssmfManagerService;
import org.onap.so.beans.nsmf.NssmfAdapterNBIRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
public class NssmfAdapterController {

    @Autowired
    private NssmfManagerService nssmfManagerService;

    @PostMapping(value = "/NSS/SliceProfiles")
    public ResponseEntity allocateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest) {
        return nssmfManagerService.allocateNssi(nbiRequest);
    }

    @PostMapping(value = "/NSS/SliceProfiles/{sliceProfileId}")
    public ResponseEntity deAllocateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("sliceProfileId") final String sliceProfileId) {
        return nssmfManagerService.deAllocateNssi(nbiRequest, sliceProfileId);
    }


    @PostMapping(value = "/NSS/{snssai}/activation")
    public ResponseEntity activateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("snssai") String snssai) {
        return nssmfManagerService.activateNssi(nbiRequest, snssai);
    }

    @PostMapping(value = "/NSS/{snssai}/deactivation")
    public ResponseEntity deactivateNssi(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("snssai") String snssai) {
        return nssmfManagerService.deActivateNssi(nbiRequest, snssai);
    }

    @PostMapping(value = "/NSS/jobs/{jobId}")
    public ResponseEntity queryJobStatus(@RequestBody NssmfAdapterNBIRequest nbiRequest,
            @PathVariable("jobId") String jobId) {
        return nssmfManagerService.queryJobStatus(nbiRequest, jobId);
    }

    @PostMapping(value = "/NSS/NSSISelectionCapability")
    public ResponseEntity queryNSSISelectionCapability(@RequestBody NssmfAdapterNBIRequest nbiRequest) {
        return nssmfManagerService.queryNSSISelectionCapability(nbiRequest);
    }

    @PostMapping(value = "/NSS/subnetCapabilityQuery")
    public ResponseEntity querySubnetCapability(@RequestBody NssmfAdapterNBIRequest nbiRequest) {
        return nssmfManagerService.querySubnetCapability(nbiRequest);
    }

}
