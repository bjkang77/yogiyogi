# Yanolza

# 목차

- [Yanolza](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석 설계](#분석-설계)
  - [구현](#구현)
    - [DDD 의 적용](#ddd의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

숙박 예약 시스템인 Yanolza의 기능적, 비기능적 요구사항은 다음과 같습니다. 사용자가 원하는 숙소를 예약한 후 결제를 완료합니다. 담당자는 예약 내역을 확인한 후 확정합니다. 사용자는 예약 현황을 확인할 수 있습니다.

기능적 요구사항

1. 사용자는 원하는 숙소를 예약한다.
2. 사용자가 결제를 완료하면 예약이 완료된다.
3. 사용자가 결제를 완료하지 못하면 예약이 취소된다.
4. 예약이 완료되면 담당자는 예약 내역을 확인하고 확정한다.
5. 사용자는 예약 현황을 조회할 수 있다.
6. 사용자는 중간에 예약을 취소할 수 있으며, 해당 예약은 삭제된다.

비기능적 요구사항

1. 담당자 부재 등으로 예약 내역을 확정할 수 없더라도 사용자는 중단 없이 재능을 예약할 수 있다. (Event Pub/Sub)
2. 결제가 완료되어야 예약이 완료된다. (Req/Res)
3. 부하 발생 시 자동으로 처리 프로세스를 증가시킨다. (Autoscale)

# 분석 설계
## Event Storming

- MSAEZ에서 Event Storming 수행
- Event 도출

![슬라이드2](https://user-images.githubusercontent.com/3106233/129735109-d44a14a2-987d-4c76-982e-556bd16d1cf4.PNG)

- Actor, Command 부착

![슬라이드3](https://user-images.githubusercontent.com/3106233/129735198-642b10b0-86b4-49c3-b608-ad3a4dec4914.PNG)

- Policy 부착
- 
![슬라이드4](https://user-images.githubusercontent.com/3106233/129735339-6fa82c18-44b5-452c-9c67-1728e1a4c115.PNG)

- Aggregate 부착

![슬라이드5](https://user-images.githubusercontent.com/3106233/129735390-8eaeea8a-c8ba-41c7-9694-929fb1ae0a93.PNG)

- View 추가 및 Bounded Context 묶기

![슬라이드6](https://user-images.githubusercontent.com/3106233/129735417-a92fb162-6a6e-48aa-a57f-c5f94da2f3e0.PNG)

- 완성 모형: Pub/Sub, Req/Res 추가

![슬라이드7](https://user-images.githubusercontent.com/3106233/129735454-4ccf11b6-735e-44a5-9f8b-eb1af5852632.PNG)

기능적 요구사항 커버 여부 검증

1. 사용자는 원하는 재능을 예약한다. (O)
2. 사용자가 결제를 완료하면 예약이 완료된다. (O)
3. 사용자가 결제를 완료하지 못하면 예약이 취소된다. (O)
4. 예약이 완료되면 예약 상세 내역이 담당자에게 전달된다. (O)
5. 담당자는 내역을 확인하고 확정한다. (O)
6. 사용자는 예약 현황을 조회할 수 있다. (O)
7. 사용자는 중간에 예약을 취소할 수 있으며, 해당 예약은 삭제된다. (O)

비기능적 요구사항 커버 여부 검증

1. 담당자 부재 등으로 예약 내역을 확정할 수 없더라도 사용자는 중단 없이 재능을 예약할 수 있다. (Event Pub/Sub) (O)
2. 결제가 완료되어야 예약이 완료된다. (Req/Res) (O)
3. 부하 발생 시 자동으로 처리 프로세스를 증가시킨다. (Autoscale) (O)

## Hexagonal Architecture Diagram

![Hexagonal](https://user-images.githubusercontent.com/3106233/129030761-f4adc773-9505-4522-8416-f263369fee43.png)

# 구현
세 개의 Microservice를 Springboot로 구현했으며, 다음과 같이 실행해 Local test를 진행했다.

```
cd order
mvn spring-boot:run

cd payment
mvn spring-boot:run

cd confirmation
mvn spring-boot:run
```

## DDD의 적용

