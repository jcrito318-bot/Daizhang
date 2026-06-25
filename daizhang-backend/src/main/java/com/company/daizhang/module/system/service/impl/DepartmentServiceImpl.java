package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.entity.Department;
import com.company.daizhang.module.system.mapper.DepartmentMapper;
import com.company.daizhang.module.system.mapper.PositionMapper;
import com.company.daizhang.module.system.service.DepartmentService;
import com.company.daizhang.module.system.vo.DepartmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    private final PositionMapper positionMapper;

    @Override
    public List<DepartmentVO> getDepartmentTree() {
        List<Department> allDepartments = this.list();
        return buildDepartmentTree(allDepartments, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDepartment(Department entity) {
        // 业务校验：部门编码不能为空
        if (StrUtil.isBlank(entity.getDeptCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门编码不能为空");
        }
        // 业务校验：部门名称不能为空
        if (StrUtil.isBlank(entity.getDeptName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门名称不能为空");
        }
        // 检查部门编码是否已存在
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeptCode, entity.getDeptCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门编码已存在");
        }
        // 如果有上级部门，校验上级部门是否存在
        if (entity.getParentId() != null && entity.getParentId() > 0) {
            Department parent = this.getById(entity.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "上级部门不存在");
            }
        }
        if (entity.getParentId() == null) {
            entity.setParentId(0L);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        this.save(entity);
        log.info("创建部门成功，部门编码: {}, 部门名称: {}", entity.getDeptCode(), entity.getDeptName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartment(Department entity) {
        if (entity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }
        Department existing = this.getById(entity.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门不存在");
        }
        // 业务校验：上级部门不能选择自身
        if (entity.getParentId() != null && entity.getParentId().equals(entity.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上级部门不能选择自身");
        }
        // 如果修改了部门编码，检查是否与其他部门冲突
        if (StrUtil.isNotBlank(entity.getDeptCode()) && !entity.getDeptCode().equals(existing.getDeptCode())) {
            LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Department::getDeptCode, entity.getDeptCode());
            wrapper.ne(Department::getId, entity.getId());
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "部门编码已存在");
            }
        }
        // 如果有上级部门，校验上级部门是否存在
        if (entity.getParentId() != null && entity.getParentId() > 0) {
            Department parent = this.getById(entity.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "上级部门不存在");
            }
        }
        this.updateById(entity);
        log.info("更新部门成功，部门ID: {}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }
        Department existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门不存在");
        }
        // 检查是否有子部门
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getParentId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在子部门，无法删除");
        }
        // 检查是否有岗位关联
        LambdaQueryWrapper<com.company.daizhang.module.system.entity.Position> posWrapper = new LambdaQueryWrapper<>();
        posWrapper.eq(com.company.daizhang.module.system.entity.Position::getDepartmentId, id);
        if (positionMapper.selectCount(posWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门下存在岗位，无法删除");
        }
        this.removeById(id);
        log.info("删除部门成功，部门ID: {}, 部门编码: {}", id, existing.getDeptCode());
    }

    /**
     * 递归构建部门树
     */
    private List<DepartmentVO> buildDepartmentTree(List<Department> departments, Long parentId) {
        List<DepartmentVO> tree = new ArrayList<>();
        for (Department dept : departments) {
            if (parentId.equals(dept.getParentId())) {
                DepartmentVO vo = convertToVO(dept);
                vo.setChildren(buildDepartmentTree(departments, dept.getId()));
                tree.add(vo);
            }
        }
        // 按排序号排序
        tree.sort((a, b) -> {
            int sortA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sortB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return sortA - sortB;
        });
        return tree;
    }

    private DepartmentVO convertToVO(Department department) {
        DepartmentVO vo = new DepartmentVO();
        BeanUtil.copyProperties(department, vo);
        return vo;
    }
}
