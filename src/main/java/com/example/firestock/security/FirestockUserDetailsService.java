package com.example.firestock.security;

import com.example.firestock.domain.primitives.strings.EmailAddress;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService implementation that loads users from the FireStock database.
 */
@Service
public class FirestockUserDetailsService implements UserDetailsService {

    private final UserQuery userQuery;
    private final UserDao userDao;

    public FirestockUserDetailsService(UserQuery userQuery, UserDao userDao) {
        this.userQuery = userQuery;
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        EmailAddress email;
        try {
            email = new EmailAddress(username);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid email format: " + username);
        }

        return userQuery.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Records a successful login by updating the last login timestamp.
     *
     * @param userDetails the authenticated user
     */
    public void recordSuccessfulLogin(FirestockUserDetails userDetails) {
        userDao.updateLastLogin(userDetails.getUserId());
    }
}
