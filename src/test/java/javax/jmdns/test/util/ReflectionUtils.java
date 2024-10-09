package javax.jmdns.test.util;

import java.lang.reflect.Field;

public class ReflectionUtils {
    private ReflectionUtils() {
        // intentionally left blank
    }

    public static void setInternalState(Object targetObject, String fieldName, Object value) throws Exception {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);  // Make the field accessible
        field.set(targetObject, value);  // Set the new value
    }

    public static Object getInternalState(Object targetObject, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = targetObject.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(targetObject);
    }
}
