package ChickenMayoDeopbab.bada.domain.ringtone.service;

import ChickenMayoDeopbab.bada.domain.file.entity.File;
import ChickenMayoDeopbab.bada.domain.file.enumeration.FileType;
import ChickenMayoDeopbab.bada.domain.file.exception.FileStatusCode;
import ChickenMayoDeopbab.bada.domain.file.repository.FileRepository;
import ChickenMayoDeopbab.bada.domain.file.service.FileService;
import ChickenMayoDeopbab.bada.domain.file.service.RingtoneAudioFixtures;
import ChickenMayoDeopbab.bada.domain.legalconsent.service.ProfileImageConsentPolicy;
import ChickenMayoDeopbab.bada.domain.ringtone.dto.request.UpdateRingtoneSettingRequest;
import ChickenMayoDeopbab.bada.domain.ringtone.repository.RingtoneSettingRepository;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.datasource.url=${BADA_RINGTONE_TEST_DB_URL}",
        "spring.datasource.username=postgres",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "BADA_RINGTONE_TEST_DB_URL", matches = ".+")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RingtonePersistenceTest {

    @Autowired private FileRepository files;
    @Autowired private UsersRepository users;
    @Autowired private RingtoneSettingRepository settings;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;

    private final S3Client s3 = mock(S3Client.class);
    private final S3Presigner presigner = mock(S3Presigner.class);
    private TransactionTemplate transaction;
    private FileService storage;
    private RingtoneService service;

    @TestConfiguration
    static class TemporaryDatabaseConfiguration {
        @Bean
        DataSource dataSource(Environment environment) {
            String url = environment.getRequiredProperty("BADA_RINGTONE_TEST_DB_URL");
            if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):\\d+/bada_ringtone_test")) {
                throw new IllegalArgumentException("벨소리 영속성 테스트는 로컬 bada_ringtone_test DB만 사용할 수 있습니다.");
            }
            DriverManagerDataSource source = new DriverManagerDataSource();
            source.setDriverClassName("org.postgresql.Driver");
            source.setUrl(url);
            source.setUsername("postgres");
            source.setPassword("");
            return source;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(10);
        storage = new FileService(s3, presigner, files, users, mock(ProfileImageConsentPolicy.class));
        ReflectionTestUtils.setField(storage, "bucket", "ringtone-test");
        service = new RingtoneService(files, storage, users, settings);
        PresignedGetObjectRequest signed = mock(PresignedGetObjectRequest.class);
        when(signed.url()).thenReturn(URI.create("https://example.com/fresh-ringtone").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(signed);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void selectionSurvivesCommitAndCanBeReadByAnotherServiceInstance() {
        Fixture fixture = fixture();
        authenticate(fixture.user().getUsername());
        transaction.executeWithoutResult(status -> service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId())));

        SecurityContextHolder.clearContext();
        authenticate(fixture.user().getUsername());
        RingtoneService anotherInstance = new RingtoneService(files, storage, users, settings);
        var response = transaction.execute(status -> anotherInstance.getSetting());

        assertThat(response.defaultRingtone()).isFalse();
        assertThat(response.fileId()).isEqualTo(fixture.file().getId());
        assertThat(response.url()).isEqualTo("https://example.com/fresh-ringtone");
    }

    @Test
    void concurrentFirstSelectionsCreateOneSetting() throws Exception {
        Fixture fixture = fixture();
        runConcurrently(fixture, () -> service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId())),
                () -> service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId())));

        assertThat(settings.findById(fixture.user().getUserId())).isPresent();
        authenticate(fixture.user().getUsername());
        assertThat(transaction.execute(status -> service.getSetting()).fileId()).isEqualTo(fixture.file().getId());
    }

    @Test
    void concurrentSelectionAndDeletionLeaveDefaultAndNoDanglingReference() throws Exception {
        Fixture fixture = fixture();
        runConcurrently(fixture, () -> {
            try {
                service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId()));
            } catch (ApplicationException exception) {
                // 삭제가 먼저 커밋되면 선택 요청은 정상적으로 404가 된다.
                assertThat(exception.getStatusCode()).isEqualTo(FileStatusCode.FILE_NOT_FOUND);
            }
        }, () -> service.delete(fixture.file().getId()));

        authenticate(fixture.user().getUsername());
        assertThat(transaction.execute(status -> service.getSetting()).defaultRingtone()).isTrue();
        assertThat(files.findById(fixture.file().getId())).isEmpty();
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deletionRollbackPreservesSelectionMetadataAndS3Object() {
        Fixture fixture = fixture();
        authenticate(fixture.user().getUsername());
        transaction.executeWithoutResult(status -> service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId())));

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.delete(fixture.file().getId());
            throw new IllegalStateException("rollback deletion");
        })).isInstanceOf(IllegalStateException.class).hasMessage("rollback deletion");

        assertThat(files.findById(fixture.file().getId())).isPresent();
        assertThat(transaction.execute(status -> service.getSetting()).fileId()).isEqualTo(fixture.file().getId());
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void uploadRollbackRemovesS3ObjectAndDoesNotKeepMetadata() throws Exception {
        Fixture fixture = fixture();
        authenticate(fixture.user().getUsername());
        MockMultipartFile audio = new MockMultipartFile("file", "new.wav", "audio/wav", RingtoneAudioFixtures.wave());

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.upload(audio);
            throw new IllegalStateException("rollback upload");
        })).isInstanceOf(IllegalStateException.class).hasMessage("rollback upload");

        assertThat(files.findAllByUserIdAndFileType(fixture.user().getUserId(), FileType.RINGTONE)).hasSize(1);
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void existingWithdrawalFileCleanupAndUserDeletionAlsoRemoveRingtoneSetting() {
        Fixture fixture = fixture();
        authenticate(fixture.user().getUsername());
        transaction.executeWithoutResult(status -> service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId())));

        transaction.executeWithoutResult(status -> {
            storage.deleteAllByUserId(fixture.user().getUserId());
            users.delete(users.findById(fixture.user().getUserId()).orElseThrow());
            users.flush();
        });

        assertThat(settings.findById(fixture.user().getUserId())).isEmpty();
        assertThat(files.findById(fixture.file().getId())).isEmpty();
        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void withdrawalRollbackKeepsUploadedRingtoneAndSelection() {
        Fixture fixture = fixture();
        authenticate(fixture.user().getUsername());
        transaction.executeWithoutResult(status -> service.updateSetting(new UpdateRingtoneSettingRequest(fixture.file().getId())));

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            storage.deleteAllByUserId(fixture.user().getUserId());
            files.flush();
            throw new IllegalStateException("rollback withdrawal");
        })).isInstanceOf(IllegalStateException.class).hasMessage("rollback withdrawal");

        assertThat(files.findById(fixture.file().getId())).isPresent();
        assertThat(transaction.execute(status -> service.getSetting()).fileId()).isEqualTo(fixture.file().getId());
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void schemaUpgradePreservesExistingTypesAndInstallsDeletionConstraints() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA ringtone_migration_test");
            try {
                statement.execute("SET search_path TO ringtone_migration_test");
                statement.execute("CREATE TABLE users (user_id BIGINT PRIMARY KEY)");
                statement.execute("CREATE TABLE file (file_id BIGINT PRIMARY KEY, file_type VARCHAR(255) "
                        + "CHECK (file_type IN ('PROFILE', 'SCENARIO_PROFILE', 'CALL_FILE', 'COMMUNITY_IMAGE')))");
                statement.execute("INSERT INTO users VALUES (7)");
                statement.execute("INSERT INTO file VALUES (1, 'PROFILE')");
                for (int attempt = 0; attempt < 2; attempt++) {
                    statement.execute("""
                            BEGIN;

                            ALTER TABLE file DROP CONSTRAINT IF EXISTS file_file_type_check;
                            ALTER TABLE file ADD CONSTRAINT file_file_type_check
                                CHECK (file_type IN ('PROFILE', 'SCENARIO_PROFILE', 'CALL_FILE', 'COMMUNITY_IMAGE', 'RINGTONE'));

                            CREATE TABLE IF NOT EXISTS ringtone_settings (
                                user_id BIGINT PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
                                selected_file_id BIGINT REFERENCES file(file_id) ON DELETE SET NULL,
                                updated_at TIMESTAMP NOT NULL
                            );

                            CREATE INDEX IF NOT EXISTS idx_ringtone_settings_selected_file
                                ON ringtone_settings(selected_file_id);

                            COMMIT;
                            """);
                }
                statement.execute("INSERT INTO file VALUES (2, 'RINGTONE')");
                statement.execute("INSERT INTO ringtone_settings VALUES (7, 2, CURRENT_TIMESTAMP)");
                statement.execute("DELETE FROM file WHERE file_id = 2");
                try (var result = statement.executeQuery("SELECT selected_file_id FROM ringtone_settings WHERE user_id = 7")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject(1)).isNull();
                }
                statement.execute("DELETE FROM users WHERE user_id = 7");
                try (var result = statement.executeQuery("SELECT COUNT(*) FROM ringtone_settings")) {
                    result.next();
                    assertThat(result.getInt(1)).isZero();
                }
                try (var result = statement.executeQuery("SELECT file_type FROM file WHERE file_id = 1")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isEqualTo("PROFILE");
                }
            } finally {
                statement.execute("SET search_path TO public");
                statement.execute("DROP SCHEMA ringtone_migration_test CASCADE");
            }
        }
    }

    private Fixture fixture() {
        return transaction.execute(status -> {
            Users user = users.saveAndFlush(Users.builder().username("ringtone-" + UUID.randomUUID()).name("벨소리 검증").build());
            File file = files.saveAndFlush(File.builder().title("ring.wav").userId(user.getUserId())
                    .fileType(FileType.RINGTONE).s3Key("ringtone/" + UUID.randomUUID()).build());
            return new Fixture(user, file);
        });
    }

    private void runConcurrently(Fixture fixture, Runnable first, Runnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(first, second).stream().map(action -> executor.submit(() -> {
                authenticate(fixture.user().getUsername());
                try {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 실행 시작 시간 초과");
                    }
                    transaction.executeWithoutResult(status -> action.run());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) {
                future.get(15, TimeUnit.SECONDS);
            }
        }
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
    }

    private record Fixture(Users user, File file) {
    }
}
