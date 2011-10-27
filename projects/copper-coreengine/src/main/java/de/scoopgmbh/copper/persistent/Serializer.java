/*
 * Copyright 2002-2011 SCOOP Software GmbH
 *
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
 */
package de.scoopgmbh.copper.persistent;

import de.scoopgmbh.copper.Response;
import de.scoopgmbh.copper.Workflow;
import de.scoopgmbh.copper.common.WorkflowRepository;

/**
 * Service for serializing and deserializing {@link Workflow} instances and {@link Response} instances.
 * The implementation decides how to serialize an instance, e.g. using standard java serialization or XML or...
 * 
 * @author austermann
 *
 */
public interface Serializer {
	
	public String serializeWorkflow(final Workflow<?> o) throws Exception;
	public Workflow<?> deserializeWorkflow(String _data, final WorkflowRepository wfRepo) throws Exception;
	
	public String serializeResponse(final Response<?> r) throws Exception;
	public Response<?> deserializeResponse(String _data) throws Exception;
	
}