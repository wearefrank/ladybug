/*
   Copyright 2024-2025 WeAreFrank!

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
package nl.nn.testtool.storage.memory;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;

/**
 * @author Jaco de Groot
 */
@Dependent
public class MemoryLogStorage extends MemoryStorage implements LogStorage {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private String lastExceptionMessage;

	@Override
	public void storeWithoutException(Report report) {
		try {
			store(report);
		} catch(Throwable throwable) {
			lastExceptionMessage = throwable.getMessage();
			// When StorageException is should already be logged
			if (!(throwable instanceof StorageException)) {
				log.error("Caught unexpected throwable storing report", throwable);
			}
		}
	}

	@Override
	public String getWarningsAndErrors() {
		return lastExceptionMessage;
	}

}
