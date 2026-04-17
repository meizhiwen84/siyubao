package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserNoMigrationService implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(UserNoMigrationService.class);

    private final AppUserRepository userRepository;
    private final UserNoService userNoService;

    public UserNoMigrationService(AppUserRepository userRepository, UserNoService userNoService) {
        this.userRepository = userRepository;
        this.userNoService = userNoService;
    }

    @Override
    public void run(String... args) {
        migrateExistingUsers();
    }

    private void migrateExistingUsers() {
        List<AppUser> usersWithoutUserNo = userRepository.findAll();
        int migratedCount = 0;

        for (AppUser user : usersWithoutUserNo) {
            if (user.getUserNo() == null || user.getUserNo().trim().isEmpty()) {
                try {
                    String userNo = userNoService.generateUniqueUserNo();
                    user.setUserNo(userNo);
                    userRepository.save(user);
                    migratedCount++;
                    logger.info("Migrated user: {} - userNo: {}", user.getUsername(), userNo);
                } catch (Exception e) {
                    logger.error("Failed to migrate user: {}", user.getUsername(), e);
                }
            }
        }

        if (migratedCount > 0) {
            logger.info("UserNo migration completed. Migrated {} users.", migratedCount);
        }
    }
}
