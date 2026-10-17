package com.javatechie.respository;

import com.javatechie.entity.DemoUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<DemoUser, Long> {

    List<DemoUser> findByNameContainingIgnoreCase(String name);

    List<DemoUser> findByLocationContainingIgnoreCase(String location);
}
