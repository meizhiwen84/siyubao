package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.CardKey;
import cn.laobayou.siyubao.repository.CardKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CardKeyService {
    @Autowired
    private CardKeyRepository cardKeyRepository;

    public Optional<CardKey> findByCode(String code) {
        if (code == null) return Optional.empty();
        String t = code.trim();
        if (t.isEmpty()) return Optional.empty();
        return cardKeyRepository.findByCode(t);
    }

    public boolean isValid(CardKey cardKey) {
        return cardKey != null && Boolean.TRUE.equals(cardKey.getEnabled()) && cardKey.getRemaining() != null && cardKey.getRemaining() > 0;
    }

    @Transactional
    public boolean decrementIfAvailable(String code) {
        if (code == null) return false;
        String t = code.trim();
        if (t.isEmpty()) return false;
        return cardKeyRepository.decrementIfAvailable(t) == 1;
    }

    public CardKey create(String code, int total) {
        CardKey ck = new CardKey();
        ck.setCode(code);
        ck.setTotal(total);
        ck.setRemaining(total);
        ck.setEnabled(true);
        ck.setCreatedAt(LocalDateTime.now());
        ck.setUpdatedAt(LocalDateTime.now());
        return cardKeyRepository.save(ck);
    }

    public Optional<CardKey> update(Long id, Integer total, Integer remaining, Boolean enabled) {
        Optional<CardKey> opt = cardKeyRepository.findById(id);
        if (!opt.isPresent()) return Optional.empty();
        CardKey ck = opt.get();
        if (total != null) ck.setTotal(total);
        if (remaining != null) ck.setRemaining(remaining);
        if (enabled != null) ck.setEnabled(enabled);
        ck.setUpdatedAt(LocalDateTime.now());
        return Optional.of(cardKeyRepository.save(ck));
    }

    public org.springframework.data.domain.Page<CardKey> findAll(org.springframework.data.domain.Pageable pageable) {
        return cardKeyRepository.findAll(pageable);
    }
}

