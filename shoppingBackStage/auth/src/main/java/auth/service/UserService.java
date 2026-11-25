package auth.service;

import model.User;

public interface UserService {
    void register(User user);
    User findByUsername(String username);
    User findByPhone(String phone);
    User findByEmail(String email);
    User findById(Long id);
    void updateProfile(User user);
    void updatePassword(Long id, String oldPwd, String newPwd);
}
