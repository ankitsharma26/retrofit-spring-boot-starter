package com.github.lianjiatech.retrofit.spring.boot.core;

import com.github.lianjiatech.retrofit.spring.boot.annotation.RetrofitClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * @author 陈添明
 */
public class ClassPathRetrofitClientScanner extends ClassPathBeanDefinitionScanner {

    private final ClassLoader classLoader;

    private final static Logger logger = LoggerFactory.getLogger(ClassPathRetrofitClientScanner.class);
    private final Environment environment;

    public ClassPathRetrofitClientScanner(BeanDefinitionRegistry registry, ClassLoader classLoader, Environment environment) {
        super(registry, false);
        this.classLoader = classLoader;
        this.environment = environment;
    }

    public void registerFilters() {
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(RetrofitClient.class);
        this.addIncludeFilter(annotationTypeFilter);
    }


    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            logger.warn("No RetrofitClient was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
        } else {
            processBeanDefinitions(beanDefinitions);
        }
        return beanDefinitions;
    }

    @Override
    protected boolean isCandidateComponent(
            AnnotatedBeanDefinition beanDefinition) {
        if (beanDefinition.getMetadata().isInterface()) {
            try {
                Class<?> target = ClassUtils.forName(
                        beanDefinition.getMetadata().getClassName(),
                        classLoader);

                return !target.isAnnotation() && legalBaseUrl(target);
            } catch (Exception ex) {
                logger.error("load class exception:", ex);
            }
        }
        return false;
    }

    private boolean legalBaseUrl(Class<?> target) {
        final RetrofitClient retrofitClient = target.getAnnotation(RetrofitClient.class);
        final String baseUrl = retrofitClient.baseUrl();
        if (StringUtils.isEmpty(baseUrl)) {
            logger.warn("No config baseUrl! interface={}", target);
            return false;
        }

        try {
            environment.resolveRequiredPlaceholders(baseUrl);
        } catch (Exception e) {
            logger.warn("No config baseUrl! interface={}", target);
            return false;
        }
        return true;
    }


    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        GenericBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (GenericBeanDefinition) holder.getBeanDefinition();
            if (logger.isDebugEnabled()) {
                logger.debug("Creating RetrofitClientBean with name '" + holder.getBeanName()
                        + "' and '" + definition.getBeanClassName() + "' Interface");
            }
            definition.getConstructorArgumentValues().addGenericArgumentValue(Objects.requireNonNull(definition.getBeanClassName()));
            // beanClass全部设置为RetrofitFactoryBean
            definition.setBeanClass(RetrofitFactoryBean.class);
        }
    }
}
