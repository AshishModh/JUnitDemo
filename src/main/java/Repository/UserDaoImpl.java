package Repository;

import Model.UserEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UserDaoImpl implements UserDao {

    private JdbcTemplate template;


    public UserDaoImpl(JdbcTemplate template) {
        this.template = template;
    }

    @Override
    public int save(UserEntity userEntity) {
        String sql = "INSERT INTO TABLE1 (id, name, gender, address)" + "VALUES (?,?,?,?)";
        return template.update(sql, userEntity.getId(), userEntity.getName(), userEntity.getGender(), userEntity.getAddress());

    }

//    @Override
//    public List<UserEntity> getAllUser() {
//        String sql="SELECT * FROM TABLE1";
//        return template.query(sql, new ResultSetExtractor<List<UserEntity>>() {
//            @Override
//            public List<UserEntity> extractData(ResultSet rs) throws SQLException, DataAccessException {
//                List<UserEntity> list=new ArrayList<UserEntity>();
//                while (rs.next()){
//                    UserEntity u = new UserEntity();
//                    u.setId(rs.getInt(1));
//                    u.setName(rs.getString(2));
//                    u.setGender(rs.getString(3));
//                    u.setAddress(rs.getString(4));
//                    list.add(u);
//                }
//                return list;
//            }
//        });
//
//    }

    @Override
    public List<UserEntity> getAllUser() {
        String sql = "SELECT * FROM TABLE1";
        return template.query(sql, new RowMapper<UserEntity>() {
            @Override
            public UserEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                UserEntity user = new UserEntity();
                user.setId(rs.getInt(1));
                user.setName(rs.getString(2));
                user.setGender(rs.getString(3));
                user.setAddress(rs.getString(4));
                return user;
            }
        });
    }


    @Override
    public UserEntity getUserByID(int id) {
        String sql = "SELECT * FROM TABLE1 WHERE id=?";
        return template.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<UserEntity>(UserEntity.class));
    }

    @Override
    public int update(UserEntity userEntity) {
        String sql = "UPDATE TABLE1 SET name=?,gender=?,address=? WHERE id=?";
        return template.update(sql, userEntity.getName(), userEntity.getGender(), userEntity.getAddress(), userEntity.getId());
    }

    @Override
    public int deleteUser(int id) {
        String sql = "DELETE fROM TABLE1 WHERE id=?";
        return template.update(sql, id);

    }

    @Override
    public int getCountOfUser() {
        String sql = "SELECT COUNT(*) FROM TABLE1";
        return template.queryForObject(sql, Integer.class);
    }


}