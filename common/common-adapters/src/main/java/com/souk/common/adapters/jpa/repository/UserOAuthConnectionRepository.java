package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.User;
import com.souk.common.domain.UserOAuthConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOAuthConnectionRepository extends JpaRepository<UserOAuthConnection, Long> {

    List<UserOAuthConnection> findByUser(User user);

    List<UserOAuthConnection> findByUser_UserId(Long userId);

    Optional<UserOAuthConnection> findByProviderAndProviderUserId(
            UserOAuthConnection.OAuthProvider provider,
            String providerUserId
    );

    Optional<UserOAuthConnection> findByUser_UserIdAndProvider(
            Long userId,
            UserOAuthConnection.OAuthProvider provider
    );

    boolean existsByProviderAndProviderUserId(
            UserOAuthConnection.OAuthProvider provider,
            String providerUserId
    );

    void deleteByUser_UserIdAndProvider(
            Long userId,
            UserOAuthConnection.OAuthProvider provider
    );
}
