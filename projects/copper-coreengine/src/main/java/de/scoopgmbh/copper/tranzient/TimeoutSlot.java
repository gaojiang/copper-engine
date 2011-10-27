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

import java.util.HashSet;
import java.util.Set;

/**
 * Internally used class.
 * 
 * @author austermann
 *
 */
final class TimeoutSlot {
	
	private final long timeoutTS;
    private final Set<String> correlationIds = new HashSet<String>();
	
	public TimeoutSlot(long timeoutTS) {
		assert timeoutTS > 0;
		this.timeoutTS = timeoutTS;
	}

	public Set<String> getCorrelationIds() {
		return correlationIds;
	}
    
    public long getTimeoutTS() {
    	return timeoutTS;
    }
}