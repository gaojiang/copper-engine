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

package org.copperengine.ext.wfrepo.classpath.testworkflows;

import org.copperengine.core.Interrupt;

public class TestWorkflowThree extends TestWorkflowTwo {

    private static final long serialVersionUID = 1L;

    private static class MyInnerClass {
        public void printFoo() {
            System.out.println("foo");
        }
    }

    @Override
    public void main() throws Interrupt {
        new MyInnerClass().printFoo();
        new Runnable() {
            @Override
            public void run() {
                System.out.println("Anonymous inner class!");
            }
        }.run();
    }

}
