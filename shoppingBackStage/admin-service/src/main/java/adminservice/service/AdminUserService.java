package adminservice.service;

import adminservice.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    @Autowired
    private AdminMapper adminMapper;

    @Transactional
    public boolean deleteUser(Long id) {
        if (adminMapper.countUserById(id) <= 0) {
            return false;
        }
        adminMapper.deleteAfterSaleByUser(id);
        adminMapper.deleteReviewsByUser(id);
        adminMapper.deleteOrdersByUser(id);
        adminMapper.deleteUser(id);
        return true;
    }
}
