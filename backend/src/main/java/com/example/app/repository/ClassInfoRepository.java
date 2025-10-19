package com.example.app.repository;

import com.example.app.entity.ClassInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 班级数据访问层接口
 * 继承JpaRepository，提供班级实体的CRUD操作
 */
@Repository
public interface ClassInfoRepository extends JpaRepository<ClassInfo, Long> {
    
    /**
     * 根据班级名称查询班级
     * @param className 班级名称
     * @return 班级信息
     */
    Optional<ClassInfo> findByClassName(String className);
}