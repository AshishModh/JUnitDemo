package Services;

import Model.UserEntity;
import Repository.UserDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class UserServiceImpl implements UserService {

    private UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public int save(UserEntity userEntity) {
        return userDao.save(userEntity);
    }

    @Override
    public List<UserEntity> getAllUser() {
        return userDao.getAllUser();
    }

    @Override
    public UserEntity getUserByID(int id) {
        return userDao.getUserByID(id);
    }

    @Override
    public int update(UserEntity userEntity) {
        return userDao.update(userEntity);

    }

    @Override
    public int deleteUser(int id) {
        return userDao.deleteUser(id);
    }

    @Override
    public int getCountOfUser() {
        return userDao.getCountOfUser();
    }


}
