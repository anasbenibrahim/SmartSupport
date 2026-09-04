package me.project.authservice.client;

import me.project.authservice.dto.UserSyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {

    @PostMapping("/api/users/sync")
    ResponseEntity<Void> syncUser(@RequestBody UserSyncRequest request);
}
