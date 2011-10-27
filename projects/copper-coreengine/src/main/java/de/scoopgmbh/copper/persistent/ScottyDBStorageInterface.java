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

import java.sql.Connection;
import java.util.List;

import de.scoopgmbh.copper.Response;
import de.scoopgmbh.copper.Workflow;

/**
 * Interface for the storage of a {@link PersistentScottyEngine}.
 * Offers methods for storing and retrieving {@link Workflow}s and {@link Response}s. 
 *  
 * @author austermann
 *
 */
public interface ScottyDBStorageInterface {

	/**
	 * Inserts a new workflow to the underlying database.
	 */
	public void insert(final Workflow<?> wf) throws Exception;

	/**
	 * Inserts a list of new workflows to the underlying database.
	 */
	public void insert(final List<Workflow<?>> wfs) throws Exception;

	/**
	 * Inserts a new workflow to the underlying database using the provided connection.
	 * It is up to the caller commit or rollback and close the connection.
	 */
	public void insert(final Workflow<?> wf, Connection con) throws Exception;

	/**
	 * Inserts a list of new workflows to the underlying database using the provided connection.
	 * It is up to the caller commit or rollback and close the connection.
	 */
	public void insert(final List<Workflow<?>> wfs, Connection con) throws Exception;

	/**
	 * Marks a workflow instance as finished or removes it from the underlying database. 
	 */
	public void finish(final Workflow<?> w);

	/**
	 * Dequeues up to <code>max</code> Workflow instances for the specified processor pool from the database.
	 * It dequeues only such workflow instances that need further processing, e.g. when a response arrived or 
	 * a timeout occured. 
	 */
	public List<Workflow<?>> dequeue(final String ppoolId, final int max)
			throws Exception;

	/**
	 * Asynchronous service to add a {@link Response} to the database.
	 */
	public void notify(final Response<?> response, final Object callback)
			throws Exception;

	/**
	 * Asynchronous service to add a list of {@link Response}s to the database.
	 */
	public void notify(final List<Response<?>> response) throws Exception;

	/**
	 * Writes a workflow instance that is waiting for one or more asynchronous response back to
	 * database.  
	 */
	public void registerCallback(final RegisterCall rc) throws Exception;

	/**
	 * Startup the service
	 */
	public void startup();

	/**
	 * Shutdown the service
	 */
	public void shutdown();
	
	/**
	 * Marks a workflow instance as failed in the database. It may me triggered again later when the
	 * error cause has been solved using the <code>restart</code> method. 
	 */
	public void error(final Workflow<?> w, Throwable t);
	
	/**
	 * Triggers the restart of a failed workflow instance.
	 */
	public void restart(final String workflowInstanceId) throws Exception;

}