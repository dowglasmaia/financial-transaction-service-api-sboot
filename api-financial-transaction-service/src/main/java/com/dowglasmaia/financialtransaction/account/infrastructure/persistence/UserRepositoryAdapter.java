package com.dowglasmaia.financialtransaction.account.infrastructure.persistence;

import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository users;

    UserRepositoryAdapter(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public Optional<User> findByExternalSubject(String externalSubject) {
        return users.findByExternalSubject(externalSubject).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public User save(User user) {
        return toDomain(users.save(toEntity(user)));
    }

    static User toDomain(UserEntity entity) {
        return entity == null ? null : User.restore(entity.getId(), entity.getName(), entity.getCPF(), entity.getEmail(),
              entity.getAreaCode(), entity.getPhoneNumber(), entity.getExternalSubject());
    }

    static UserEntity toEntity(User user) {
        return user == null ? null : UserEntity.builder()
              .id(user.getId())
              .name(user.getName())
              .CPF(user.getCpf())
              .email(user.getEmail())
              .areaCode(user.getAreaCode())
              .phoneNumber(user.getPhoneNumber())
              .externalSubject(user.getExternalSubject())
              .build();
    }
}
