package com.toeflsimulator.common;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class TimeConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
}
