package Controller;

import Model.UserEntity;
import Services.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.ui.Model;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest1 {

    @InjectMocks
    private HomeController test;

    @Mock
    private UserService service;

    @Mock
    private Model model;


    @Test
    void printHelloTest() {
        String actual = test.PrintHello();
        assertEquals("HomePage", actual);
    }

    @Test
    void showFormTest() {
        String actual = test.showForm(model);
        assertEquals("user-form", actual);
    }

    @Test
    void saveTest() {
        UserEntity user = new UserEntity(1, "Ashish", "Male", "Ahmedabad");
        UserEntity user1 = new UserEntity(2, "Parth", "Male", "Ahmedabad");
        UserEntity user2 = new UserEntity(3, "Virat", "Male", "Ahmedabad");
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        list.add(user1);
        list.add(user2);
        Mockito.when(service.save(user)).thenReturn(list.size());
        int actual = service.save(user);
        Assertions.assertEquals(3, actual);
        verify(service, times(1)).save(user);
    }

    @Test
    void getAllUserTest() {
        UserEntity user = new UserEntity(1, "Ashish", "Male", "Ahmedabad");
        UserEntity user1 = new UserEntity(2, "Parth", "Male", "Ahmedabad");
        UserEntity user2 = new UserEntity(3, "Virat", "Male", "Ahmedabad");
        List<UserEntity> list = new ArrayList<>();
        list.add(user);
        list.add(user1);
        list.add(user2);
        Mockito.when(service.getAllUser()).thenReturn(list);
        List<UserEntity> actual = service.getAllUser();
        Assert.assertEquals(list, actual);
        verify(service, times(1)).getAllUser();


    }

    @Test
    void editTest() {
        int id = 1;
        UserEntity user = new UserEntity(id, "Ashish", "Male", "Ahmedabad");
        Mockito.when(service.getUserByID(id)).thenReturn(user);
        UserEntity actual = service.getUserByID(id);
        assertEquals(user, actual);
        verify(service, times(1)).getUserByID(id);
    }

    @Test
    void editSave() {
        UserEntity user = new UserEntity(1, "Ashish", "Male", "Ahmedabad");
        service.update(user);
        user.setName("Parth");
        user.setGender("Male");
        user.setAddress("Ahmedabad");
        verify(service, times(1)).update(user);

    }

    @Test
    void deleteUser() {
        int id = 1;
        service.deleteUser(id);
        verify(service, times(1)).deleteUser(eq(id));
    }

}