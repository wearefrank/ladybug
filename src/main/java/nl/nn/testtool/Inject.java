/*
   Copyright 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool;

/**
 * The pom.xml will replace javax.inject.Inject to this dummy to prevent problems with WebSphere when Enable-CDI is not
 * set to false (see also: https://github.com/ibissource/iaf/pull/4211/files):
 * <pre>
 * com.ibm.ws.exception.RuntimeWarning: com.ibm.ws.exception.RuntimeError: com.ibm.ws.exception.RuntimeError: java.lang.RuntimeException: com.ibm.ws.cdi.CDIRuntimeException: com.ibm.ws.cdi.CDIDeploymentRuntimeException: org.jboss.weld.exceptions.DeploymentException: Exception List with 6 exceptions:
 * Exception 0 :
 * org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type List&lt;String&gt; with qualifiers @Default
 *   at injection point [BackedAnnotatedField] @Inject @Autowired private nl.nn.testtool.filter.View.metadataNames
 *   at nl.nn.testtool.filter.View.metadataNames(View.java:0)
 *   at org.jboss.weld.bootstrap.Validator.validateInjectionPointForDeploymentProblems(Validator.java:362)
 *   at org.jboss.weld.bootstrap.Validator.validateInjectionPoint(Validator.java:284)
 *   at org.jboss.weld.bootstrap.Validator.validateGeneralBean(Validator.java:137)
 *   at org.jboss.weld.bootstrap.Validator.validateRIBean(Validator.java:158)
 *   at org.jboss.weld.bootstrap.Validator.validateBean(Validator.java:501)
 *   at org.jboss.weld.bootstrap.ConcurrentValidator$1.doWork(ConcurrentValidator.java:61)
 *   at org.jboss.weld.bootstrap.ConcurrentValidator$1.doWork(ConcurrentValidator.java:59)
 *   at org.jboss.weld.executor.IterativeWorkerTaskFactory$1.call(IterativeWorkerTaskFactory.java:62)
 *   at org.jboss.weld.executor.IterativeWorkerTaskFactory$1.call(IterativeWorkerTaskFactory.java:55)
 *   at java.util.concurrent.FutureTask.run(FutureTask.java:277)
 *   at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1160)
 *   at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
 *   at java.lang.Thread.run(Thread.java:825)
 * </pre>
 * 
 * @author Jaco de Groot
 */
public @interface Inject {

}