package com.nexusflow.permission.repository;

import com.nexusflow.permission.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    List<Role> findByScope(String scope);

    List<Role> findByIsSystem(boolean isSystem);
}
