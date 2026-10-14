package com.restaurant.ddd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class RestaurantApplication {
    public static void main(String[] args) {
        // Keep JDBC/PostgreSQL timestamp handling consistent across developer machines.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(RestaurantApplication.class, args);
    }
}
