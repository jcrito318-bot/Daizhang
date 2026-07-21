package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.AbstractLibraryQueryRequest;
import com.company.daizhang.module.voucher.dto.AbstractLibraryRequest;
import com.company.daizhang.module.voucher.entity.AbstractLibrary;
import com.company.daizhang.module.voucher.vo.AbstractLibraryVO;

import java.util.List;

/**
 * 常用摘要库服务接口
 * <p>
 * 提供常用摘要的增删改查及"使用次数 +1"能力。凭证保存时若使用了摘要库中的摘要,
 * 调用 {@link #incrementUseCount(Long)} 累计使用次数,后续搜索时按使用次数降序排序,
 * 实现智能推荐常用摘要。
 */
public interface AbstractLibraryService extends IService<AbstractLibrary> {

    /**
     * 分页查询常用摘要
     *
     * @param request 查询条件
     * @return 分页结果
     */
    PageResult<AbstractLibraryVO> pageAbstracts(AbstractLibraryQueryRequest request);

    /**
     * 搜索常用摘要(按使用次数 DESC 排序,用于凭证录入页 el-autocomplete 数据源)
     *
     * @param accountSetId 账套ID
     * @param keyword      搜索关键词(模糊匹配摘要文本,可为空表示返回全部)
     * @param limit        返回条数上限
     * @return 摘要列表
     */
    List<AbstractLibraryVO> searchAbstracts(Long accountSetId, String keyword, Integer limit);

    /**
     * 新增常用摘要
     *
     * @param request 摘要请求
     * @return 新建摘要ID
     */
    Long createAbstract(AbstractLibraryRequest request);

    /**
     * 使用次数 +1(凭证保存时调用)
     *
     * @param id 摘要ID
     */
    void incrementUseCount(Long id);

    /**
     * 删除常用摘要
     *
     * @param id 摘要ID
     */
    void deleteAbstract(Long id);
}
