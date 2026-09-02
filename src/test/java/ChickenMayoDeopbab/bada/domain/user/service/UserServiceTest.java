package ChickenMayoDeopbab.bada.domain.user.service;

import ChickenMayoDeopbab.bada.domain.attendance.repository.AttendanceQueryRepository;
import ChickenMayoDeopbab.bada.domain.attendance.repository.AttendanceRepository;
import ChickenMayoDeopbab.bada.domain.callanxiety.repository.CallAnxietyStateRepository;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.CallPhobiaLevel;
import ChickenMayoDeopbab.bada.domain.diagnosis.entity.DiagnosisResult;
import ChickenMayoDeopbab.bada.domain.diagnosis.exception.DiagnosisResultStatusCode;
import ChickenMayoDeopbab.bada.domain.diagnosis.repository.DiagnosisResultRepository;
import ChickenMayoDeopbab.bada.domain.trainingcallschedule.repository.TrainingCallScheduleRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.repository.TrainingRecordRepository;
import ChickenMayoDeopbab.bada.domain.trainingrecord.service.TrainingRecordService;
import ChickenMayoDeopbab.bada.domain.user.dto.response.MyPageResponse;
import ChickenMayoDeopbab.bada.domain.user.entity.Users;
import ChickenMayoDeopbab.bada.domain.user.exception.UsersStatusCode;
import ChickenMayoDeopbab.bada.domain.user.repository.UsersRepository;
import ChickenMayoDeopbab.bada.global.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UsersRepository usersRepository = mock(UsersRepository.class);
    private final DiagnosisResultRepository diagnosisResultRepository = mock(DiagnosisResultRepository.class);
    private final AttendanceQueryRepository attendanceQueryRepository = mock(AttendanceQueryRepository.class);
    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final CallAnxietyStateRepository callAnxietyStateRepository = mock(CallAnxietyStateRepository.class);
    private final TrainingRecordRepository trainingRecordRepository = mock(TrainingRecordRepository.class);
    private final TrainingRecordService trainingRecordService = mock(TrainingRecordService.class);
    private final TrainingCallScheduleRepository trainingCallScheduleRepository =
            mock(TrainingCallScheduleRepository.class);
    private final BCryptPasswordEncoder bCryptPasswordEncoder = mock(BCryptPasswordEncoder.class);

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);

    private final UserService service = new UserService(
            usersRepository,
            diagnosisResultRepository,
            attendanceQueryRepository,
            attendanceRepository,
            callAnxietyStateRepository,
            trainingRecordRepository,
            trainingRecordService,
            trainingCallScheduleRepository,
            bCryptPasswordEncoder,
            redisTemplate
    );

    private final Users user = mock(Users.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null));
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.of(user));
        when(user.getUserId()).thenReturn(7L);
    }

    @Test
    void withdrawDeletesEveryChildRowBeforeTheUser() {
        login();

        service.withdraw();

        InOrder order = inOrder(
                trainingRecordService,
                trainingCallScheduleRepository,
                attendanceRepository,
                callAnxietyStateRepository,
                diagnosisResultRepository,
                usersRepository
        );
        order.verify(trainingRecordService).deleteAllByUser(user);
        order.verify(trainingCallScheduleRepository).deleteAllByUser(user);
        order.verify(attendanceRepository).deleteAllByUser(user);
        order.verify(callAnxietyStateRepository).deleteByUser(user);
        order.verify(diagnosisResultRepository).deleteAllByUser(user);
        order.verify(usersRepository).delete(user);
    }

    @Test
    void withdrawRemovesRefreshToken() {
        login();

        service.withdraw();

        verify(redisTemplate).delete("refreshToken: 7");
    }

    @Test
    void withdrawWithoutMatchingUserThrowsNotFoundAndDeletesNothing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("junha", null));
        when(usersRepository.findByUsername("junha")).thenReturn(Optional.empty());

        assertThatThrownBy(service::withdraw)
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(UsersStatusCode.USER_NOT_FOUND);

        verify(usersRepository, never()).delete(any());
        verifyNoInteractions(
                trainingRecordService,
                trainingCallScheduleRepository,
                attendanceRepository,
                callAnxietyStateRepository,
                diagnosisResultRepository
        );
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void myPageReadsTheMostRecentDiagnosisResultWhenTheUserHasSeveral() {
        login();
        DiagnosisResult latest = DiagnosisResult.builder()
                .user(user)
                .level(CallPhobiaLevel.LEVEL_2)
                .score(2.5)
                .updatedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .build();
        when(diagnosisResultRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(latest));

        MyPageResponse response = service.myPage().data();

        assertThat(response.level()).isEqualTo(CallPhobiaLevel.LEVEL_2);
        assertThat(response.score()).isEqualTo(2.5);
        assertThat(response.diagnosisDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void myPageWithoutAnyDiagnosisResultThrowsNotFound() {
        login();
        when(diagnosisResultRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(service::myPage)
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getStatusCode())
                .isEqualTo(DiagnosisResultStatusCode.DIAGNOSIS_RESULT_NOT_FOUND);
    }
}
