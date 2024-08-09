/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.converter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class FhirResourceConverter {

    @Converter
    public static InputStream convertResourceToInputStream(DomainResource resource) {
        FhirContext ctx = FhirContext.forR4();
        String json = ctx.newJsonParser().encodeResourceToString(resource);
        return new ByteArrayInputStream(json.getBytes());
    }

    @Converter
    public static IBaseResource convertMethodOutcomeToIBaseResource(MethodOutcome outcome) {
        if (outcome.getResource() != null) {
            return (IBaseResource) outcome.getResource();
        } else {
            log.warn("The MethodOutcome does not contain a valid IBaseResource. Returning null.");
            return null;
        }
    }
}
