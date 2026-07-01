package com.company.daizhang.module.subject.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.subject.dto.SubjectCreateRequest;
import com.company.daizhang.module.subject.dto.SubjectUpdateRequest;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.subject.service.SubjectService;
import com.company.daizhang.module.subject.vo.SubjectVO;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 科目服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl extends ServiceImpl<SubjectMapper, Subject> implements SubjectService {
    
    private final VoucherDetailMapper voucherDetailMapper;
    
    // 科目编码正则：4位数字开头，可以有下级编码
    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("^\\d{4}(\\d{2})*$");
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultSubjects(Long accountSetId, String accountingStandard) {
        // 检查是否已存在科目，避免重复初始化
        long existingCount = this.count(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId));
        if (existingCount > 0) {
            log.info("账套ID: {} 的科目已存在，跳过初始化", accountSetId);
            return;
        }

        List<Subject> subjects = new ArrayList<>();
        
        // 资产类
        addSubject(subjects, accountSetId, "1001", "库存现金", "资产", 0L, 1, 1, 0, 1, 0, 0);
        addSubject(subjects, accountSetId, "1002", "银行存款", "资产", 0L, 1, 1, 0, 0, 1, 0);
        addSubject(subjects, accountSetId, "1012", "其他货币资金", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1101", "短期投资", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1121", "应收票据", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1122", "应收账款", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1123", "预付账款", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1131", "应收股利", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1132", "应收利息", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1221", "其他应收款", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1401", "材料采购", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1402", "在途物资", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1403", "原材料", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1405", "库存商品", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1411", "周转材料", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1501", "长期债券投资", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1511", "长期股权投资", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1601", "固定资产", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1602", "累计折旧", "资产", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1604", "在建工程", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1701", "无形资产", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1702", "累计摊销", "资产", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1801", "长期待摊费用", "资产", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "1901", "待处理财产损溢", "资产", 0L, 1, 1, 0, 0, 0, 0);
        
        // 负债类
        addSubject(subjects, accountSetId, "2001", "短期借款", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2201", "应付票据", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2202", "应付账款", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2203", "预收账款", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2211", "应付职工薪酬", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2221", "应交税费", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2231", "应付利息", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2232", "应付利润", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2241", "其他应付款", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2501", "长期借款", "负债", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "2502", "长期应付款", "负债", 0L, 1, 2, 0, 0, 0, 0);
        
        // 所有者权益类
        addSubject(subjects, accountSetId, "3001", "实收资本", "所有者权益", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "3002", "资本公积", "所有者权益", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "3101", "盈余公积", "所有者权益", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "3103", "本年利润", "所有者权益", 0L, 1, 2, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "3104", "利润分配", "所有者权益", 0L, 1, 2, 0, 0, 0, 0);
        
        // 成本类
        addSubject(subjects, accountSetId, "4001", "生产成本", "成本", 0L, 1, 1, 0, 0, 0, 0);
        addSubject(subjects, accountSetId, "4101", "制造费用", "成本", 0L, 1, 1, 0, 0, 0, 0);
        
        // 损益类
        addSubject(subjects, accountSetId, "5001", "主营业务收入", "损益", 0L, 1, 2, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5051", "其他业务收入", "损益", 0L, 1, 2, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5111", "投资收益", "损益", 0L, 1, 2, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5301", "营业外收入", "损益", 0L, 1, 2, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5401", "主营业务成本", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5402", "其他业务成本", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5403", "税金及附加", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5601", "销售费用", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5602", "管理费用", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5603", "财务费用", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5711", "营业外支出", "损益", 0L, 1, 1, 0, 0, 0, 1);
        addSubject(subjects, accountSetId, "5801", "所得税费用", "损益", 0L, 1, 1, 0, 0, 0, 1);
        
        this.saveBatch(subjects);
        
        log.info("初始化默认科目成功，账套ID: {}, 会计准则: {}", accountSetId, accountingStandard);
    }
    
    @Override
    public List<SubjectVO> listSubjectsByAccountSetId(Long accountSetId) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId)
               .orderByAsc(Subject::getCode);
        
        List<Subject> subjects = this.list(wrapper);
        return subjects.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public SubjectVO getSubjectById(Long id) {
        Subject subject = this.getById(id);
        if (subject == null) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        return convertToVO(subject);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSubject(SubjectCreateRequest request) {
        // 业务校验：科目编码不能为空
        if (StrUtil.isBlank(request.getSubjectCode())) {
            throw new BusinessException(ErrorCode.SUBJECT_CODE_BLANK);
        }
        // 业务校验：科目名称不能为空
        if (StrUtil.isBlank(request.getSubjectName())) {
            throw new BusinessException(ErrorCode.SUBJECT_NAME_BLANK);
        }
        // 业务校验：科目类别不能为空
        if (StrUtil.isBlank(request.getCategory())) {
            throw new BusinessException(ErrorCode.SUBJECT_CATEGORY_BLANK);
        }
        // 业务校验：科目余额方向不能为空
        if (request.getBalanceDirection() == null) {
            throw new BusinessException(ErrorCode.SUBJECT_BALANCE_DIRECTION_BLANK);
        }
        // 业务校验：科目余额方向必须是1或2
        if (request.getBalanceDirection() != 1 && request.getBalanceDirection() != 2) {
            throw new BusinessException(ErrorCode.SUBJECT_BALANCE_DIRECTION_INVALID);
        }
        
        // 业务校验：科目编码格式校验（4位数字开头，可以有下级编码）
        if (!SUBJECT_CODE_PATTERN.matcher(request.getSubjectCode()).matches()) {
            throw new BusinessException(ErrorCode.SUBJECT_CODE_INVALID);
        }
        
        // 业务校验：科目编码长度校验（最多4级，每级2位）
        int codeLength = request.getSubjectCode().length();
        if (codeLength > 10) { // 4+2+2+2=10
            throw new BusinessException(ErrorCode.SUBJECT_LEVEL_EXCEED);
        }
        
        // 计算科目层级
        int level = (codeLength - 4) / 2 + 1;
        
        // 业务校验：科目层级不能超过4级
        if (level > 4) {
            throw new BusinessException(ErrorCode.SUBJECT_LEVEL_EXCEED);
        }
        
        // 如果有上级科目，校验上级科目
        if (request.getParentId() != null && request.getParentId() > 0) {
            Subject parentSubject = this.getById(request.getParentId());
            if (parentSubject == null) {
                throw new BusinessException(ErrorCode.SUBJECT_PARENT_NOT_FOUND);
            }

            // 校验科目类别是否一致
            if (!parentSubject.getCategory().equals(request.getCategory())) {
                throw new BusinessException(ErrorCode.SUBJECT_CATEGORY_MISMATCH);
            }
            
            // 校验科目编码是否以父科目编码开头
            if (!request.getSubjectCode().startsWith(parentSubject.getCode())) {
                throw new BusinessException(ErrorCode.SUBJECT_CODE_INVALID);
            }
        }
        
        // 检查科目编码是否已存在
        LambdaQueryWrapper<Subject> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Subject::getAccountSetId, request.getAccountSetId())
                   .eq(Subject::getCode, request.getSubjectCode());
        if (this.count(checkWrapper) > 0) {
            throw new BusinessException(ErrorCode.SUBJECT_CODE_DUPLICATE);
        }
        
        Subject subject = new Subject();
        BeanUtil.copyProperties(request, subject);
        subject.setCode(request.getSubjectCode());
        subject.setName(request.getSubjectName());
        subject.setLevel(level);
        subject.setIsAuxiliary(request.getAuxiliaryAccounting());
        if (subject.getParentId() == null) {
            subject.setParentId(0L);
        }
        if (subject.getStatus() == null) {
            subject.setStatus(1);
        }
        this.save(subject);
        
        log.info("创建科目成功，科目编码: {}, 科目名称: {}", subject.getCode(), subject.getName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubject(Long id, SubjectUpdateRequest request) {
        Subject subject = this.getById(id);
        if (subject == null) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        
        // 业务校验：科目名称不能为空
        if (StrUtil.isBlank(request.getSubjectName())) {
            throw new BusinessException(ErrorCode.SUBJECT_NAME_BLANK);
        }
        // 业务校验：科目余额方向必须是1或2
        if (request.getBalanceDirection() != null &&
            request.getBalanceDirection() != 1 && request.getBalanceDirection() != 2) {
            throw new BusinessException(ErrorCode.SUBJECT_BALANCE_DIRECTION_INVALID);
        }

        // 业务校验：已被凭证引用的科目不允许修改余额方向
        // 否则会破坏已记账凭证的借贷语义,导致余额计算方向反转(资产变负债/收入变费用)
        if (request.getBalanceDirection() != null
                && !request.getBalanceDirection().equals(subject.getBalanceDirection())) {
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(VoucherDetail::getSubjectId, id);
            if (voucherDetailMapper.selectCount(detailWrapper) > 0) {
                throw new BusinessException(ErrorCode.SUBJECT_HAS_VOUCHERS.getCode(),
                        "科目已被凭证引用，余额方向不可修改");
            }
        }
        
        BeanUtil.copyProperties(request, subject);
        subject.setName(request.getSubjectName());
        subject.setIsAuxiliary(request.getAuxiliaryAccounting());
        this.updateById(subject);
        
        log.info("更新科目成功，科目ID: {}, 科目编码: {}", id, subject.getCode());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubject(Long id) {
        Subject subject = this.getById(id);
        if (subject == null) {
            throw new BusinessException(ErrorCode.SUBJECT_NOT_FOUND);
        }
        
        // 业务校验：检查是否存在下级科目
        LambdaQueryWrapper<Subject> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Subject::getParentId, id);
        if (this.count(childWrapper) > 0) {
            throw new BusinessException(ErrorCode.SUBJECT_HAS_CHILDREN);
        }
        
        // 业务校验：检查科目是否被凭证使用
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherDetail::getSubjectId, id);
        if (voucherDetailMapper.selectCount(detailWrapper) > 0) {
            throw new BusinessException(ErrorCode.SUBJECT_HAS_VOUCHERS);
        }
        
        this.removeById(id);
        
        log.info("删除科目成功，科目ID: {}, 科目编码: {}", id, subject.getCode());
    }
    
    private void addSubject(List<Subject> list, Long accountSetId, String code, String name,
                            String category, Long parentId, int level, int balanceDirection,
                            int isAuxiliary, int isCash, int isBank, int isCurrent) {
        Subject subject = new Subject();
        subject.setAccountSetId(accountSetId);
        subject.setCode(code);
        subject.setName(name);
        subject.setCategory(category);
        subject.setParentId(parentId);
        subject.setLevel(level);
        subject.setBalanceDirection(balanceDirection);
        subject.setIsAuxiliary(isAuxiliary);
        subject.setIsCash(isCash);
        subject.setIsBank(isBank);
        subject.setIsCurrent(isCurrent);
        subject.setStatus(1);
        list.add(subject);
    }
    
    private SubjectVO convertToVO(Subject subject) {
        SubjectVO vo = new SubjectVO();
        BeanUtil.copyProperties(subject, vo);
        vo.setSubjectCode(subject.getCode());
        vo.setSubjectName(subject.getName());
        vo.setAuxiliaryAccounting(subject.getIsAuxiliary());
        return vo;
    }
}
