/*
   Copyright 2026 WeAreFrank!

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
package org.wearefrank.ladybug.otel.companion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OtelTestCompanionApplication {

	public static void main(String[] args) {
		// Exit explicitly so that threads of the OpenTelemetry exporter cannot keep the JVM alive. When sending fails,
		// SpringApplication.run() throws and the JVM exits with a non-zero exit code.
		System.exit(SpringApplication.exit(SpringApplication.run(OtelTestCompanionApplication.class, args)));
	}
}
