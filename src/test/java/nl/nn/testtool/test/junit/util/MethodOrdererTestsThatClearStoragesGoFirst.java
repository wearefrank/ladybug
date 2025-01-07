package nl.nn.testtool.test.junit.util;

import nl.nn.testtool.test.junit.storage.TestStorages;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

import java.util.Comparator;

public final class MethodOrdererTestsThatClearStoragesGoFirst implements MethodOrderer {

    @Override
    public void orderMethods(MethodOrdererContext context) {
        context.getMethodDescriptors().sort(COMPARATOR);
    }

    private static final Comparator<MethodDescriptor> COMPARATOR = (d1, d2) -> {
        int result = Priority.of(d1).compareTo(Priority.of(d2));
        if (result == 0) {
            result = d1.getMethod().getDeclaringClass().getName().compareTo(d2.getMethod().getDeclaringClass().getName());
        }
        if (result == 0) {
            result = d1.getDisplayName().compareTo(d2.getDisplayName());
        }
        return result;
    };
    private enum Priority {
        CLEARS_STORAGES,
        KEEPS_STORAGES;

        public static Priority of(MethodDescriptor d) {
            if (d.getMethod().getDeclaringClass().getName().equals(TestStorages.class.getName())) {
                return CLEARS_STORAGES;
            } else {
                return KEEPS_STORAGES;
            }
        }
    }
}
