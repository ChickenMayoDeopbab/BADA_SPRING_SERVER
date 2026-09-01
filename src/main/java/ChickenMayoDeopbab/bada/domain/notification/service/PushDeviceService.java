package ChickenMayoDeopbab.bada.domain.notification.service;

import ChickenMayoDeopbab.bada.domain.notification.dto.request.RegisterPushDeviceRequest;
import ChickenMayoDeopbab.bada.domain.notification.entity.PushDevice;
import ChickenMayoDeopbab.bada.domain.notification.repository.PushDeviceRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceRepository pushDeviceRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public void register(RegisterPushDeviceRequest request) {
        Users user = getUserInfo();
        Optional<PushDevice> installationDevice =
                pushDeviceRepository.findByInstallationId(request.installationId());
        Optional<PushDevice> tokenDevice = pushDeviceRepository.findByToken(request.token());

        PushDevice target = resolveTarget(installationDevice, tokenDevice, user, request);
        pushDeviceRepository.save(target);
    }

    @Transactional
    public void unregister(String installationId) {
        pushDeviceRepository.deleteByInstallationIdAndUser(installationId, getUserInfo());
    }

    private PushDevice resolveTarget(
            Optional<PushDevice> installationDevice,
            Optional<PushDevice> tokenDevice,
            Users user,
            RegisterPushDeviceRequest request
    ) {
        if (installationDevice.isPresent()) {
            PushDevice target = installationDevice.get();

            // 토큰은 앱 재설치나 계정 전환으로 다른 설치 정보에서 재발급될 수 있다.
            tokenDevice
                    .filter(existing -> !existing.getPushDeviceId().equals(target.getPushDeviceId()))
                    .ifPresent(existing -> {
                        pushDeviceRepository.delete(existing);
                        pushDeviceRepository.flush();
                    });

            target.updateRegistration(
                    user,
                    request.installationId(),
                    request.token(),
                    request.platform()
            );
            return target;
        }

        if (tokenDevice.isPresent()) {
            PushDevice target = tokenDevice.get();
            target.updateRegistration(
                    user,
                    request.installationId(),
                    request.token(),
                    request.platform()
            );
            return target;
        }

        return PushDevice.create(
                user,
                request.installationId(),
                request.token(),
                request.platform()
        );
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ApplicationException(UsersStatusCode.USER_NOT_FOUND));
    }
}
