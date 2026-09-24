package ChickenMayoDeopbab.bada.global.jwt;

import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.domain.user.service.UserAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;
    private final UserAccessPolicy userAccessPolicy;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        userAccessPolicy.ensureCanAccess(user);

        return new MemberDetails(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(id.toString()));
        userAccessPolicy.ensureCanAccess(user);

        return new MemberDetails(user);
    }
}
