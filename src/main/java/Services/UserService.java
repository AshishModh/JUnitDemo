package Services;

import Model.UserEntity;

import java.util.List;

public interface UserService {

    int save(UserEntity userEntity);

    List<UserEntity> getAllUser();

    UserEntity getUserByID(int id);

    int update(UserEntity userEntity);

    int deleteUser(int id);

    int getCountOfUser();
}
