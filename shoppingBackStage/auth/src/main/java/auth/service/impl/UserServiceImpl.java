package auth.service.impl;

import auth.mapper.UserMapper;
import auth.service.UserService;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utils.PasswordUtil;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public void register(User user) {
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        userMapper.insert(user);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public void updateProfile(User user) {
        userMapper.updateProfile(user);
    }

    @Override
    public void updatePassword(Long id, String oldPwd, String newPwd) {
        User user = userMapper.findById(id);
        if (user == null || !PasswordUtil.matches(oldPwd, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        userMapper.updatePassword(id, PasswordUtil.encode(newPwd));
    }
}
