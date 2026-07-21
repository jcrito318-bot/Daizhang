package com.company.daizhang.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.system.dto.AccountSetSortItem;
import com.company.daizhang.module.system.entity.UserAccountSetPreference;
import com.company.daizhang.module.system.mapper.UserAccountSetPreferenceMapper;
import com.company.daizhang.module.system.service.UserAccountSetPreferenceService;
import com.company.daizhang.module.system.vo.AccountSetPreferenceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户账套偏好服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountSetPreferenceServiceImpl
        extends ServiceImpl<UserAccountSetPreferenceMapper, UserAccountSetPreference>
        implements UserAccountSetPreferenceService {

    private final AccountSetMapper accountSetMapper;

    @Override
    public List<AccountSetPreferenceVO> listPreferences(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        // 收藏在前,其次按最近访问时间倒序
        LambdaQueryWrapper<UserAccountSetPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSetPreference::getUserId, userId)
                .orderByDesc(UserAccountSetPreference::getIsFavorite)
                .orderByDesc(UserAccountSetPreference::getLastAccessedAt);
        List<UserAccountSetPreference> list = this.list(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查询账套名称,过滤已(逻辑)删除的账套
        Set<Long> accountSetIds = list.stream()
                .map(UserAccountSetPreference::getAccountSetId)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = accountSetMapper.selectBatchIds(accountSetIds).stream()
                .collect(Collectors.toMap(AccountSet::getId, AccountSet::getName, (a, b) -> a));
        return list.stream()
                .filter(p -> nameMap.containsKey(p.getAccountSetId()))
                .map(p -> {
                    AccountSetPreferenceVO vo = new AccountSetPreferenceVO();
                    vo.setAccountSetId(p.getAccountSetId());
                    vo.setAccountSetName(nameMap.get(p.getAccountSetId()));
                    vo.setIsFavorite(p.getIsFavorite());
                    vo.setLastAccessedAt(p.getLastAccessedAt());
                    vo.setAccessCount(p.getAccessCount());
                    vo.setSortOrder(p.getSortOrder());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 异步记录访问:不阻塞主流程,内部吞掉所有异常。
     * <p>
     * 不加 @Transactional: update 与 insert 各自自动提交,
     * 通过唯一键 uk_user_account 捕获并发插入冲突,避免 @Async 与 @Transactional 同方法时的代理顺序歧义。
     */
    @Async("preferenceAsyncExecutor")
    @Override
    public void recordAccess(Long userId, Long accountSetId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            // 先尝试更新(自增访问次数 + 刷新最近访问时间)
            LambdaUpdateWrapper<UserAccountSetPreference> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(UserAccountSetPreference::getUserId, userId)
                    .eq(UserAccountSetPreference::getAccountSetId, accountSetId)
                    .set(UserAccountSetPreference::getLastAccessedAt, now)
                    .set(UserAccountSetPreference::getUpdateTime, now)
                    .setSql("access_count = access_count + 1");
            int affected = baseMapper.update(null, wrapper);
            if (affected == 0) {
                // 记录不存在,插入
                UserAccountSetPreference pref = new UserAccountSetPreference();
                pref.setUserId(userId);
                pref.setAccountSetId(accountSetId);
                pref.setIsFavorite(0);
                pref.setLastAccessedAt(now);
                pref.setAccessCount(1);
                pref.setSortOrder(0);
                try {
                    baseMapper.insert(pref);
                } catch (DuplicateKeyException e) {
                    // 并发插入冲突:另一线程已插入,改为更新
                    baseMapper.update(null, wrapper);
                }
            }
        } catch (Exception e) {
            // 记录访问为辅助功能,任何异常都不影响账套切换主流程
            log.warn("记录账套访问偏好失败, userId={}, accountSetId={}", userId, accountSetId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long userId, Long accountSetId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<UserAccountSetPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSetPreference::getUserId, userId)
                .eq(UserAccountSetPreference::getAccountSetId, accountSetId);
        UserAccountSetPreference existing = this.getOne(wrapper);
        if (existing == null) {
            // 首次操作(收藏):插入一条 isFavorite=1 的记录
            UserAccountSetPreference pref = new UserAccountSetPreference();
            pref.setUserId(userId);
            pref.setAccountSetId(accountSetId);
            pref.setIsFavorite(1);
            pref.setAccessCount(0);
            pref.setSortOrder(0);
            try {
                baseMapper.insert(pref);
                return true;
            } catch (DuplicateKeyException e) {
                // 并发:另一线程已插入,改为查询后更新
                existing = this.getOne(wrapper);
                if (existing == null) {
                    return false;
                }
            }
        }
        int newFav = (existing.getIsFavorite() != null && existing.getIsFavorite() == 1) ? 0 : 1;
        LambdaUpdateWrapper<UserAccountSetPreference> update = new LambdaUpdateWrapper<>();
        update.eq(UserAccountSetPreference::getUserId, userId)
                .eq(UserAccountSetPreference::getAccountSetId, accountSetId)
                .set(UserAccountSetPreference::getIsFavorite, newFav)
                .set(UserAccountSetPreference::getUpdateTime, now);
        baseMapper.update(null, update);
        return newFav == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(Long userId, List<AccountSetSortItem> items) {
        if (userId == null || items == null || items.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (AccountSetSortItem item : items) {
            if (item.getAccountSetId() == null) {
                continue;
            }
            LambdaUpdateWrapper<UserAccountSetPreference> update = new LambdaUpdateWrapper<>();
            update.eq(UserAccountSetPreference::getUserId, userId)
                    .eq(UserAccountSetPreference::getAccountSetId, item.getAccountSetId())
                    .set(UserAccountSetPreference::getSortOrder, item.getSortOrder())
                    .set(UserAccountSetPreference::getUpdateTime, now);
            baseMapper.update(null, update);
        }
    }
}
