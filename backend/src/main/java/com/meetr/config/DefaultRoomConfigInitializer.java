package com.meetr.config;

import com.meetr.domain.entity.RoomConfig;
import com.meetr.domain.enums.RoomStatus;
import com.meetr.domain.repository.RoomConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DefaultRoomConfigInitializer {

    private final RoomConfigRepository roomConfigRepository;

    @Bean
    public ApplicationRunner defaultRoomConfigRunner() {
        return args -> roomConfigRepository.findFirstByRoomIdIsNull()
            .orElseGet(() -> roomConfigRepository.save(defaultConfig()));
    }

    private RoomConfig defaultConfig() {
        RoomConfig config = new RoomConfig();
        config.setRoomId(null);
        config.setResolution(1800);
        config.setDefaultDuration(60);
        config.setMorningStarts("08:00");
        config.setEveningEnds("22:00");
        config.setMinBookAheadMinutes(0);
        config.setMaxBookAheadDays(30);
        config.setMinDurationMinutes(15);
        config.setMaxDurationMinutes(480);
        config.setMaxPerDay(3);
        config.setMaxPerWeek(10);
        config.setApprovalRequired(Boolean.FALSE);
        config.setStatus(RoomStatus.ENABLED);
        return config;
    }
}
