package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.vo.UserSignInRecordsVo;
import com.chushi.aiinterview.commons.vo.UserSignInStatVo;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.UserSignInService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;

@Service
public class UserSignInServiceImpl implements UserSignInService {
    private static final String USER_SIGN_IN_KEY_PREFIX = "user:signins:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public UserSignInStatVo signIn(User currentUser) {
        var today = LocalDate.now();
        var key = buildSignInKey(currentUser.getId(), today.getYear());
        var offset = resolveDayOffset(today);
        // 直接把今天对应的 bit 位置 1，重复签到不会产生脏数据
        stringRedisTemplate.opsForValue().setBit(key, offset, true);
        return buildSignInStat(currentUser, today);
    }

    @Override
    public UserSignInStatVo getSignInStat(User currentUser) {
        return buildSignInStat(currentUser, LocalDate.now());
    }

    @Override
    public UserSignInRecordsVo getSignInRecords(User currentUser, Integer year) {
        var signedDates = new ArrayList<String>();
        var date = LocalDate.of(year, 1, 1);
        var maxDay = Year.of(year).length();
        var key = buildSignInKey(currentUser.getId(), year);

        // 逐天读取位图，把已签到日期整理成前端可直接使用的日期列表
        for (int day = 0; day < maxDay; day++) {
            if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue().getBit(key, day))) {
                signedDates.add(date.plusDays(day).toString());
            }
        }
        return new UserSignInRecordsVo(year, signedDates);
    }

    private UserSignInStatVo buildSignInStat(User currentUser, LocalDate today) {
        var signedToday = hasSignedInOn(currentUser.getId(), today);
        var currentStreak = calculateCurrentStreak(currentUser.getId(), today);
        var currentMonthSignDays = countCurrentMonthSignDays(currentUser.getId(), today);
        var totalSignDays = countTotalSignDays(currentUser, today.getYear());
        return new UserSignInStatVo(signedToday, currentStreak, currentMonthSignDays, totalSignDays);
    }

    private Boolean hasSignedInOn(Long userId, LocalDate date) {
        var key = buildSignInKey(userId, date.getYear());
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().getBit(key, resolveDayOffset(date)));
    }

    private Integer calculateCurrentStreak(Long userId, LocalDate today) {
        var streak = 0;
        var date = today;
        // 从今天往前回溯，直到遇到第一天未签到为止
        while (hasSignedInOn(userId, date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private Integer countTotalSignDays(User currentUser, Integer currentYear) {
        var joinYear = currentUser.getJoinTime() == null ? currentYear : currentUser.getJoinTime().getYear();
        var total = 0L;
        // 逐年累计位图中的 1 的数量，避免把所有日期明细都加载到内存里
        for (int year = joinYear; year <= currentYear; year++) {
            total += bitCount(buildSignInKey(currentUser.getId(), year));
        }
        return Math.toIntExact(total);
    }

    private Integer countCurrentMonthSignDays(Long userId, LocalDate today) {
        var count = 0;
        var date = today.withDayOfMonth(1);
        while (!date.isAfter(today)) {
            if (hasSignedInOn(userId, date)) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    private Long bitCount(String key) {
        return stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                connection.stringCommands().bitCount(key.getBytes(StandardCharsets.UTF_8)));
    }

    private String buildSignInKey(Long userId, Integer year) {
        return USER_SIGN_IN_KEY_PREFIX + year + ":" + userId;
    }

    private int resolveDayOffset(LocalDate date) {
        return date.getDayOfYear() - 1;
    }
}
