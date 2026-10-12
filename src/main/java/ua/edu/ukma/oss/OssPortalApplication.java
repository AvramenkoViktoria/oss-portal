package ua.edu.ukma.oss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the OSS portal: a modular monolith serving the Student Board (SK)
 * and the Student Congregation (KS) of NaUKMA as two tenants of one application.
 */
@SpringBootApplication
public class OssPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(OssPortalApplication.class, args);
    }
}
