package com.toeflsimulator.identity;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
@Profile("local")
public class LocalAccountBootstrap {
    @Bean ApplicationRunner localAccount(JdbcTemplate jdbc, PasswordEncoder encoder,
        @Value("${LOCAL_USER_EMAIL}") String email, @Value("${LOCAL_USER_PASSWORD}") String password) {
        return args -> {
            if (password.length()<16 || password.length()>72) throw new IllegalStateException("Local password must contain 16 to 72 characters.");
            jdbc.update("INSERT INTO simulator.users(id,email,password_hash) VALUES (?,?,?) ON CONFLICT(email) DO NOTHING", UUID.randomUUID(),email.toLowerCase(Locale.ROOT),encoder.encode(password));
        };
    }
}
