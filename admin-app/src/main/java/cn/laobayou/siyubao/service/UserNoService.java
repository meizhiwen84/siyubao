package cn.laobayou.siyubao.service;

import cn.laobayou.siyubao.bean.AppUser;
import cn.laobayou.siyubao.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class UserNoService {
    private static final SecureRandom random = new SecureRandom();
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int LENGTH = 24;

    private final AppUserRepository userRepository;

    public UserNoService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUniqueUserNo() {
        for (int i = 0; i < 100; i++) {
            String userNo = generateUserNo();
            if (userRepository.findByUserNo(userNo).isEmpty()) {
                return userNo;
            }
        }
        throw new RuntimeException("无法生成唯一用户编号，100次尝试均失败");
    }

    private String generateUserNo() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
