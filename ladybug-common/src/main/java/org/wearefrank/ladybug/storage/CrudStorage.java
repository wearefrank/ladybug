/*
   Copyright 2020, 2022, 2024, 2025 WeAreFrank!, 2018 Nationale-Nederlanden

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
package org.wearefrank.ladybug.storage;

import org.wearefrank.ladybug.Report;

/**
 * Storage supporting Create, Read, Update and Delete actions. Primarily intended for the Test tab although it can be
 * used in the Debug tab as well (be careful when doing so, see {@link LogStorage}). 
 * 
 * @author Jaco de Groot
 */
public interface CrudStorage extends Storage {
	public void store(Report report) throws StorageException;

	public void update(Report report) throws StorageException;

	public void delete(Report report) throws StorageException;

}
