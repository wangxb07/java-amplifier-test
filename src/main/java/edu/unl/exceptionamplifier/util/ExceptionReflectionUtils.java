package edu.unl.exceptionamplifier.util; // THIS MUST BE THE FIRST LINE

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

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
}