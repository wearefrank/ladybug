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
package nl.nn.testtool.web.api;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
class LoginController {
	@ResponseBody
	@GetMapping("/login")
	public String login() {
		return Arrays.asList(new String[] {
			"<html>",
			"<head><title>Login</title></head>",
			"  <body>",
			"    <form method=\"post\" action=\"/ladybug/api/login\">",
			"      <input type=\"text\" name=\"username\" placeholder=\"Username\"/>",
			"      <input type=\"password\" name=\"password\" placeholder=\"Password\"/>",
			"      <button type=\"submit\">Login</button>",
			"    </form>",
			"  </body>",
			"</html>"
		}).stream().collect(Collectors.joining("\n"));
	}
}