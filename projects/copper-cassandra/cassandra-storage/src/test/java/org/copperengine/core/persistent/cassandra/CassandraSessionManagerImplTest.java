/*
 * Copyright 2002-2015 SCOOP Software GmbH
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
package org.copperengine.core.persistent.cassandra;

import static org.junit.Assume.assumeFalse;

import java.util.Collections;

import org.junit.Test;

public class CassandraSessionManagerImplTest extends CassandraUnitTest {

    @Test()
    public void test() {
        assumeFalse(skipTests());
        CassandraSessionManagerImpl cassandraSessionManagerImpl = new CassandraSessionManagerImpl(Collections.singletonList("localhost"), null, "copper");
        cassandraSessionManagerImpl.startup();
        cassandraSessionManagerImpl.shutdown();
    }

}