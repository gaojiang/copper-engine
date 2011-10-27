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
package de.scoopgmbh.copper.tranzient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import de.scoopgmbh.copper.CopperRuntimeException;
import de.scoopgmbh.copper.DependencyInjector;
import de.scoopgmbh.copper.EngineState;
import de.scoopgmbh.copper.ProcessingEngine;
import de.scoopgmbh.copper.Response;
import de.scoopgmbh.copper.WaitMode;
import de.scoopgmbh.copper.Workflow;
import de.scoopgmbh.copper.common.AbstractProcessingEngine;
import de.scoopgmbh.copper.common.ProcessorPoolManager;
import de.scoopgmbh.copper.common.TicketPoolManager;
import de.scoopgmbh.copper.persistent.PersistentWorkflow;

/**
 * Transient implementation of a COPPER {@link ProcessingEngine}.
 * 
 * A transient engine may run instances of {@link Workflow} or {@link PersistentWorkflow}.
 * Anyhow, alle workflow instances will only reside in the local JVM heap.  
 * 
 * @author austermann
 *
 */
public class TransientScottyEngine extends AbstractProcessingEngine implements ProcessingEngine {

	private static final Logger logger = Logger.getLogger(TransientScottyEngine.class);

	private final Map<String, CorrelationSet> correlationMap = new HashMap<String, CorrelationSet>(50000);
	private final Map<String, Workflow<?>> workflowMap = new ConcurrentHashMap<String, Workflow<?>>(50000);
	private ProcessorPoolManager<TransientProcessorPool> poolManager;
	private TimeoutManager timeoutManager;
	private EarlyResponseContainer earlyResponseContainer;
	private TicketPoolManager ticketPoolManager;
	private DependencyInjector dependencyInjector;
	
	public void setTicketPoolManager(TicketPoolManager ticketPoolManager) {
		if (ticketPoolManager == null) throw new NullPointerException();
		this.ticketPoolManager = ticketPoolManager;
	}

	public void setTimeoutManager(TimeoutManager timeoutManager) {
		if (timeoutManager == null) throw new NullPointerException();
		this.timeoutManager = timeoutManager;
	}

	public void setPoolManager(ProcessorPoolManager<TransientProcessorPool> poolManager) {
		if (poolManager == null) throw new NullPointerException();
		this.poolManager = poolManager;
	}

	public void setEarlyResponseContainer(EarlyResponseContainer earlyResponseContainer) {
		if (earlyResponseContainer == null) throw new NullPointerException();
		this.earlyResponseContainer = earlyResponseContainer;
	}
	
	public void setDependencyInjector(DependencyInjector dependencyInjector) {
		if (dependencyInjector == null) throw new NullPointerException();
		this.dependencyInjector = dependencyInjector;
	}

	@Override
	public void notify(Response<?> response) {
		if (logger.isDebugEnabled()) logger.debug("notify("+response+")");

		try {
			startupBlocker.pass();
		} 
		catch (InterruptedException e) {
			// ignore
		}
		
		synchronized (correlationMap) {
			final CorrelationSet cs = correlationMap.remove(response.getCorrelationId());
			if (cs == null) {
				earlyResponseContainer.put(response);
				return;
			}
			final Workflow<?> wf = workflowMap.get(cs.getWorkflowId());
			if (wf == null) {
				logger.error("Workflow with id "+cs.getWorkflowId()+" not found");
				return;
			}
			cs.getMissingCorrelationIds().remove(response.getCorrelationId());
			if (cs.getTimeoutTS() != null && !response.isTimeout()) timeoutManager.unregisterTimeout(cs.getTimeoutTS(), response.getCorrelationId());
			wf.putResponse(response);

			if (cs.getMode() == WaitMode.FIRST) {
				if (!cs.getMissingCorrelationIds().isEmpty()) {
					if (cs.getTimeoutTS() != null && !response.isTimeout()) timeoutManager.unregisterTimeout(cs.getTimeoutTS(), cs.getMissingCorrelationIds());
					for (String cid : cs.getMissingCorrelationIds()) {
						correlationMap.remove(cid);
					}
					earlyResponseContainer.putStaleCorrelationId(cs.getMissingCorrelationIds());
				}
				enqueue(wf);
				return;
			}
			if (cs.getMissingCorrelationIds().isEmpty()) {
				enqueue(wf);
				return;
			}
		}
	}

