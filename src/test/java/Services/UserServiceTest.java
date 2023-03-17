package Services;

import Model.UserEntity;

import Repository.UserDaoImpl;


import org.h2.tools.RunScript;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import org.springframework.jdbc.core.JdbcTemplate;


import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class UserServiceTest {

    private static ApplicationContext context = new FileSystemXmlApplicationContext(new String[]{"classpath:/config.xml"});

    private static String SCRIPT_FILEPATH = "./src/test/resources/schema.sql";

    private JdbcTemplate template;

    private UserServiceImpl service;

    private UserDaoImpl dao;

    private UserEntity userEntity;


    @BeforeEach
    void setUp() throws FileNotFoundException {
        template = (JdbcTemplate) context.getBean("H2Template");
        FileReader reader = new FileReader(SCRIPT_FILEPATH);
        try (Connection conn = template.getDataSource().getConnection()) {
            RunScript.execute(conn, reader);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dao = new UserDaoImpl(template);
    }


    @Test
    void saveTest() {
        UserEntity user = new UserEntity(4, "Ashish", "Male", "Ahmedabad");
        service = new UserServiceImpl(dao);
        service.save(user);
        Assertions.assertEquals(4, user.getId());
        Assertions.assertEquals("Ashish", user.getName());
        Assertions.assertEquals("Male", user.getGender());
        Assertions.assertEquals("Ahmedabad", user.getAddress());

    }

    @Test
    void getAllUserTest() {

//        List<UserEntity> expectedList = new ArrayList<>();
//        expectedList.add(new UserEntity(1, "Ashish", "Male", "Ahmedabad"));
//        expectedList.add(new UserEntity(2, "Parth", "Male", "Ahmedabad"));
//        expectedList.add(new UserEntity(3, "Virat", "Male", "Ahmedabad"));
//        int actNum = (int) expectedList.stream().count();
//        System.out.println(actNum);

        service = new UserServiceImpl(dao);
        List<UserEntity> actual = service.getAllUser();
        System.out.println(actual);
        int actNum = (int) actual.stream().count();
        System.out.println(actNum);

        Assertions.assertEquals(7, actNum);

    }

    @Test
    void getUserByIDTest() {
        int id = 1;
        service = new UserServiceImpl(dao);
        UserEntity user = service.getUserByID(id);

        Assertions.assertEquals(1, user.getId());
        Assertions.assertEquals("Ashish", user.getName());
        Assertions.assertEquals("Male", user.getGender());
        Assertions.assertEquals("Ahmedabad", user.getAddress());


    }

    @Test
    void updateTest() {
        UserEntity user = new UserEntity(1, "Rutvik", "Male", "Palanpur");
        service = new UserServiceImpl(dao);
        service.update(user);
        Assertions.assertEquals(1, user.getId());
        Assertions.assertEquals("Rutvik", user.getName());
        Assertions.assertEquals("Male", user.getGender());
        Assertions.assertEquals("Palanpur", user.getAddress());

    }

    @Test
    void deleteUserTest() {
        int id = 1;
        service = new UserServiceImpl(dao);
        service.deleteUser(id);


    }


}