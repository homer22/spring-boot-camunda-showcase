package de.sschleis.showcase.configuration;

import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class CamundaConfig {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private PlatformTransactionManager transactionManager;

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DataSource dataSource;

    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(){
        SpringProcessEngineConfiguration result = new SpringProcessEngineConfiguration();
        result.setProcessEngineName("engine");
        result.setDataSource(dataSource);
        result.setTransactionManager(transactionManager);
        result.setDatabaseSchemaUpdate("true");
        Resource[] resources = new Resource[1];
        resources[0] = new ClassPathResource("loan-approval.bpmn");
        result.setDeploymentResources(resources);
        result.setJobExecutorActivate(false);
        return result;
    }

    @Bean
    public ProcessEngineFactoryBean processEngineFactoryBean(){
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
        processEngineFactoryBean.setProcessEngineConfiguration(springProcessEngineConfiguration());
        return processEngineFactoryBean;
    }

    @Bean
    public RepositoryService repositoryService() throws Exception {
        return processEngineFactoryBean().getObject().getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService() throws Exception {
        return processEngineFactoryBean().getObject().getRuntimeService();
    }

    @Bean
    public TaskService taskService() throws Exception {
        return processEngineFactoryBean().getObject().getTaskService();
    }

    @Bean
    public HistoryService historyService() throws Exception {
        return processEngineFactoryBean().getObject().getHistoryService();
    }

    @Bean
    public ManagementService managementService() throws Exception {
        return processEngineFactoryBean().getObject().getManagementService();
    }
}
