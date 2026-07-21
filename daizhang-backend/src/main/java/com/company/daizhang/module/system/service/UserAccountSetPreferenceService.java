package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.system.dto.AccountSetSortItem;
import com.company.daizhang.module.system.entity.UserAccountSetPreference;
import com.company.daizhang.module.system.vo.AccountSetPreferenceVO;

import java.util.List;

/**
 * 用户账套偏好服务接口
 * <p>
 * 用于顶部账套切换器的"最近访问 + 收藏置顶"能力。
 */
public interface UserAccountSetPreferenceService extends IService<UserAccountSetPreference> {

    /**
     * 获取指定用户的账套偏好列表(收藏在前,按 last_accessed_at DESC 排序)。
     * 已删除的账套会被过滤。
     *
     * @param userId 用户ID
     * @return 偏好列表
     */
    List<AccountSetPreferenceVO> listPreferences(Long userId);

    /**
     * 记录账套访问:更新 lastAccessedAt + accessCount++,记录不存在则插入。
     * <p>
     * 异步执行,不阻塞调用方(主流程),内部吞掉异常以保证不影响账套切换。
     *
     * @param userId        用户ID(由调用方在请求线程同步获取后传入,避免异步线程丢失 SecurityContext)
     * @param accountSetId  账套ID
     */
    void recordAccess(Long userId, Long accountSetId);

    /**
     * 切换账套收藏状态(记录不存在则创建)。
     *
     * @param userId        用户ID
     * @param accountSetId  账套ID
     * @return 切换后的收藏状态(true=已收藏)
     */
    boolean toggleFavorite(Long userId, Long accountSetId);

    /**
     * 批量更新排序(仅更新当前用户已存在的偏好记录)。
     *
     * @param userId 用户ID
     * @param items  排序项列表
     */
    void updateSort(Long userId, List<AccountSetSortItem> items);
}
