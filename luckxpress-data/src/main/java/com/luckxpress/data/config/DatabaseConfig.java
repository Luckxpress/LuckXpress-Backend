package com.luckxpress.data.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Database Configuration
 * CRITICAL: Configures database connections, JPA, and auditing
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.luckxpress.data.repository")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class DatabaseConfig {
    
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    
    @Value("${spring.datasource.password}")
    private String databasePassword;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    
    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String hibernateDdlAuto;
    
    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;
    
    @Value("${spring.jpa.properties.hibernate.format_sql:false}")
    private boolean formatSql;
    
    @Value("${luckxpress.database.pool.minimum-idle:5}")
    private int minimumIdle;
    
    @Value("${luckxpress.database.pool.maximum-pool-size:20}")
    private int maximumPoolSize;
    
    @Value("${luckxpress.database.pool.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${luckxpress.database.pool.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${luckxpress.database.pool.max-lifetime:1800000}")
    private long maxLifetime;
    
    /**
     * Primary DataSource with HikariCP connection pooling
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.datasource.url")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection properties
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(driverClassName);
        
        // Connection pool settings
        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Pool name for monitoring
        config.setPoolName("LuckXpress-HikariCP");
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // Performance optimizations
        config.setLeakDetectionThreshold(60000); // 60 seconds
        config.setRegisterMbeans(true); // Enable JMX monitoring
        
        // PostgreSQL specific optimizations
        if (driverClassName.contains("postgresql")) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
        }
        
        return new HikariDataSource(config);
    }
    
    /**
     * EntityManagerFactory configuration
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.luckxpress.data.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        
        // Hibernate properties
        properties.setProperty("hibernate.hbm2ddl.auto", hibernateDdlAuto);
        properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
        properties.setProperty("hibernate.format_sql", String.valueOf(formatSql));
        properties.setProperty("hibernate.use_sql_comments", "true");
        
        // Database dialect
        if (driverClassName.contains("postgresql")) {
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else if (driverClassName.contains("h2")) {
            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        }
        
        // Performance settings
        properties.setProperty("hibernate.jdbc.batch_size", "25");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        
        // Second level cache (disabled for now)
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        
        // Connection handling
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.connection.autocommit", "false");
        
        // Statistics (enable in development)
        properties.setProperty("hibernate.generate_statistics", String.valueOf(showSql));
        
        // Envers (audit) configuration
        properties.setProperty("org.hibernate.envers.audit_table_suffix", "_audit");
        properties.setProperty("org.hibernate.envers.revision_field_name", "revision_id");
        properties.setProperty("org.hibernate.envers.revision_type_field_name", "revision_type");
        properties.setProperty("org.hibernate.envers.store_data_at_delete", "true");
        
        em.setJpaProperties(properties);
        
        return em;
    }
    
    /**
     * Transaction Manager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
    
    /**
     * Auditor Provider for JPA Auditing
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }
    
    /**
     * H2 Configuration for local development
     */
    @Configuration
    @Profile("local")
    @ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.h2.Driver")
    static class H2Config {
        
        @Bean
        @Primary
        public DataSource h2DataSource(
                @Value("${spring.datasource.url}") String url,
                @Value("${spring.datasource.username}") String username,
                @Value("${spring.datasource.password}") String password) {
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.h2.Driver");
            
            // H2 specific settings
            config.setMinimumIdle(1);
            config.setMaximumPoolSize(5);
            config.setConnectionTimeout(30000);
            config.setPoolName("LuckXpress-H2-HikariCP");
            
            // H2 optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "50");
            
            return new HikariDataSource(config);
        }
    }
    
    /**
     * PostgreSQL Configuration for production
     */
    @Configuration
    @Profile({"dev", "prod"})
    @ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.postgresql.Driver")
    static class PostgreSQLConfig {
        
        @Bean
        @Primary
        public DataSource postgresDataSource(
                @Value("${spring.datasource.url}") String url,
                @Value("${spring.datasource.username}") String username,
                @Value("${spring.datasource.password}") String password,
                @Value("${luckxpress.database.pool.maximum-pool-size:50}") int maxPoolSize) {
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            
            // Production pool settings
            config.setMinimumIdle(10);
            config.setMaximumPoolSize(maxPoolSize);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(300000); // 5 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setPoolName("LuckXpress-PostgreSQL-HikariCP");
            
            // PostgreSQL optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("ApplicationName", "LuckXpress-Backend");
            
            // Connection validation
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);
            
            return new HikariDataSource(config);
        }
    }
}
