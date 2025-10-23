package com.tech.apicargamasiva.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Value("${async.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${async.executor.thread-name-prefix:import-async-}")
    private String threadNamePrefix;

    @Bean(name = "importacionExecutor")
    public Executor importacionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Configuración del pool
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // Política de rechazo: El caller ejecuta la tarea
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Esperar a que terminen las tareas al hacer shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Permitir que los threads del core timeout
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        executor.initialize();

        log.info("✅ ThreadPoolTaskExecutor 'importacionExecutor' inicializado:");
        log.info("   - Core Pool Size: {}", corePoolSize);
        log.info("   - Max Pool Size: {}", maxPoolSize);
        log.info("   - Queue Capacity: {}", queueCapacity);

        return executor;
    }

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("✅ ThreadPoolTaskExecutor 'emailExecutor' inicializado");

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return importacionExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Error en método asíncrono: {} - Parámetros: {}",
                    method.getName(), params, throwable);
        };
    }
}
