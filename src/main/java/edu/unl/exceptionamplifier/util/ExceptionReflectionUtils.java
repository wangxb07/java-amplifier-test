package edu.unl.exceptionamplifier.util; // THIS MUST BE THE FIRST LINE

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ExceptionReflectionUtils {
    // ... (rest of the code as provided previously)
    public static int countDeclaredExceptions(
            List<String> apiCallStrings,
            Map<String, String> serviceKeyToClassNameMap) {

        if (apiCallStrings == null || serviceKeyToClassNameMap == null) {
            System.err.println("API call strings or FQCN map is null. Returning 0.");
            return 0;
        }

        int totalPotentialExceptions = 0;

        for (String apiCallString : apiCallStrings) {
            if (apiCallString == null || !apiCallString.contains(".")) {
                System.err.println("Skipping invalid API call string (must contain '.'): " + apiCallString);
                continue;
            }

            int firstDotIndex = apiCallString.indexOf('.');
            if (firstDotIndex == -1 || firstDotIndex == 0 || firstDotIndex == apiCallString.length() - 1) {
                 System.err.println("Skipping malformed API call string (dot position invalid): " + apiCallString);
                 continue;
            }
            String serviceKey = apiCallString.substring(0, firstDotIndex);
            String methodName = apiCallString.substring(firstDotIndex + 1);


            String fqcn = serviceKeyToClassNameMap.get(serviceKey);
            if (fqcn == null) {
                System.err.println("No FQCN mapping found for service key: \"" + serviceKey + "\" in API call: \"" + apiCallString + "\"");
                continue;
            }

            try {
                Class<?> targetClass = Class.forName(fqcn);
                Method[] methods = targetClass.getMethods(); 
                boolean methodFound = false;
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        Class<?>[] exceptionTypes = method.getExceptionTypes();
                        totalPotentialExceptions += exceptionTypes.length;
                        methodFound = true;
                        break; 
                    }
                }
                if (!methodFound) {
                   // System.err.println("Method \"" + methodName + "\" not found in class \"" + fqcn + "\" (or its public supertypes) for API call: \"" + apiCallString + "\"");
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: \"" + fqcn + "\" for API call: \"" + apiCallString + "\". Error: " + e.getMessage());
            } catch (SecurityException e) {
                System.err.println("Security exception accessing class/method for: \"" + apiCallString + "\". Error: " + e.getMessage());
            } catch (LinkageError e) { 
                 System.err.println("Linkage error processing method " + methodName + " in class " + fqcn + ". Error: " + e.getMessage());
            }
        }
        return totalPotentialExceptions;
    }

    public static Throwable createExceptionInstance(String className, String message) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = null;
            Throwable instance = null;

            // Try to find a constructor that takes a String (message)
            try {
                constructor = clazz.getConstructor(String.class);
                instance = (Throwable) constructor.newInstance(message);
            } catch (NoSuchMethodException e) {
                // If no String constructor, try a no-arg constructor
                try {
                    constructor = clazz.getConstructor();
                    instance = (Throwable) constructor.newInstance();
                    // Attempt to set message if possible (e.g. via initCause or if it's a RuntimeException)
                    // This part is a bit speculative as there's no standard setCauseWithMessage method.
                    // For simplicity, we'll rely on the constructor or let it be without a message if no String constructor.
                } catch (NoSuchMethodException ex) {
                    System.err.println("No suitable constructor found for " + className + " (tried String and no-arg). Returning generic RuntimeException.");
                    return new RuntimeException("Could not instantiate " + className + ": " + message + " (no suitable constructor)");
                }
            }
            return instance;
        } catch (ClassNotFoundException e) {
            System.err.println("Exception class not found: " + className + ". Returning generic RuntimeException.");
            return new RuntimeException("Original exception class not found: " + className + ", message: " + message, e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error instantiating exception " + className + ": " + e.getMessage() + ". Returning generic RuntimeException.");
            return new RuntimeException("Error instantiating " + className + ": " + message, e);
        } catch (SecurityException e) {
            System.err.println("Security exception while creating instance of " + className + ": " + e.getMessage() + ". Returning generic RuntimeException.");
            return new RuntimeException("Security issue creating " + className + ": " + message, e);
        }
    }
}