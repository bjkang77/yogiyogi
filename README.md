# Yogiyogi

# 목차

- [yogiyogi](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석 설계](#분석-설계)
  - [구현](#구현)
    - [DDD 의 적용](#DDD-의-적용)
    - [Polyglot Persistence](#Polyglot-Persistence)
    - [CQRS](#CQRS)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
    - [Liveness](#Liveness)
    - [ConfigMap](#ConfigMap)


# 서비스 시나리오

숙박 예약 시스템인 yogiyogi의 기능적, 비기능적 요구사항은 다음과 같습니다. 사용자가 원하는 숙소를 예약한 후 결제를 완료합니다. 담당자는 예약 내역을 확인한 후 확정합니다. 사용자는 예약 현황을 확인할 수 있습니다.

기능적 요구사항

1. 사용자는 원하는 숙소를 예약한다.
2. 사용자가 결제를 완료하면 예약이 완료된다.
3. 사용자가 결제를 완료하지 못하면 예약이 취소된다.
4. 예약이 완료되면 숙소 운영자는 예약 내역을 확인하고 확정하거나 취소한다.
5. 숙소 운영자는 예약 현황을 조회할수 있다.
6. 사용자는 예약 현황을 조회할 수 있다.
7. 사용자는 중간에 예약을 취소할 수 있으며, 해당 예약은 삭제된다.

비기능적 요구사항

1. 담당자 부재 등으로 예약 내역을 확정할 수 없더라도 사용자는 중단 없이 숙소를 예약할 수 있다. (Event Pub/Sub)
2. 결제가 완료되어야 예약이 완료된다. (Req/Res)
3. 부하 발생 시 자동으로 처리 프로세스를 증가시킨다. (Autoscale)

# 분석 설계
## Event Storming

- MSAEZ에서 Event Storming 수행
- Event 도출

![Event](https://user-images.githubusercontent.com/87048664/130742436-e279662e-b455-4034-82fe-affe80e4b82d.png)

- Actor, Command 부착

![Event1](https://user-images.githubusercontent.com/87048664/130746213-62423c0e-55ac-4458-88b9-c7b23b6db9db.png)

- Policy 부착

![Event2](https://user-images.githubusercontent.com/87048664/130746296-669cc227-44d0-4b70-8d27-62d0715755c8.png)

- Aggregate 부착

![Event3](https://user-images.githubusercontent.com/87048664/130746338-582e0860-b9d8-43ee-8fa4-ca289cd4933c.png)

- View 추가 및 Bounded Context 묶기

![Event4](https://user-images.githubusercontent.com/87048664/130746394-8df87112-233c-48e1-8069-5b5de6ba7117.png)

- 완성 모형: Pub/Sub, Req/Res 추가

![Event5](https://user-images.githubusercontent.com/87048664/130747113-3e0a1f43-04f8-4a2b-9528-e439cb029a52.png)

기능적 요구사항 커버 여부 검증

1. 사용자는 원하는 숙소를 예약한다. (O)
2. 사용자가 결제를 완료하면 예약이 완료된다. (O)
3. 사용자가 결제를 완료하지 못하면 예약이 취소된다. (O)
4. 예약이 완료되면 숙소 운영자는 예약 내역을 확인하고 확정하거나 취소한다. (O)
5. 숙소 운영자는 예약 현황을 조회할수 있다. (O)
6. 사용자는 예약 현황을 조회할 수 있다. (O)
7. 사용자는 중간에 예약을 취소할 수 있으며, 해당 예약은 삭제된다. (O)

비기능적 요구사항 커버 여부 검증

1. 담당자 부재 등으로 예약 내역을 확정할 수 없더라도 사용자는 중단 없이 숙소를 예약할 수 있다. (Event Pub/Sub) (O)
2. 결제가 완료되어야 예약이 완료된다. (Req/Res) (O)
3. 부하 발생 시 자동으로 처리 프로세스를 증가시킨다. (Autoscale) (O)

## Hexagonal Architecture Diagram

![Hexagonal](https://user-images.githubusercontent.com/87048664/130757136-4a29459c-8164-4885-af5c-17024376301f.png)

- Inbound adaptor와 Outbound adaptor를 구분함
- 호출관계에서 PubSub 과 Req/Resp 를 구분함
- 서브 도메인과 바운디드 컨텍스트를 분리함


# 구현
5개의 Microservice를 Springboot로 구현했으며, 다음과 같이 실행해 Local test를 진행했다. Port number는 8081~8085이다.

```
cd customer
mvn spring-boot:run

cd order
mvn spring-boot:run

cd payment
mvn spring-boot:run

cd reservation
mvn spring-boot:run

cd hotel
mvn spring-boot:run
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. 

```
package yogiyogi;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="PaymentHistory_table")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private Long cardNo;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getCardNo() {
        return cardNo;
    }

    public void setCardNo(Long cardNo) {
        this.cardNo = cardNo;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package yogiyogi;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="paymentHistories", path="paymentHistories")
public interface PaymentHistoryRepository extends PagingAndSortingRepository<PaymentHistory, Long>{

}
```
- 적용 후 REST API 의 테스트
```
# order 서비스의 주문처리
http localhost:8081/orders name="BJ.Kang" cardNo=111 status="order start"

# hotel 서비스의 예약 확정 처리
http localhost:8085/confirmations orderid=1 status="Confirmed"

# 주문 상태 확인    
http localhost:8081/orders/1
HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8
Date: Thu, 2 Sep 2021 02:05:39 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 111,
    "name": "BJ.Kang",
    "status": "order start"
}

```


## Polyglot Persistence

Polyglot Persistence를 위해 h2datase를 hsqldb로 변경

```
		<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
			<scope>runtime</scope>
		</dependency>
<!--
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
-->

# 변경/재기동 후 예약 주문
http localhost:8081/orders name="Kang" cardNo=1 status="order started"

HTTP/1.1 201 
Content-Type: application/json;charset=UTF-8
Date: Thu, 2 Sep 2021 07:41:30 GMT
Location: http://localhost:8081/orders/1
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 1,
    "name": "Kang",
    "status": "order started"
}


# 저장이 잘 되었는지 조회
http localhost:8081/orders/1

HTTP/1.1 200
Content-Type: application/hal+json;charset=UTF-8    
Date: Thu, 2 Sep 2021 07:42:25 GMT
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/1"
        },
        "self": {
            "href": "http://localhost:8081/orders/1"
        }
    },
    "cardNo": 1,
    "name": "Kang",
    "status": "order started"
}
```

## CQRS

CQRS 구현을 위해 고객의 예약 상황을 확인할 수 있는 Mypage 와 숙소 운영자가 예약 상황을 확인할 수 있는 HotelView 를 구현.

### mypage 호출 
![CQRS1](https://user-images.githubusercontent.com/87048664/131767196-e0d69df5-3b98-4079-81eb-865a58bee914.png)

### hotelview 호출 
![CQRS2](https://user-images.githubusercontent.com/87048664/131767236-d4544105-267c-4ea4-ab0d-99c12f80993d.png)

## 동기식 호출 과 Fallback 처리

주문(Order)->결제(Payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# ( order )PaymentHistoryService.java

package yogiyogi.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="payment", url="${api.payment.url}", fallback = PaymentHistoryServiceFallback.class)
public interface PaymentHistoryService {
    @RequestMapping(method= RequestMethod.POST, path="/paymentHistories")
    public void pay(@RequestBody PaymentHistory paymentHistory);

}

# ( order )PaymentHistoryServiceFallback.java

package yogiyogi.external;

public class PaymentHistoryServiceFallback implements PaymentHistoryService {
    @Override
    public void pay(PaymentHistory paymentHistory)
    {
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
    }

}

```

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Order.java (Entity)

    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        yogiyogi.external.PaymentHistory paymentHistory = new yogiyogi.external.PaymentHistory();
        // mappings goes here
        //PaymentHistory payment = new PaymentHistory();
        System.out.println("this.id() : " + this.id);
        paymentHistory.setOrderId(this.id);
        paymentHistory.setStatus("Reservation Good");
        paymentHistory.setCardNo(this.cardNo);      
        
        
        OrderApplication.applicationContext.getBean(yogiyogi.external.PaymentHistoryService.class)
            .pay(paymentHistory);
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


```
# 결제 (payment) 서비스를 잠시 내려놓음 (ctrl+c)

# 주문요청
http localhost:8081/orders name="aaa" cardNo=111 status="order start"

HTTP/1.1 500
Connection: close
Content-Type: application/json;charset=UTF-8
Date: Thu, 2 Sep 2021 06:14:53 GMT
Transfer-Encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/orders",
    "status": 500,
    "timestamp": "2021-09-02T06:14:53.402+0000"
}

# 결제 (payment) 재기동
mvn spring-boot:run

#주문처리
http localhost:8081/orders name="aaa" cardNo=111 status="order start"

HTTP/1.1 201
Content-Type: application/json;charset=UTF-8
Date: Thu, 2 Sep 2021 06:18:04 GMT
Location: http://localhost:8081/orders/5
Transfer-Encoding: chunked

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/5"
        },
        "self": {
            "href": "http://localhost:8081/orders/5"
        }
    },
    "cardNo": 111,
    "name": "aaa",
    "status": "order start"
}
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커 필요함)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


결제가 이루어진 후에 예약 시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 예약 시스템의 처리를 위하여 결제주문이 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package yogiyogi;

 ...
    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        
	paymentApproved.setStatus("Pay OK");
        
	BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();

    }
```
- 예약 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package yogiyogi;

...

@Service
public class PolicyHandler{
    @Autowired ReservationRepository reservationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_AcceptReserve(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener AcceptReserve : " + paymentApproved.toJson() + "\n\n");
	
        Reservation reservation = new Reservation();
        reservation.setStatus("Reservation Complete");
        reservation.setOrderId(paymentApproved.getOrderId());
        reservation.setId(paymentApproved.getOrderId());
        reservationRepository.save(reservation);
       

    }
```

예약 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 예약시스템이 배포/로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 예약 서비스 (reservation) 를 잠시 내려놓음 (ctrl+c)

#주문처리
http localhost:8081/orders name="Kang" cardNo=27 status="order started"

#주문상태 확인
http localhost:8081/orders/3      # 주문정상

{
    "_links": {
        "order": {
            "href": "http://localhost:8081/orders/3"
        },
        "self": {
            "href": "http://localhost:8081/orders/3"
        }
    },
    "cardNo": 27,
    "name": "Kang",
    "status": "order started"
}
	    
#예약 서비스 기동
cd reservation
mvn spring-boot:run

#주문상태 확인
http localhost:8084/mypages     # 예약 상태가 "Reservation Complete"으로 확인

 {
                "_links": {
                    "mypage": {
                        "href": "http://localhost:8084/mypages/3"
                    },
                    "self": {
                        "href": "http://localhost:8084/mypages/3"
                    }
                },
                "cancellationId": null,
                "name": "Kang",
                "orderId": 3,
                "reservationId": 1,
                "status": "Reservation Complete"
            }
```


# 운영

## Deploy

아래와 같은 순서로 AWS 사전 설정을 진행한다.
```
1) AWS IAM 설정
2) EKC Cluster 생성	
3) AWS 클러스터 토큰 가져오기
4) Docker Start/Login 
```
이후 사전 설정이 완료된 상태에서 아래 배포 수행한다.
```
(1) order build/push
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:v1 .
aws ecr create-repository --repository-name user01-order --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:v1

(2) customer build/push
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-customer:v1 .
aws ecr create-repository --repository-name user01-customer --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-customer:v1

(3) gateway build/push
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:v1 .
aws ecr create-repository --repository-name user01-gateway --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:v1

(4) hotel build/push
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-hotel:v1 .
aws ecr create-repository --repository-name user01-hotel --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-hotel:v1

(5) payment build/push
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:v1 .
aws ecr create-repository --repository-name user01-payment --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:v1

(6) reservation build/push
mvn package
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-reservation:v1 .
aws ecr create-repository --repository-name user01-reservation --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-reservation:v1

(7) 배포
kubectl create deploy payment --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-payment:v1
kubectl create deploy order --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-order:v1
kubectl create deploy reservation --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-reservation:v1
kubectl create deploy customer --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-customer:v1
kubectl create deploy hotel --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-hotel:v1
kubectl create deploy gateway --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user01-gateway:v1

kubectl expose deploy order --type=ClusterIP --port=8080
kubectl expose deploy reservation --type=ClusterIP --port=8080
kubectl expose deploy payment --type=ClusterIP --port=8080
kubectl expose deploy customer --type=ClusterIP --port=8080
kubectl expose deploy gateway --type=LoadBalancer --port=8080
kubectl expose deploy hotel --type="ClusterIP" --port=8080
```
배포 과정은 아래와 같다. Gateway는 LoadBalancer type으로 설정하여 인터넷을 통해 접근 가능하다.
![deploy](https://user-images.githubusercontent.com/87048664/131771116-6c104701-9dd3-400a-9d29-6a34bbc3a21d.png)
![deploy2](https://user-images.githubusercontent.com/87048664/131771399-81010b67-6133-4746-a9d4-e69ab816ff59.png)

## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 주문(order)-->결제(payment) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB(Circuit Breaker ) 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# (order) application.yml

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

- 피호출 서비스(payment) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# (payment) PaymentHistory.java (Entity)

    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        paymentApproved.setStatus("Pay OK");
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();
	
	//결제이력을 저장한 후 적당한 시간 끌기
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시
![CB1](https://user-images.githubusercontent.com/87048664/131446364-2ef12a4d-372d-4e86-8bc1-f2b3271e3c30.png)

* 요청이 과도하여 CB가 동작하여 요청을 차단했다가 다시 열리기를 반복함
![CB2](https://user-images.githubusercontent.com/87048664/131446580-1c4b44f3-350a-449d-90c4-b7c29edbcc97.png)
![CB3](https://user-images.githubusercontent.com/87048664/131446670-dc96d3be-d3f4-4583-9280-f161f76be94e.png)

* 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 하지만, 67.29% 가 성공하였고, 33%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 Auto Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.


### 오토스케일 아웃
payment에 대한 조회증가 시 replica 를 동적으로 늘려주도록 오토스케일아웃을 설정한다.

- payment_autoscale.yml에 resources 설정을 추가한다
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment
  labels:
    app: payment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: payment
      ...
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: "200m"
            limits:
              cpu: "500m"
```	      

- payment 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다.<br/>
![autoscale1](https://user-images.githubusercontent.com/87048664/131607595-95bfd0d3-581f-440a-af66-6627ddde252b.png)

- 부하를 동시사용자 100명으로 걸어준다.
![autoscale2](https://user-images.githubusercontent.com/87048664/131607667-f6081d1e-02fb-4077-8da4-b8361b0db7fc.png)

- 모니터링 결과 스케일 아웃 정상작동을 확인할 수 있다.
![autoscale3](https://user-images.githubusercontent.com/87048664/131607705-6f30fcfb-40b9-4d78-a5ff-219a7852f8d6.png)


## 무정지 재배포 (Readiness)

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
- customer microservice v2 이미지를 생성해 deploy
- 새 터미널에서 seige 로 배포작업 직전에 워크로드를 모니터링 함.
- 새버전으로 배포

```
kubectl apply -f /home/jacesky/yogiyogi-team/kubernetes/deployment_readiness_v1.yml
```

- seige에서  Availability 가 100% 미만으로 떨어졌는지 확인

![Readiness 1](https://user-images.githubusercontent.com/3106233/130053885-2bece799-de7e-44e4-b6eb-f588a0fd37e2.png)

배포기간중 Availability 가 평소 100%에서 90%대로 떨어지는 것을 확인. Kubernetes가 신규로 Deploy된 Microservice를 준비 상태로 인식해 서비스 수행했기 때문임.
방지를 위해 Readiness Probe 를 설정함:

```
# deployment.yaml 의 readiness probe 의 설정:
kubectl apply -f kubernetes/deployment.yaml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:

![Readiness 2](https://user-images.githubusercontent.com/3106233/130053849-49de6039-299a-47fa-adde-dac3e114dab0.png)

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


## Liveness

임의로 Pod의 Health check에 문제를 발생하도록 customer 서비스의 deployment.yaml 에 설정하고, Liveness Probe가 Pod를 재기동하는지 확인

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: customer
  labels:
    app: customer
	...
          args:
          - /bin/sh
          - -c
          - touch /tmp/healthy; sleep 90; rm -rf /tmp/healthy; sleep 600
          ports:
            - containerPort: 8080
          livenessProbe:
            exec:
              command:
              - cat
              - /tmp/healthy
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```


RESTARTS 회수가 증가함을 확인

![liveness](https://user-images.githubusercontent.com/87048664/131455896-9b1784fa-d288-46a5-b762-b3b464c40847.png)


## ConfigMap
주문(Order)->결제(Payment) 구간은 동기식 호출(Req/Res)로 연결되어 있어 Payment 서비스의 URL 이 변경될 경우 유연하게 처리가 가능해야 한다.
Order 서비스에서 바라보는 Payment 서비스 url 부분을 ConfigMap 사용하여 구현하였다.

```
# ( order ) PaymentHistoryService.java

package yogiyogi.external;

@FeignClient(name="payment", url="${api.payment.url}", fallback = PaymentHistoryServiceFallback.class)
public interface PaymentHistoryService {
    @RequestMapping(method= RequestMethod.POST, path="/paymentHistories")
    public void pay(@RequestBody PaymentHistory paymentHistory);

}

# ( order ) application.yml

api:
  payment:
    url: ${payment-url}
    
# order_configmap.yml

apiVersion: v1
kind: ConfigMap
metadata:
  name: order-configmap
  namespace: default
data:
  payment-url: payment:8080

# ( order ) cm_deploy_order.yml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
...
    spec:
      containers:
        - name: order
          ...
          env:
            - name: PAYMENT-URL
              valueFrom:
                configMapKeyRef:
                  name: order-configmap
                  key: payment-url

```
![cm1](https://user-images.githubusercontent.com/87048664/131793284-00ddd19e-4f3a-45db-810f-26821e15b520.png)
![cm2](https://user-images.githubusercontent.com/87048664/131793335-62d6e78b-795b-46d6-963f-e76b9a0a99c3.png)
