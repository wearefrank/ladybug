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
package org.wearefrank.ladybug.test.webapp.springmvc;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.wearefrank.ladybug.storage.database.DbmsSupport;
import org.wearefrank.ladybug.transform.ReportXmlTransformer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.wearefrank.ladybug.web.FrontendServlet;
import org.wearefrank.ladybug.web.common.TestToolInfoResponse;
import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.Filter;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

@Configuration
@SpringBootApplication
@ImportResource("classpath:ladybugSpringBootWebapp.xml")
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, proxyTargetClass = true)
@Slf4j
public class LadybugSpringBootApplication {
	// All backend roles known to Ladybug. Granted to anonymous requests when
	// ladybug.ui.test.mode=NO_AUTH, so that both the URL-level and the
	// @RolesAllowed method-level authorization checks let every request through.
	private static final String[] ALL_LADYBUG_ROLES = {
			"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester", "IbisWebService"
	};

	@Value("${ladybug.ui.test.mode:DEFAULT}")
	private TestToolInfoResponse.UI_TEST_MODE uiTestMode;

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
		// Protect the whole Ladybug application (frontend and api), not just /ladybug/api. Otherwise the
		// initial page load is never challenged for Basic Authentication, so the browser never caches
		// credentials for the realm and subsequent calls to /ladybug/api are sent unauthenticated.
		PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults().basePath("/ladybug");
		// Endpoints on which the SecurityFilterChain (filter) will match, also for OPTIONS requests!
		// This does not authenticate the user, but only means the filter will be triggered.
		http.securityMatcher(builder.matcher("/**"));

		if (uiTestMode.equals(TestToolInfoResponse.UI_TEST_MODE.NO_AUTH)) {
			log.error("Behavior of ladybug modified by test.properties - not for production!: authorization for {} is disabled because ladybug.ui.test.mode=NO_AUTH", builder.matcher("/**"));
			// Let every request through at the URL level...
			http.authorizeHttpRequests(requests -> requests
					.requestMatchers(builder.matcher("/**")).permitAll());
			// ...and grant every Ladybug role to unauthenticated requests, so that
			// @RolesAllowed checks on controller methods do not block them either.
			http.anonymous(anonymous -> anonymous.authorities(AuthorityUtils.createAuthorityList(
					Arrays.stream(ALL_LADYBUG_ROLES).map(role -> "ROLE_" + role).toArray(String[]::new))));
		} else {
			// Enables security for URL /ladybug
			http.authorizeHttpRequests(requests -> requests
					.requestMatchers(builder.matcher("/**")).authenticated());

			// Uses a BasicAuthenticationEntryPoint to force users to log in
			http.httpBasic(Customizer.withDefaults());
		}

		// TODO: Do we want to disable CSRF protection for Ladybug?
		http.csrf().disable();
		return http.build();
	}

	@Bean
	// Spring Boot's autoconfiguration adds its own TransactionManager for the ladybugDataSource bean, in addition to
	// the ladybugTransactionManager bean from org.wearefrank.ladybug.Config. This makes @Transactional on
	// DatabaseStorage ambiguous, so tell annotation-driven transaction management which one to use.
	TransactionManagementConfigurer transactionManagementConfigurer(
			@Qualifier("ladybugTransactionManager") TransactionManager ladybugTransactionManager) {
		return () -> ladybugTransactionManager;
	}

	@Bean
	InMemoryUserDetailsManager userDetailsManager() {
		UserDetails observerUser = User.builder()
				.username("IbisObserver")
				.password("{noop}IbisObserver")
				.roles("IbisObserver")
				.build();
		UserDetails dataAdminUser = User.builder()
				.username("IbisDataAdmin")
				.password("{noop}IbisDataAdmin")
				.roles("IbisDataAdmin")
				.build();
		UserDetails adminUser = User.builder()
				.username("IbisAdmin")
				.password("{noop}IbisAdmin")
				.roles("IbisAdmin")
				.build();
		UserDetails testerUser = User.builder()
				.username("IbisTester")
				.password("{noop}IbisTester")
				.roles("IbisTester")
				.build();
		// Create an UserDetailsManager without any users.
		return new InMemoryUserDetailsManager(observerUser, dataAdminUser, adminUser, testerUser);
	}
}
