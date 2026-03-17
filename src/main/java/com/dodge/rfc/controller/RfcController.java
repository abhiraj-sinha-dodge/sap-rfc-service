package com.dodge.rfc.controller;

import com.dodge.rfc.model.RfcMetadata;
import com.dodge.rfc.model.RfcRequest;
import com.dodge.rfc.model.RfcResponse;
import com.dodge.rfc.service.RfcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rfc")
public class RfcController {
    private static final Logger log = LoggerFactory.getLogger(RfcController.class);

    private final RfcService rfcService;

    public RfcController(RfcService rfcService) {
        this.rfcService = rfcService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "timestamp", System.currentTimeMillis(),
            "status", "ok",
            "service", "rfc-service"
        ));
    }

    @PostMapping("/execute")
    public ResponseEntity<RfcResponse> execute(@RequestBody RfcRequest request) {
        log.info("Executing RFC: {} on destination: {}", request.getFunctionModule(), request.getDestination());

        RfcResponse response = rfcService.execute(request);

        if (response.isSuccess()) {
            log.info("RFC {} completed in {}ms, hasErrors: {}",
                request.getFunctionModule(), response.getDurationMs(), response.isHasErrors());
        } else {
            log.warn("RFC {} failed: {} ({})",
                request.getFunctionModule(), response.getError(), response.getErrorCode());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/metadata/{destination}/{functionModule}")
    public ResponseEntity<?> getMetadata(
            @PathVariable String destination,
            @PathVariable String functionModule) {
        log.info("Getting metadata for {} on destination {}", functionModule, destination);

        RfcMetadata metadata = rfcService.getMetadata(destination, functionModule);

        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(metadata);
    }
}
