package Controller;

import Model.UserEntity;
import Repository.UserDaoImpl;
import Services.UserServiceImpl;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.Model;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HomeControllerTest {

    private static ApplicationContext context = new FileSystemXmlApplicationContext(new String[]{"classpath:/config.xml"});

    private static String SCRIPT_FILEPATH = "./src/test/resources/schema.sql";

    private JdbcTemplate template;

    private UserServiceImpl service;

    private UserDaoImpl dao;


    @BeforeEach
    void setUp() throws FileNotFoundException {
        template = (JdbcTemplate) context.getBean("H2Template");
        dao = new UserDaoImpl(template);
        service = new UserServiceImpl(dao);
        FileReader reader = new FileReader(SCRIPT_FILEPATH);
        try (Connection conn = template.getDataSource().getConnection()) {
            RunScript.execute(conn, reader);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void printHello() {
        HomeController test = new HomeController();
        String actual = test.PrintHello();
        assertEquals("HomePage", actual);
    }

    @Test
    void save() {
        UserEntity user = new UserEntity(1, "Ashish", "Male", "Ahmedabad");
        service.save(user);
    }

    @Test
    void getAllUser() {
        List<UserEntity> list = service.getAllUser();
        Assertions.assertEquals(13, list.size());
    }

    @Test
    void edit() {
        int id = 1;
        UserEntity user = service.getUserByID(id);
        Assertions.assertEquals("Ashish", user.getName());
        Assertions.assertEquals("Male", user.getGender());
        Assertions.assertEquals("Ahmedabad", user.getAddress());
    }

    @Test
    void editSave() {
        int id = 1;
        UserEntity user = new UserEntity(id, "Ashish", "Male", "Palanpur");
        service.update(user);
        Assertions.assertEquals(1, user.getId());
        Assertions.assertEquals("Ashish", user.getName());
        Assertions.assertEquals("Male", user.getGender());
        Assertions.assertEquals("Palanpur", user.getAddress());

    }

    @Test
    void deleteUser() {
        int id = 1;
        int aa = service.deleteUser(id);
    }

}