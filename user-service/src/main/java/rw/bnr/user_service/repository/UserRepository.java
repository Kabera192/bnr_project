package rw.bnr.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.bnr.user_service.model.User;

public interface UserRepository extends JpaRepository<User, Long>
{
    boolean existsByEmail(String email);
    User findByEmail(String email);

    User findByUsername(String username);
}
