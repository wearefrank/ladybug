package org.wearefrank.ladybug.web.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LadybugValidator {
	private ValidatorFactory validatatorFactory = Validation
			.byDefaultProvider()
			.configure()
			.messageInterpolator(new ParameterMessageInterpolator())
			.buildValidatorFactory();

	public <T> void validateObject(T object) {
		Validator validator = validatatorFactory.getValidator();
		Set<ConstraintViolation<T>> violations = validator.validate(object);
		if (!violations.isEmpty()) {
			List<String> messageLines = new ArrayList<>();
			messageLines.add(String.format("LadybugValidator.validateObject(): Integrity violation of an %s", object.getClass().getName()));
			for (ConstraintViolation<T> violation: violations) {
				messageLines.add(violation.toString());
				throw new IllegalArgumentException(messageLines.stream().collect(Collectors.joining("\n")));
			}
		}
	}
}
