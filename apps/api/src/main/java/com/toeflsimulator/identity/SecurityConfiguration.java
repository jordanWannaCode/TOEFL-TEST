package com.toeflsimulator.identity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import java.util.*;
@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetails(JdbcTemplate jdbc) {
        return email -> jdbc.query("SELECT id,email,password_hash FROM simulator.users WHERE email=?", (rs,n) -> new StudentPrincipal(rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("password_hash")), email.toLowerCase(Locale.ROOT)).stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository contextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean CsrfTokenRepository csrfRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository contexts, CsrfTokenRepository csrf, ObjectMapper json) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/health/**", "/api/v1/auth/csrf", "/api/v1/auth/login", "/error").permitAll()
            .anyRequest().authenticated())
            .securityContext(context -> context.securityContextRepository(contexts))
            .csrf(config -> config.csrfTokenRepository(csrf))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,error) -> writeError(response,json,401,"UNAUTHENTICATED"))
                .accessDeniedHandler((request,response,error) -> writeError(response,json,403,"FORBIDDEN")))
            .logout(logout -> logout.logoutUrl("/api/v1/auth/logout").logoutSuccessHandler((request,response,auth) -> response.setStatus(204)))
            .build();
    }
    private static void writeError(jakarta.servlet.http.HttpServletResponse response, ObjectMapper json, int status, String code) throws java.io.IOException {
        response.setStatus(status); response.setContentType("application/problem+json");
        json.writeValue(response.getOutputStream(), Map.of("type", "urn:toefl:problem:"+code, "title",code,"status",status,"code",code));
    }
}
