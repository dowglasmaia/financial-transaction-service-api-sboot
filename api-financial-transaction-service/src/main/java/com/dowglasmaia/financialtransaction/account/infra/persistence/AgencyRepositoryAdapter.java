package com.dowglasmaia.financialtransaction.account.infra.persistence;

import com.dowglasmaia.financialtransaction.account.domain.Agency;
import com.dowglasmaia.financialtransaction.account.domain.AgencyRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class AgencyRepositoryAdapter implements AgencyRepository {

    private final AgencyJpaRepository agencies;

    AgencyRepositoryAdapter(AgencyJpaRepository agencies) {
        this.agencies = agencies;
    }

    @Override
    public Optional<Agency> findByNumber(String number) {
        return agencies.findByNumber(number).map(AgencyRepositoryAdapter::toDomain);
    }

    @Override
    public Agency save(Agency agency) {
        return toDomain(agencies.save(toEntity(agency)));
    }

    static Agency toDomain(AgencyEntity entity) {
        return entity == null ? null : Agency.restore(entity.getId(), entity.getNumber());
    }

    static AgencyEntity toEntity(Agency agency) {
        return agency == null ? null : AgencyEntity.builder().id(agency.getId()).number(agency.getNumber()).build();
    }
}
