package com.example.app.repository;

import com.example.app.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统用户数据访问层（SPEC-identity）
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * 按用户名查找账号（登录、唯一性校验）
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 按关联学生ID查找账号
     */
    Optional<SysUser> findByStudentId(Long studentId);

    /**
     * 用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 学生ID是否已绑定账号
     */
    boolean existsByStudentId(Long studentId);
}
