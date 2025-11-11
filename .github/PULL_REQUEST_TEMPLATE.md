#23 에서 도입한 Redis 기반 비동기 알림 처리에 예약/모니터링/재처리 기능을 추가했다. [마이크로서비스 메시지 브로커 비교 포스팅](https://logit.io/blog/post/rabbitmq-vs-kafka-vs-redis/)

을 참고한 결과, 소규모 메시지 처리(수백 바이트, 하루 수백 건)에는 Kafka나 RabbitMQ보다 Redis가 적합했다. Redis는 Sorted Set으로 예약 처리를 간단히 구현할 수 있고, 단일 컨테이너로 큐/스케줄러/로그/DLQ를 통합 관리하여 경량화에 유리하다.



## 주요 기능

- **예약 알림 스케줄러**  

  Redis Sorted Set(`chat:scheduled-notifications`)으로 만기 작업을 일반 큐로 전환하여 실제 전송  

  예약 취소/수정 시 `ZREM`으로 제거하고, 메타데이터는 Hash에 저장하여 관리



- **알림 모니터링 및 장애 처리**  

  알림 처리 로그(`chat:notification-logs`)와 실패 큐(DLQ)를 Redis에 저장하고, 재처리/모니터링용 REST API 제공  

  로그는 `LTRIM`으로 일정 길이 이상 자동 정리, 재시도 횟수는 Hash에 누적



- **예약 메시지 시스템**  

  Spring 관리 콘솔(`NotificationController`)에서 Node.js 예약 메시지 API(`/api/messages/reserve`)를 호출하여 채팅 메시지를 MySQL에 저장 → Socket 이벤트 발송 → Redis 알림 큐 적재



## 기술 스택

- **Redis (`ioredis`)**: 메시지 큐 및 스케줄러 관리

- **Node.js + Express + Socket.IO**: 실시간 채팅 서버와 REST API 제공

- **MySQL**: 채팅 메시지 본문 영구 저장

- **Spring Boot + RestTemplate**: 관리자 기능 및 Node.js 알림 API 연동

