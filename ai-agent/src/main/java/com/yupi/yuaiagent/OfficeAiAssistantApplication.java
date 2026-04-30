package com.yupi.yuaiagent;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {PgVectorStoreAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class OfficeAiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficeAiAssistantApplication.class, args);
    }
}
