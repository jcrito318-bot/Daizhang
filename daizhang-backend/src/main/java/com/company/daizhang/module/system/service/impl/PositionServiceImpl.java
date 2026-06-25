package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.Department;
import com.company.daizhang.module.system.entity.Position;
import com.company.daizhang.module.system.mapper.DepartmentMapper;
import com.company.daizhang.module.system.mapper.PositionMapper;
import com.company.daizhang.module.system.service.PositionService;
import com.company.daizhang.module.system.vo.PositionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    private final DepartmentMapper departmentMapper;

    @Override
    public PageResult<PositionVO> pagePositions(String positionName, String positionCode, Long departmentId,
                                                Integer status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        Page<Position> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(positionName), Position::getPositionName, positionName)
               .like(StrUtil.isNotBlank(positionCode), Position::getPositionCode, positionCode)
               .eq(departmentId != null, Position::getDepartmentId, departmentId)
               .eq(status != null, Position::getStatus, status)
               .orderByAsc(Position::getSortOrder);
        Page<Position> result = this.page(page, wrapper);
        List<PositionVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<PositionVO> listPositions() {
        List<Position> list = this.list();
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPosition(Position entity) {
        // 业务校验：岗位编码不能为空
        if (StrUtil.isBlank(entity.getPositionCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位编码不能为空");
        }
        // 业务校验：岗位名称不能为空
        if (StrUtil.isBlank(entity.getPositionName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位名称不能为空");
        }
        // 检查岗位编码是否已存在
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getPositionCode, entity.getPositionCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位编码已存在");
        }
        // 如果有部门，校验部门是否存在
        if (entity.getDepartmentId() != null) {
            Department department = departmentMapper.selectById(entity.getDepartmentId());
            if (department == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "所属部门不存在");
            }
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        this.save(entity);
        log.info("创建岗位成功，岗位编码: {}, 岗位名称: {}", entity.getPositionCode(), entity.getPositionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePosition(Position entity) {
        if (entity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位ID不能为空");
        }
        Position existing = this.getById(entity.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位不存在");
        }
        // 如果修改了岗位编码，检查是否与其他岗位冲突
        if (StrUtil.isNotBlank(entity.getPositionCode()) && !entity.getPositionCode().equals(existing.getPositionCode())) {
            LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Position::getPositionCode, entity.getPositionCode());
            wrapper.ne(Position::getId, entity.getId());
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位编码已存在");
            }
        }
        // 如果有部门，校验部门是否存在
        if (entity.getDepartmentId() != null) {
            Department department = departmentMapper.selectById(entity.getDepartmentId());
            if (department == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "所属部门不存在");
            }
        }
        this.updateById(entity);
        log.info("更新岗位成功，岗位ID: {}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePosition(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位ID不能为空");
        }
        Position existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位不存在");
        }
        this.removeById(id);
        log.info("删除岗位成功，岗位ID: {}, 岗位编码: {}", id, existing.getPositionCode());
    }

    private PositionVO convertToVO(Position position) {
        PositionVO vo = new PositionVO();
        BeanUtil.copyProperties(position, vo);
        return vo;
    }
}
