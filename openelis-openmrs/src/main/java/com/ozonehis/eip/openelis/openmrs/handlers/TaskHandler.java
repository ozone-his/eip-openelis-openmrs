/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class TaskHandler {

    public Task sendTask(ProducerTemplate producerTemplate, Task task) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_TASK_ID, task.getIdPart());
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-task-route", task, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        Task savedTask = ctx.newJsonParser().parseResource(Task.class, response);
        return savedTask;
    }
}
