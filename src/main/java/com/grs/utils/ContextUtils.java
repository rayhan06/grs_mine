package com.grs.utils;

import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextUtils {
    /**
     *
     * @param applicationContext
     * @param annotation
     * @return a map of services and their methods annotated with annotation
     */
    public static Map<String, List<Method>> findServicesWithMethodAnnotation(ApplicationContext applicationContext, Class<? extends Annotation> annotation) {
        Map<String, List<Method>> beanMap = new HashMap<>();
        String[] allBeanNames = applicationContext.getBeanDefinitionNames();
        if (allBeanNames != null) {
            for (String beanName : allBeanNames) {
                Object listener = applicationContext.getBean(beanName);
                Class<?> listenerType = listener.getClass();
                if (Advised.class.isAssignableFrom(listenerType)) {
                    listenerType = ((Advised) listener).getTargetSource().getTargetClass();
                }
                Method[] methods = listenerType.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(annotation)) {
                        List<Method> methodList = beanMap.get(beanName);
                        if (methodList == null) {
                            methodList = new ArrayList<>();
                            beanMap.put(beanName, methodList);
                        }
                        methodList.add(method);
                    }
                }
            }
        }
        return beanMap;
    }
}
