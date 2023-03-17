package Repository;

import Model.UserEntity;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


class UserDaoTest {

    private static ApplicationContext context = new FileSystemXmlApplicationContext(new String[]{"classpath:/config.xml"});

    private static String SCRIPT_FILEPATH = "./src/test/resources/schema.sql";

    private JdbcTemplate template;


    @BeforeEach
    void setup() throws FileNotFoundException {
        template = (JdbcTemplate) context.getBean("H2Template");
        FileReader reader = new FileReader(SCRIPT_FILEPATH);
        try (Connection conn = template.getDataSource().getConnection()) {
            RunScript.execute(conn, reader);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void saveTest() {
        UserEntity user = new UserEntity(4, "Ashish", "Male", "Ahmedabad");
        String sql = "INSERT INTO TABLE1 (id, name, gender, address)" + "VALUES (?,?,?,?)";
        template.update(sql, user.getId(), user.getName(), user.getGender(), user.getAddress());
    }

    @Test
    void getAllUserTest() {
        String sql = "SELECT * FROM TABLE1";
        List<UserEntity> list = template.query(sql, new RowMapper<UserEntity>() {
            @Override
            public UserEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                UserEntity user = new UserEntity();
                user.setId(rs.getInt(1));
                user.setName(rs.getString(2));
                user.setGender(rs.getString(2));
                user.setAddress(rs.getString(4));
                return user;
            }
        });
        Assertions.assertEquals(list.size(), 7);
    }

    @Test
    void getUserByIDTest() {
        int id = 1;
        String sql = "SELECT * FROM TABLE1 WHERE id=?";
        UserEntity user = template.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<UserEntity>(UserEntity.class));
        Assertions.assertEquals("Ashish", user.getName());
        Assertions.assertEquals("Male", user.getGender());
        Assertions.assertEquals("Ahmedabad", user.getAddress());
    }

    @Test
    void updateTest() {
        int id = 1;
        UserEntity user = new UserEntity(id, "xyz", "Male", "Ahmedabad");
        String sql = "UPDATE TABLE1 SET name=?,gender=?,address=? WHERE id=1";
        template.update(sql, user.getName(), user.getGender(), user.getAddress());
    }

    @Test
    void deleteUserTest() {
        int id = 1;
        String sql = "DELETE FROM TABLE1 WHERE id=?";
        template.update(sql, id);
    }

}