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
package org.wearefrank.ladybug.spring.boot;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.wearefrank.ladybug.storage.database.DbmsSupport;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.wearefrank.ladybug.web.FrontendServlet;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.Filter;

@Configuration
@SpringBootApplication
@ImportResource("classpath:ladybugSpringBootWebapp.xml")
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, proxyTargetClass = true)
public class LadybugSpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(LadybugSpringBootApplication.class, args);
	}

	@Bean
	ServletRegistrationBean<FrontendServlet> ladybugFrontendServletBean() {
		ServletRegistrationBean<FrontendServlet> bean = new ServletRegistrationBean<>(
				new FrontendServlet(), "/ladybug/*");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	ServletRegistrationBean<RootRedirectingServlet> rootRedirectingServletBean() {
		ServletRegistrationBean<RootRedirectingServlet> bean = new ServletRegistrationBean<>(
				new RootRedirectingServlet(), "/");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	public SecurityFilterChain ladybugApiSecurityFilterChain(HttpSecurity http) throws Exception {
		PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults().basePath("/ladybug/api");
		// Endpoints on which the SecurityFilterChain (filter) will match, also for OPTIONS requests!
		// This does not authenticate the user, but only means the filter will be triggered.
		http.securityMatcher(builder.matcher("/**"));

		// Enables security for URL /ladybug/api
		http.authorizeHttpRequests(requests -> requests
				.requestMatchers(builder.matcher("/**")).authenticated());

		// Uses a BasicAuthenticationEntryPoint to force users to log in
		http.httpBasic(Customizer.withDefaults());

		// TODO: Do we want to disable CSRF protection for Ladybug?
		http.csrf().disable();
		return http.build();
	}

	@Bean
	InMemoryUserDetailsManager userDetailsManager() {
		UserDetails observerUser = User.builder()
				.username("observer")
				.password("{noop}observer")
				.roles("IbisObserver")
				.build();
		UserDetails dataAdminUser = User.builder()
				.username("dataAdmin")
				.password("{noop}dataAdmin")
				.roles("IbisDataAdmin")
				.build();
		UserDetails adminUser = User.builder()
				.username("admin")
				.password("{noop}admin")
				.roles("IbisAdmin")
				.build();
		UserDetails testerUser = User.builder()
				.username("tester")
				.password("{noop}tester")
				.roles("IbisTester")
				.build();
		// Create an UserDetailsManager without any users.
		return new InMemoryUserDetailsManager(observerUser, dataAdminUser, adminUser, testerUser);
	}

	@Bean
	@Scope("singleton")
	DbmsSupport dbmsSupport() {
		return new DbmsSupport();
	}

	@Bean
	@Scope("singleton")
	public ReportXmlTransformer reportXmlTransformer() {
		return new ReportXmlTransformer();
	}

	@Bean
	@Scope("singleton")
	List<String> metadataNames() {
		List<String> metadataNames = new ArrayList<String>();
		metadataNames.add("storageId");
		metadataNames.add("endTime");
		metadataNames.add("duration");
		metadataNames.add("name");
		metadataNames.add("correlationId");
		metadataNames.add("status");
		metadataNames.add("numberOfCheckpoints");
		metadataNames.add("estimatedMemoryUsage");
		metadataNames.add("storageSize");
		return metadataNames;
	}
}
