package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.Position;
import com.company.daizhang.module.system.vo.PositionVO;

import java.util.List;

/**
 * 岗位服务接口
 */
public interface PositionService extends IService<Position> {

    /**
     * 分页查询岗位
     */
    PageResult<PositionVO> pagePositions(String positionName, String positionCode, Long departmentId,
                                         Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询所有岗位列表
     */
    List<PositionVO> listPositions();

    /**
     * 创建岗位
     */
    void createPosition(Position entity);

    /**
     * 更新岗位
     */
    void updatePosition(Position entity);

    /**
     * 删除岗位
     */
    void deletePosition(Long id);
}
