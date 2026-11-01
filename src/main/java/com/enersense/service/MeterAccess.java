package com.enersense.service;

import com.enersense.domain.Meter;
import com.enersense.repository.MeterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place ownership is checked.
 *
 * <p>Every service funnels through here, so there is one definition of "this consumer may
 * see this meter" rather than the same check copied into each one — where one copy
 * eventually drifts.
 */
@Service
@Transactional(readOnly = true)
public class MeterAccess {

    private final MeterRepository meters;

    public MeterAccess(MeterRepository meters) {
        this.meters = meters;
    }

    public Meter require(String consumerId, String meterId) {
        return meters.findOwned(consumerId, meterId)
                .orElseThrow(() -> new MeterNotFoundException(consumerId, meterId));
    }
}
