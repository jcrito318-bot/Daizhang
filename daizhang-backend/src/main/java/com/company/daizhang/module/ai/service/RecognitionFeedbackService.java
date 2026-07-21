package com.company.daizhang.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.ai.dto.RecognitionFeedbackRequest;
import com.company.daizhang.module.ai.entity.AiRecognitionFeedback;
import com.company.daizhang.module.ai.vo.AutoLearnResultVO;
import com.company.daizhang.module.ai.vo.FeedbackStatsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 识别反馈服务接口
 * <p>
 * 反馈闭环:
 * 1. saveFeedback: 用户提交反馈,作为 few-shot 示例与学习样本
 * 2. getRecentFewShotExamples: 供 GlmAiService 注入 prompt,提升同账套识别准确率
 * 3. autoLearnFromFeedback: 定时/手动触发,聚合高频反馈自动生成规则
 * 4. getFeedbackStats: 反馈采纳率统计,用于评估 AI 建议质量
 */
public interface RecognitionFeedbackService extends IService<AiRecognitionFeedback> {

    /**
     * 保存用户反馈
     * <p>
     * 后端基于 aiSuggested* 与 actual* 自动计算 accepted 取值,不信任前端传入值
     */
    void saveFeedback(RecognitionFeedbackRequest request);

    /**
     * 拉取最近 N 条反馈作为 few-shot 示例(优先取完全采纳的)
     *
     * @param accountSetId 账套ID
     * @param limit        最多返回条数
     */
    List<AiRecognitionFeedback> getRecentFewShotExamples(Long accountSetId, int limit);

    /**
     * 自动学习:扫描最近 30 天反馈,聚合高频描述自动生成账套级规则
     * <p>
     * 触发方式:定时任务(@Scheduled)或手动调用(ADMIN only)
     */
    AutoLearnResultVO autoLearnFromFeedback();

    /**
     * 反馈统计:返回指定账套在 [startDate, endDate] 区间内的采纳率
     */
    FeedbackStatsVO getFeedbackStats(Long accountSetId, LocalDate startDate, LocalDate endDate);
}
