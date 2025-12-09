/*
   Copyright 2025 WeAreFrank!

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
package org.wearefrank.ladybug.web.common;

import static org.wearefrank.ladybug.web.common.Util.fullMessage;

public class HttpNotFoundException extends Exception {
	public HttpNotFoundException(String msg) {
		super(msg);
	}

	public HttpNotFoundException(Throwable e) {
		super(fullMessage(e));
	}

	public HttpNotFoundException(String msg, Throwable e)
	{
		super(msg + "caused by: " + fullMessage(e));
	}
}
