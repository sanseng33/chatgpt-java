package com.unfbx.chatgpt.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class JsonSchemaEnumAspect {

    @Around("execution(* *(..)) && @annotation(ResponseBody)")
    public Object processEnum(ProceedingJoinPoint joinPoint) throws Throwable {
        Object retVal = joinPoint.proceed();

        // Get the fields of the returned object's class
        Field[] fields = retVal.getClass().getDeclaredFields();
        for (Field field : fields) {
            // Check if the field has the JsonSchemaEnum annotation
            JsonSchemaEnum annotation = field.getAnnotation(JsonSchemaEnum.class);
            if (annotation != null) {
                // Get the enum values from the annotation
                List<String> enumValues = Arrays.asList(annotation.values());

                // Create a map to represent the enum field in the desired format
                Map<String, Object> enumFieldMap = new HashMap<>();

                enumFieldMap.put("enum", enumValues);

                // Replace the field's value with the map
                field.setAccessible(true);
                field.set(retVal, enumFieldMap);
            }
        }
        return retVal;
    }
}
