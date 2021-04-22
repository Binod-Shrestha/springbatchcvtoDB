package com.shresthabinod.springbatch;

import ch.qos.logback.core.db.DriverManagerConnectionSource;
import com.shresthabinod.springbatch.model.Product;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration

public class BatchConfig {

    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job(){return jobBuilderFactory.get("j2")
            .incrementer(new RunIdIncrementer())
            .start(step())
            .build();}

    @Bean
    public Step step(){return stepBuilderFactory.get("step-1").<Product, Product>chunk(3)
            .reader(reader())
            .processor(processor())
            .writer(writer()).build();
    }

    @Bean
    public ItemReader<Product> reader(){
        FlatFileItemReader<Product> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("products.csv"));

        DefaultLineMapper<Product> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setNames("id", "name", "description", "price");
        BeanWrapperFieldSetMapper<Product> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Product.class);

        lineMapper.setLineTokenizer(delimitedLineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        reader.setLineMapper(lineMapper);

        return reader;
    }

    @Bean
    public ItemProcessor<Product, Product> processor(){
        return (p) -> {
            p.setPrice(p.getPrice()-p.getPrice()*10/100);
            return p;
        };
    }

    //writing data to database
    @Bean
    public ItemWriter<Product> writer(){
        JdbcBatchItemWriter<Product> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(dataSource());
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("INSERT INTO PRODUCT (ID, NAME, DESCRIPTION, PRICE) VALUES(:id, : name, : description, :price)");
        return writer;
    }
    @Bean
    public DataSource dataSource(){
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/testDB");
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        return dataSource;
    }
}
