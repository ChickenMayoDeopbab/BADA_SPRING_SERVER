package ChickenMayoDeopbab.bada.domain.trainingcallschedule.port;

import ChickenMayoDeopbab.bada.domain.trainingcallschedule.entity.TrainingCallSchedule;

public interface PushNotificationPort {

    void notifyIncomingCall(TrainingCallSchedule schedule);
}
