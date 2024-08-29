/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.converter;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.converter.stream.InputStreamCache;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class FhirResourceConverter {

    @Converter
    public static Bundle toBundle(InputStreamCache inputStreamCache, Exchange exchange) throws Exception {
        FhirContext ctx = FhirContext.forR4();
        String body = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, inputStreamCache);
        return (Bundle) ctx.newJsonParser().parseResource(Bundle.class, body);
    }
}
