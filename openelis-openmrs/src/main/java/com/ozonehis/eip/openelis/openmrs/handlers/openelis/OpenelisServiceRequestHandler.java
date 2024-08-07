/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisServiceRequestHandler {

    public ServiceRequest sendServiceRequest(ProducerTemplate producerTemplate, ServiceRequest serviceRequest) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequest.getIdPart());
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-service-request-route", serviceRequest, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        ServiceRequest savedServiceRequest = ctx.newJsonParser().parseResource(ServiceRequest.class, response);
        return savedServiceRequest;
    }

    public void deleteServiceRequest(ProducerTemplate producerTemplate, String serviceRequestID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequestID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-delete-service-request-route", null, headers, String.class);

        log.info("Openelis: deleteServiceRequest response {}", response);
    }
}
