package com.crd.rental.config;
import com.crd.rental.domain.Car;
import com.crd.rental.repository.InMemoryCarRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.ArrayList;

@Configuration
public class BatchPerformanceConfig {
    @Bean
    public ItemReader<Car> carBatchReader(InMemoryCarRepository repository) {
        return new ListItemReader<>(new ArrayList<>(repository.findAll()));
    }
    @Bean
    public ItemProcessor<Car, String> carBatchProcessor() {
        return car -> "Optimized-Report-Log-For-CarID: " + car.id();
    }
    @Bean
    public ItemWriter<String> carBatchWriter() {
        return chunk -> chunk.forEach(System.out::println);
    }
    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(10);
        return executor;
    }
    @Bean
    public Step parallelBatchStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                  ItemReader<Car> reader, ItemProcessor<Car, String> processor, ItemWriter<String> writer) {
        return new StepBuilder("parallelBatchStep", jobRepository)
                .<Car, String>chunk(5, txManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor())
                .build();
    }
    @Bean
    public Job maintenanceJob(JobRepository jobRepository, Step parallelBatchStep) {
        return new JobBuilder("maintenanceJob", jobRepository).start(parallelBatchStep).build();
    }
}
