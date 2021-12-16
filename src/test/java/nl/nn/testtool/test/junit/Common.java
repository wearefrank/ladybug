package nl.nn.testtool.test.junit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Common {
	// Use this context at least in all tests that use debug storage otherwise when more then one context is creating
	// storage beans the storageId's are likely to not be unique anymore which will give unexpected results
	public static final ApplicationContext CONTEXT_FILE_STORAGE = new ClassPathXmlApplicationContext("springTestToolTestJUnit-forFileStorage.xml");
}
