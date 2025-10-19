-- MySQL init script, migrated from H2
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- CLASS_INFO (3 rows)
INSERT INTO `class_info` (`id`, `class_name`, `grade`, `major`) VALUES
(1, '计科2301班', '2023级', '计算机科学与技术'),
(2, '软工2302班', '2023级', '软件工程'),
(3, '大数据2401班', '2024级', '数据科学与大数据技术');

-- COURSE (5 rows)
INSERT INTO `course` (`id`, `course_name`, `credits`, `description`) VALUES
(1, '高等数学', 5, '微积分、线性代数基础'),
(2, '数据结构', 4, '线性表、树、图与算法分析'),
(3, 'Java程序设计', 3, '面向对象编程与Spring框架入门'),
(4, '数据库原理', 3, '关系模型、SQL与事务'),
(5, '操作系统', 4, '进程、内存与文件系统');

-- STUDENT (7 rows)
INSERT INTO `student` (`id`, `birth_date`, `email`, `gender`, `name`, `phone`, `student_number`, `class_id`) VALUES
(1, '2005-03-12', 'zhangwei@example.com', '男', '张伟', '13800000001', '20230101', 1),
(2, '2005-07-25', 'lina@example.com', '女', '李娜', '13800000002', '20230102', 1),
(3, '2004-11-08', 'wangqiang@example.com', '男', '王强', '13800000003', '20230103', 1),
(4, '2005-01-30', 'liuyang@example.com', '男', '刘洋', '13800000004', '20230201', 2),
(5, '2005-09-17', 'chenjing@example.com', '女', '陈静', '13800000005', '20230202', 2),
(6, '2006-05-02', 'zhaomin@example.com', '女', '赵敏', '13800000006', '20240101', 3),
(7, '2006-02-14', 'sunlei@example.com', '男', '孙磊', '13800000007', '20240102', 3);

-- STUDENT_COURSE (10 rows)
INSERT INTO `student_course` (`id`, `grade`, `course_id`, `student_id`) VALUES
(1, 88.5, 1, 1),
(2, 92.0, 2, 1),
(3, 95.0, 1, 2),
(4, 89.0, 3, 2),
(5, 76.5, 2, 3),
(6, 84.0, 4, 4),
(7, 91.5, 5, 5),
(8, 87.0, 4, 5),
(9, 79.0, 1, 6),
(10, 93.5, 3, 7);

SET FOREIGN_KEY_CHECKS = 1;
