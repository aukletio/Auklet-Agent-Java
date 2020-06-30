package io.auklet.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SysUtilTest {
    @Test void testGetValue() {
        assertEquals("0", SysUtil.getValue("0", "1", "2", false));
        assertEquals(true, SysUtil.getValue(true, "1", "2", false));
        assertNull(SysUtil.getValue((String)null, "1", "2", false));
        assertNull(SysUtil.getValue((Boolean)null, "1", "2", false));
        assertNull(SysUtil.getValue((Integer)null, "1", "2", false));
    }

    @Test void testGetEnvVar() throws Exception {
        setEnv("test", "testAnswer");
        assertEquals(SysUtil.getEnvVar("test", false), "testAnswer");
    }

    @Test void testGetSysProp() {
        System.setProperty("test", "testAnswer");
        assertEquals(SysUtil.getSysProp("test", false), "testAnswer");
    }

    // Hack for changing the environment variable in Java. This method should only be used for testing
    // https://stackoverflow.com/a/7201825
    protected static void setEnv(String key, String value) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.put(key, value);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.put(key, value);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.put(key, value);
                }
            }
        }
    }
}
