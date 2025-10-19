-- 测试数据：班级
INSERT INTO class_info (class_name, grade, major) VALUES
('计科2301班', '2023级', '计算机科学与技术'),
('软工2302班', '2023级', '软件工程'),
('大数据2401班', '2024级', '数据科学与大数据技术');

-- 测试数据：课程
INSERT INTO course (course_name, credits, description) VALUES
('高等数学', 5, '微积分、线性代数基础'),
('数据结构', 4, '线性表、树、图与算法分析'),
('Java程序设计', 3, '面向对象编程与Spring框架入门'),
('数据库原理', 3, '关系模型、SQL与事务'),
('操作系统', 4, '进程、内存与文件系统');

-- 测试数据：学生
INSERT INTO student (student_number, name, gender, birth_date, email, phone, class_id) VALUES
('20230101', '张伟', '男', '2005-03-12', 'zhangwei@example.com', '13800000001', (SELECT id FROM class_info WHERE class_name='计科2301班')),
('20230102', '李娜', '女', '2005-07-25', 'lina@example.com', '13800000002', (SELECT id FROM class_info WHERE class_name='计科2301班')),
('20230103', '王强', '男', '2004-11-08', 'wangqiang@example.com', '13800000003', (SELECT id FROM class_info WHERE class_name='计科2301班')),
('20230201', '刘洋', '男', '2005-01-30', 'liuyang@example.com', '13800000004', (SELECT id FROM class_info WHERE class_name='软工2302班')),
('20230202', '陈静', '女', '2005-09-17', 'chenjing@example.com', '13800000005', (SELECT id FROM class_info WHERE class_name='软工2302班')),
('20240101', '赵敏', '女', '2006-05-02', 'zhaomin@example.com', '13800000006', (SELECT id FROM class_info WHERE class_name='大数据2401班')),
('20240102', '孙磊', '男', '2006-02-14', 'sunlei@example.com', '13800000007', (SELECT id FROM class_info WHERE class_name='大数据2401班'));

-- 测试数据：选课与成绩
INSERT INTO student_course (student_id, course_id, grade) VALUES
((SELECT id FROM student WHERE student_number='20230101'), (SELECT id FROM course WHERE course_name='高等数学'), 88.5),
((SELECT id FROM student WHERE student_number='20230101'), (SELECT id FROM course WHERE course_name='数据结构'), 92.0),
((SELECT id FROM student WHERE student_number='20230102'), (SELECT id FROM course WHERE course_name='高等数学'), 95.0),
((SELECT id FROM student WHERE student_number='20230102'), (SELECT id FROM course WHERE course_name='Java程序设计'), 89.0),
((SELECT id FROM student WHERE student_number='20230103'), (SELECT id FROM course WHERE course_name='数据结构'), 76.5),
((SELECT id FROM student WHERE student_number='20230201'), (SELECT id FROM course WHERE course_name='数据库原理'), 84.0),
((SELECT id FROM student WHERE student_number='20230202'), (SELECT id FROM course WHERE course_name='操作系统'), 91.5),
((SELECT id FROM student WHERE student_number='20230202'), (SELECT id FROM course WHERE course_name='数据库原理'), 87.0),
((SELECT id FROM student WHERE student_number='20240101'), (SELECT id FROM course WHERE course_name='高等数学'), 79.0),
((SELECT id FROM student WHERE student_number='20240102'), (SELECT id FROM course WHERE course_name='Java程序设计'), 93.5);
