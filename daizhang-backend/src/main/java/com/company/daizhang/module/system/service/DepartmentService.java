package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.system.entity.Department;
import com.company.daizhang.module.system.vo.DepartmentVO;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 获取部门树
     */
    List<DepartmentVO> getDepartmentTree();

    /**
     * 创建部门
     */
    void createDepartment(Department entity);

    /**
     * 更新部门
     */
    void updateDepartment(Department entity);

    /**
     * 删除部门
     */
    void deleteDepartment(Long id);
}
