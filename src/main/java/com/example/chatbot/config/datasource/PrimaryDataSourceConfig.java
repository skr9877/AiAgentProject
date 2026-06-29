package com.example.chatbot.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * Primary DataSource (Oracle - coupleapp)
 * 매퍼 패키지: com.example.chatbot.tools
 * XML 위치: classpath:mapper/primary/*.xml
 *
 * 새 데이터소스 추가 방법:
 *   1. SecondaryDataSourceConfig.java 참고
 *   2. 매퍼 인터페이스를 별도 패키지에 생성 (ex. com.example.chatbot.external)
 *   3. XML 파일을 mapper/secondary/ 에 추가
 */
@Configuration
@MapperScan(
        basePackages = "com.example.chatbot.tools",
        sqlSessionFactoryRef = "primarySqlSessionFactory"
)
public class PrimaryDataSourceConfig {

    @Primary
    @Bean("primaryDataSourceProperties")
    @ConfigurationProperties("primary.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean("primaryDataSource")
    @ConfigurationProperties("primary.datasource.hikari")
    public HikariDataSource primaryDataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                         .type(HikariDataSource.class)
                         .build();
    }

    @Primary
    @Bean("primarySqlSessionFactory")
    public SqlSessionFactory primarySqlSessionFactory(
            @Qualifier("primaryDataSource") DataSource dataSource) throws Exception {

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/primary/*.xml"));

        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(config);

        factory.setTypeAliasesPackage("com.example.chatbot.tools,com.example.chatbot.common");

        return factory.getObject();
    }
}