	@Override
	public void run(Workflow<?> w) {
		try {
			startupBlocker.pass();
		} 
		catch (InterruptedException e) {
			// ignore
		}

		ticketPoolManager.obtain(w);
		try {
			boolean newId = false;
			if (w.getId() == null) {
				w.setId(createUUID());
				newId = true;
			};
			if (w.getProcessorPoolId() == null) {
				w.setProcessorPoolId(TransientProcessorPool.DEFAULT_POOL_ID);
			}
			synchronized (workflowMap) {
				if (!newId && workflowMap.containsKey(w.getId()))
					throw new IllegalStateException("engine already contains a workflow with id '"+w.getId()+"'");
				workflowMap.put(w.getId(), w);
			}
			dependencyInjector.inject(w);
			enqueue(w);
		}
		catch(RuntimeException e) {
			logger.error("run/enqeue failed",e);
			workflowMap.remove(w.getId());
			ticketPoolManager.release(w);
			throw e;
		}
		catch(Exception e) {
			logger.error("run/enqeue failed",e);
			workflowMap.remove(w.getId());
			ticketPoolManager.release(w);
			throw new CopperRuntimeException(e);
		}
	}

	private void enqueue(Workflow<?> w) {
		TransientProcessorPool pool = poolManager.getProcessorPool(w.getProcessorPoolId());
		if (pool == null) {
			logger.fatal("Unable to find processor pool "+w.getProcessorPoolId()+" - using default processor pool");
			pool = poolManager.getProcessorPool(TransientProcessorPool.DEFAULT_POOL_ID);
		}
		pool.enqueue(w);
	}

	@Override
	public synchronized void shutdown() {
		if (engineState != EngineState.STARTED)
			throw new IllegalStateException();

		logger.info("Engine is shutting down...");
		engineState = EngineState.SHUTTING_DOWN;
		wfRepository.shutdown();
		timeoutManager.shutdown();
		earlyResponseContainer.shutdown();
		poolManager.shutdown();
		super.shutdown();
		logger.info("Engine is stopped");
		engineState = EngineState.STOPPED;
	}

	@Override
	public synchronized void startup() {
		if (engineState != EngineState.RAW)
			throw new IllegalStateException();
		
		logger.info("Engine is starting up...");
		wfRepository.start();
		timeoutManager.setEngine(this);
		poolManager.setEngine(this);
		dependencyInjector.setEngine(this);
		timeoutManager.startup();
		earlyResponseContainer.startup();
		poolManager.startup();
		engineState = EngineState.STARTED;
		logger.info("Engine is running");
		startupBlocker.unblock();
	}

	@Override
	public void registerCallbacks(Workflow<?> w, WaitMode mode, long timeoutMsec, String... correlationIds) {
		if (logger.isDebugEnabled()) logger.debug("registerCallbacks("+w+", "+mode+", "+timeoutMsec+", "+Arrays.asList(correlationIds)+")");
		if (correlationIds.length == 0) throw new IllegalArgumentException("No correlationids given");

		boolean doEnqueue = false;
		CorrelationSet cs = new CorrelationSet(w, correlationIds, mode, timeoutMsec > 0 ? System.currentTimeMillis() + timeoutMsec : null);
		synchronized (correlationMap) {
			for (String cid : correlationIds) {
				Response<?> earlyResponse = earlyResponseContainer.get(cid);
				if (earlyResponse != null) {
					w.putResponse(earlyResponse);
					cs.getMissingCorrelationIds().remove(cid);
				}
			}
			if (cs.getMissingCorrelationIds().isEmpty() || (cs.getMissingCorrelationIds().size() < correlationIds.length && mode == WaitMode.FIRST)) {
				doEnqueue = true;
			}
			else {
				for (String cid : cs.getMissingCorrelationIds()) {
					correlationMap.put(cid, cs);
				}
				if (cs.getTimeoutTS() != null) {
					if (mode == WaitMode.FIRST)
						timeoutManager.registerTimeout(cs.getTimeoutTS().longValue(), cs.getMissingCorrelationIds().get(0));
					else 
						timeoutManager.registerTimeout(cs.getTimeoutTS().longValue(), cs.getMissingCorrelationIds());
				}
			}
		}
		if (doEnqueue) {
			enqueue(w);
		}
	}

	public void removeWorkflow(String id) {
		final Workflow<?> wf = workflowMap.remove(id);
		if (wf != null) {
			ticketPoolManager.release(wf);
		}
	}

	@Override
	public void run(List<Workflow<?>> list) {
		for (Workflow<?> w : list) {
			run(w);
		}
	}



}