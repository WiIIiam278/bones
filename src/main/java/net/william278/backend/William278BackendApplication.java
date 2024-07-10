package net.william278.backend;

import net.william278.backend.configuration.AppConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;

@EnableConfigurationProperties({
        AppConfiguration.class
})
@SpringBootApplication
@ServletComponentScan
public class William278BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(William278BackendApplication.class, args);
    }
}
