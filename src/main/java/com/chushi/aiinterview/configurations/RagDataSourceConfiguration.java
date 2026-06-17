package com.chushi.aiinterview.configurations;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.chushi.aiinterview.rag.mappers", sqlSessionTemplateRef = "ragSqlSessionTemplate")
public class RagDataSourceConfiguration {
    @Bean
    public DataSource ragDataSource(RagProperties ragProperties) {
        var datasource = new HikariDataSource();
        var properties = ragProperties.getDatasource();
        datasource.setJdbcUrl(properties.getUrl());
        datasource.setUsername(properties.getUsername());
        datasource.setPassword(properties.getPassword());
        datasource.setDriverClassName(properties.getDriverClassName());
        datasource.setMaximumPoolSize(5);
        datasource.setMinimumIdle(1);
        return datasource;
    }

    @Bean(initMethod = "migrate")
    public Flyway ragFlyway(@Qualifier("ragDataSource") DataSource ragDataSource) {
        return Flyway.configure()
                .dataSource(ragDataSource)
                .locations("classpath:rag-migrations")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    public SqlSessionFactory ragSqlSessionFactory(@Qualifier("ragDataSource") DataSource ragDataSource) throws Exception {
        var factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ragDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/rag/*.xml"));

        var configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate ragSqlSessionTemplate(@Qualifier("ragSqlSessionFactory") SqlSessionFactory ragSqlSessionFactory) {
        return new SqlSessionTemplate(ragSqlSessionFactory);
    }
}
