package ChickenMayoDeopbab.bada.domain.ringtone.service;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.response.RingtoneResponse;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.request.UpdateRingtoneSettingRequest;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.response.RingtoneSettingResponse;
import ChickenMayoDeopbab.bada.domain.ringtone.entity.RingtoneSetting;
import ChickenMayoDeopbab.bada.domain.ringtone.repository.RingtoneSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class RingtoneService {

    private final FileRepository fileRepository;
    private final FileService fileService;
    private final UsersRepository usersRepository;
    private final RingtoneSettingRepository ringtoneSettingRepository;

    public RingtoneResponse upload(MultipartFile file) {
        return RingtoneResponse.from(fileService.upload(file, FileType.RINGTONE));
    }

    @Transactional(readOnly = true)
    public List<RingtoneResponse> list() {
        Users user = getUserInfo();
        return fileRepository.findAllByUserIdAndFileTypeOrderByIdDesc(user.getUserId(), FileType.RINGTONE)
                .stream()
                .map(file -> RingtoneResponse.from(file, fileService.generatePresignedUrl(file.getS3Key())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RingtoneResponse get(Long fileId) {
        File file = getOwnedRingtone(fileId, getUserInfo());
        return RingtoneResponse.from(file, fileService.generatePresignedUrl(file.getS3Key()));
    }

    public void delete(Long fileId) {
        Users user = getUserForUpdate();
        File file = getOwnedRingtone(fileId, user);
        ringtoneSettingRepository.findById(user.getUserId()).ifPresent(setting -> {
            if (setting.getSelectedFile() != null && setting.getSelectedFile().getId().equals(fileId)) {
                setting.select(null);
                ringtoneSettingRepository.saveAndFlush(setting);
            }
        });
        fileService.deleteRingtone(file);
    }

    @Transactional(readOnly = true)
    public RingtoneSettingResponse getSetting() {
        Users user = getUserInfo();
        return ringtoneSettingRepository.findById(user.getUserId())
                .map(this::settingResponse)
                .orElseGet(RingtoneSettingResponse::defaultSetting);
    }

    public RingtoneSettingResponse updateSetting(UpdateRingtoneSettingRequest request) {
        Users user = getUserForUpdate();
        File file = request.fileId() == null ? null : getOwnedRingtone(request.fileId(), user);
        RingtoneSetting setting = ringtoneSettingRepository.findById(user.getUserId())
                .orElseGet(() -> RingtoneSetting.defaultFor(user));
        setting.select(file);
        ringtoneSettingRepository.saveAndFlush(setting);
        return settingResponse(setting);
    }

    private RingtoneSettingResponse settingResponse(RingtoneSetting setting) {
        File file = setting.getSelectedFile();
        return file == null ? RingtoneSettingResponse.defaultSetting()
                : RingtoneSettingResponse.selected(
                        RingtoneResponse.from(file, fileService.generatePresignedUrl(file.getS3Key()))
                );
    }

    private Users getUserForUpdate() {
        // 같은 계정의 첫 설정 저장과 음원 삭제를 직렬화해 중복 생성과 삭제된 파일 선택을 막는다.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsernameForRingtoneUpdate(auth.getName())
                .orElseThrow(() -> ApplicationException.of(UsersStatusCode.USER_NOT_FOUND));
    }

    private File getOwnedRingtone(Long fileId, Users user) {
        return fileRepository.findByIdAndUserIdAndFileType(fileId, user.getUserId(), FileType.RINGTONE)
                .orElseThrow(() -> ApplicationException.of(FileStatusCode.FILE_NOT_FOUND));
    }

    private Users getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return usersRepository.findByUsername(auth.getName())
                .orElseThrow(() -> ApplicationException.of(UsersStatusCode.USER_NOT_FOUND));
    }
}
