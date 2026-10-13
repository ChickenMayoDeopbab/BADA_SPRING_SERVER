package ChickenMayoDeopbab.bada.domain.ringtone.service;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.request.UpdateRingtoneSettingRequest;
import ChickenMayoDeopbab.bada.domain.ringtone.entity.RingtoneSetting;
import ChickenMayoDeopbab.bada.domain.ringtone.repository.RingtoneSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RingtoneServiceTest {

    private final FileRepository files = mock(FileRepository.class);
    private final FileService storage = mock(FileService.class);
    private final UsersRepository users = mock(UsersRepository.class);
    private final RingtoneSettingRepository settings = mock(RingtoneSettingRepository.class);
    private final RingtoneService service = new RingtoneService(files, storage, users, settings);
    private final Users user = mock(Users.class);
    private final File file = ringtone(12L);

    @BeforeEach
    void setUp() {
        when(user.getUserId()).thenReturn(7L);
        when(users.findByUsername("owner")).thenReturn(Optional.of(user));
        when(users.findByUsernameForRingtoneUpdate("owner")).thenReturn(Optional.of(user));
        when(storage.generatePresignedUrl(file.getS3Key())).thenReturn("https://example.com/fresh");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner", null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listQueriesOnlyOwnedRingtones() {
        when(files.findAllByUserIdAndFileTypeOrderByIdDesc(7L, FileType.RINGTONE)).thenReturn(List.of(file));

        assertThat(service.list()).singleElement().satisfies(response -> {
            assertThat(response.fileId()).isEqualTo(12L);
            assertThat(response.url()).isEqualTo("https://example.com/fresh");
        });
    }

    @Test
    void getRejectsMissingOtherUsersAndNonRingtoneFiles() {
        assertNotFound(() -> service.get(12L));
        verify(files).findByIdAndUserIdAndFileType(12L, 7L, FileType.RINGTONE);
        verifyNoInteractions(storage);
    }

    @Test
    void firstReadUsesDefaultWithoutCreatingSetting() {
        assertThat(service.getSetting().defaultRingtone()).isTrue();
        verify(settings, never()).saveAndFlush(any());
        verifyNoInteractions(storage);
    }

    @Test
    void selectionRequiresOwnedRingtoneBeforeWritingSetting() {
        assertNotFound(() -> service.updateSetting(new UpdateRingtoneSettingRequest(12L)));
        verifyNoInteractions(settings, storage);
    }

    @Test
    void selectionPersistsFileReferenceAndReturnsFreshUrl() {
        when(files.findByIdAndUserIdAndFileType(12L, 7L, FileType.RINGTONE)).thenReturn(Optional.of(file));

        var response = service.updateSetting(new UpdateRingtoneSettingRequest(12L));

        assertThat(response.defaultRingtone()).isFalse();
        assertThat(response.fileId()).isEqualTo(12L);
        assertThat(response.url()).isEqualTo("https://example.com/fresh");
        verify(settings).saveAndFlush(argThat(setting -> setting.getSelectedFile() == file && setting.getUser() == user));
        verify(users).findByUsernameForRingtoneUpdate("owner");
    }

    @Test
    void settingReadRegeneratesUrlInsteadOfPersistingExpiringUrl() {
        RingtoneSetting setting = selectedSetting(file);
        when(settings.findById(7L)).thenReturn(Optional.of(setting));
        when(storage.generatePresignedUrl(file.getS3Key())).thenReturn("first-url", "second-url");

        assertThat(service.getSetting().url()).isEqualTo("first-url");
        assertThat(service.getSetting().url()).isEqualTo("second-url");
        assertThat(setting.getSelectedFile()).isSameAs(file);
    }

    @Test
    void nullSelectionRestoresDefaultWithoutDeletingUploadedFile() {
        RingtoneSetting setting = selectedSetting(file);
        when(settings.findById(7L)).thenReturn(Optional.of(setting));

        var response = service.updateSetting(new UpdateRingtoneSettingRequest(null));

        assertThat(response.defaultRingtone()).isTrue();
        assertThat(response.fileId()).isNull();
        assertThat(setting.getSelectedFile()).isNull();
        verifyNoInteractions(files, storage);
    }

    @Test
    void deletingSelectedRingtoneRestoresDefaultBeforeFileDeletion() {
        RingtoneSetting setting = selectedSetting(file);
        when(settings.findById(7L)).thenReturn(Optional.of(setting));
        when(files.findByIdAndUserIdAndFileType(12L, 7L, FileType.RINGTONE)).thenReturn(Optional.of(file));

        service.delete(12L);

        assertThat(setting.getSelectedFile()).isNull();
        var order = inOrder(settings, storage);
        order.verify(settings).saveAndFlush(setting);
        order.verify(storage).deleteRingtone(file);
        verify(users).findByUsernameForRingtoneUpdate("owner");
    }

    @Test
    void deletingUnselectedRingtoneKeepsCurrentSelection() {
        File selected = ringtone(20L);
        RingtoneSetting setting = selectedSetting(selected);
        when(settings.findById(7L)).thenReturn(Optional.of(setting));
        when(files.findByIdAndUserIdAndFileType(12L, 7L, FileType.RINGTONE)).thenReturn(Optional.of(file));

        service.delete(12L);

        assertThat(setting.getSelectedFile()).isSameAs(selected);
        verify(settings, never()).saveAndFlush(any());
        verify(storage).deleteRingtone(file);
    }

    @Test
    void deletingMissingOrOtherUsersFileDoesNotChangeSettingOrStorage() {
        assertNotFound(() -> service.delete(12L));
        verifyNoInteractions(settings, storage);
    }

    private RingtoneSetting selectedSetting(File selected) {
        RingtoneSetting setting = RingtoneSetting.defaultFor(user);
        setting.select(selected);
        return setting;
    }

    private File ringtone(Long id) {
        File ringtone = File.builder().title("ring.wav").s3Key("ringtone/" + id)
                .fileType(FileType.RINGTONE).userId(7L).build();
        ReflectionTestUtils.setField(ringtone, "id", id);
        return ringtone;
    }

    private void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOf(ApplicationException.class).extracting("statusCode")
                .isEqualTo(FileStatusCode.FILE_NOT_FOUND);
    }
}
