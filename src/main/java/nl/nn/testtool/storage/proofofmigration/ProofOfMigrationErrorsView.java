/*
   Copyright 2022, 2024 WeAreFrank!

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
package nl.nn.testtool.storage.proofofmigration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.inject.Inject;
import lombok.Setter;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.storage.LogStorage;

//@Dependent disabled for Quarkus for now because of the use of JdbcTemplate
public class ProofOfMigrationErrorsView extends View {
	protected @Setter @Inject @Autowired ProofOfMigrationErrorsStorage proofOfMigrationErrorsStorage;

	@Override
	public String getName() {
		if (name == null) {
			return "Proof of migration errors";
		} else {
			return name;
		}
	}

	@Override
	public LogStorage getDebugStorage() {
		return proofOfMigrationErrorsStorage;
	}

	@Override
	public List<String> getMetadataNames() {
		return proofOfMigrationErrorsStorage.getMetadataNames();
	}
}
