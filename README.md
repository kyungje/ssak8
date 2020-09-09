# ssak8 - 청소대행 서비스

# 목차

  - [서비스 시나리오](#서비스-시나리오)
  - [분석/설계](#분석설계)
  - [구현](#구현)
    - [DDD 의 적용](#DDD-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#CI/CD-설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
    - [ConfigMap 사용](#ConfigMap-사용)


# 서비스 시나리오
  
## 기능적 요구사항
1. 고객이 정보를 등록한다.(Sync, 카카오톡서비스)
2. 고객이 정보등록을 취소한다. (Async, 알림서비스)
3. 고객은 본인의 등록 내역을 조회할 수 있다.

## 비기능적 요구사항
### 1. 트랜잭션
- 카카오톡으로 전달되지 않는 고객은 등록되지 않는다. → Sync 호출 
### 2. 장애격리
- 등록취소건은 카카오톡이 되지 않아도 취소된다. - Async (event-driven), Eventual Consistency
- 카카오톡이 과중되면 등록을 잠시 미룬다. → Circuit breaker, fallback
### 3. 성능
- 고객이 확인할 수 있는 마이페이지(프론트엔드)가 있다. → CQRS
- 등록 취소가 될때마다 카톡 등으로 알림을 줄 수 있어야 한다 → Event driven

# 분석/설계

고객등록 및 취소 시 Saga패턴(예약 Req/Resp, 취소 Pub/Sub)을 적용하여 구현되도록 설계함

## AS-IS 조직 (Horizontally-Aligned)
  ![03](https://user-images.githubusercontent.com/69634194/92385495-f3e68200-f14c-11ea-9ca0-c27cc85c986d.png)


## TO-BE 조직 (Vertically-Aligned)
  ![4](https://user-images.githubusercontent.com/69634194/92545590-72493e00-f28b-11ea-847d-4afdd801020e.png)

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과 : http://www.msaez.io/#/storming/k1eXHY4YSrSFKU3UpQTDRHUvSS23/mine/a2c3fea59b264b6c89b743b50424dc6e/-MGlhpIKpdgJUgaz2zuN

  
# 구현/배포(deploy)
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 배포는 아래와 같이 수행한다.

## Azure Configure
```console
- Azure (http://portal.azure.com) : admin8@gkn2019hotmail.onmicrosoft.com
- AZure 포탈에서 리소스 그룹 > 쿠버네티스 서비스 생성 > 컨테이너 레지스트리 생성
- 리소스 그룹 생성 : ssak8-rg
- 컨테이너 생성( Kubernetes ) : ssak8-aks
- 레지스트리 생성 : ssak8acr, ssak8acr.azurecr.io
```

## 접속환경
- Azure 포탈에서 가상머신 신규 생성 - ubuntu 18.04

## Kubectl install
```
sudo apt-get update && sudo apt-get install -y apt-transport-https
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee -a /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubectl
```

## Azure-Cli  install
```console
# curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
# az login -u  -p
```


## Azure 인증
```console
# az login
# az aks get-credentials --resource-group ssak8-rg --name ssak8-aks
# az acr login --name ssak8acr --expose-token

```

## Azure AKS와 ACR 연결
```console
az aks update -n ssak8-aks -g ssak8-rg --attach-acr ssak8acr
```

## kubectl로 확인
```console
kubectl config current-context
kubectl get all
```

## jdk설치
```console
sudo apt-get update
sudo apt install default-jdk
[bash에 환경변수 추가]
1. cd ~
2. nano .bashrc 
3. 편집기 맨 아래로 이동
4. (JAVA_HOME 설정 및 실행 Path 추가)
export JAVA_HOME=‘/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin:.

ctrl + x, y 입력, 종료
source ~/.bashrc
5. 설치확인
echo $JAVA_HOME
java -version

```

## 리눅스에 Docker client 설치
```console
sudo apt-get update
sudo apt install apt-transport-https ca-certificates curl software-properties-common
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add 
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu bionic stable"
sudo apt update
sudo apt install docker-ce
# 리눅스 설치시 생성한 사용자 명 입력
sudo usermod -aG docker skccadmin
```

## 리눅스에 docker demon install
```console
sudo apt-get update
sudo apt-get install \
     apt-transport-https \
     ca-certificates \
     curl \
     gnupg-agent \
     software-properties-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo apt-key fingerprint 0EBFCD88

sudo add-apt-repository \
     "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) \
     stable"

sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io


(demon server 시작)
sudo service docker start
(확인)
docker version
sudo docker run hello-world

```

## Docker demon과 Docker client 연결
```console
cd
nano .bashrc
맨아래 줄에 아래 환경변수 추가
방향키로 맨 아래까지 내린 다음, 새로운 행에 아래 내용 입력
export DOCKER_HOST=tcp://0.0.0.0:2375 
저장 & 종료 : Ctrl + x, 입력 후, y 입력  후 엔터
source ~/.bashrc
```

## Kafka install (kubernetes/helm)
참고 - (https://workflowy.com/s/msa/27a0ioMCzlpV04Ib#/a7018fb8c62)
```console

curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
kubectl --namespace kube-system create sa tiller      
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller
helm init --service-account tiller
kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'

helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
helm repo update

helm install --name my-kafka --namespace kafka incubator/kafka
```

## Kafka delete
```console
helm del my-kafka  --purge
```


## Istio 설치
```console
kubectl create namespace istio-system

curl -L https://git.io/getLatestIstio | ISTIO_VERSION=1.4.5 sh -
cd istio-1.4.5
export PATH=$PWD/bin:$PATH
for i in install/kubernetes/helm/istio-init/files/crd*yaml; do kubectl apply -f $i; done
kubectl apply -f install/kubernetes/istio-demo.yaml
kubectl get pod -n istio-system
```

## kiali 설치
```console

vi kiali.yaml    

apiVersion: v1
kind: Secret
metadata:
  name: kiali
  namespace: istio-system
  labels:
    app: kiali
type: Opaque
data:
  username: YWRtaW4=
  passphrase: YWRtaW4=

----- save (:wq)

kubectl apply -f kiali.yaml
helm template --set kiali.enabled=true install/kubernetes/helm/istio --name istio --namespace istio-system > kiali_istio.yaml    
kubectl apply -f kiali_istio.yaml
```
- load balancer로 변경
```console
kubectl edit service/kiali -n istio-system
(ClusterIP -> LoadBalancer)
```

## namespace create
```console
kubectl create namespace ssak8
```
## namespace 선택 설정 (-n ssak8 옵션을 주지 않도록 default 작업 ns 설정 방법)
```console
kubectl config set-context --current --namespace=ssak8
```

## istio enabled
```console
kubectl label namespace ssak8 istio-injection=enabled
```

## siege deploy
```console
cd ssak8/yaml
kubectl apply -f siege.yaml 
kubectl exec -it siege -n ssak8 -- /bin/bash
apt-get update
apt-get install httpie
```

## image build & push
- compile
```console
cd ssak8/gateway
mvn package
```

- for azure cli
```console
docker build -t ssak8acr.azurecr.io/gateway .
docker images
docker push ssak8acr.azurecr.io/gateway
```

## application deploy
```console
kubectl create ns ssak8
kubectl label ns ssak8 istio-injection=enabled

kubectl create deploy gateway --image=ssak8acr.azurecr.io/gateway -n ssak8
kubectl create deploy kakao --image=ssak8acr.azurecr.io/kakao -n ssak8
kubectl create deploy customer --image=ssak8acr.azurecr.io/customer -n ssak8
kubectl create deploy customerview --image=ssak8acr.azurecr.io/customerview -n ssak8

kubectl expose deploy gateway --port=8080 -n ssak8
kubectl expose deploy kakao --port=8080 -n ssak8
kubectl expose deploy customer --port=8080 -n ssak8
kubectl expose deploy customerview --port=8080 -n ssak8

cd ssak8/yaml

kubectl apply -f configmap.yaml
kubectl apply -f gateway.yaml
kubectl apply -f kakao.yaml
kubectl apply -f customer.yaml
kubectl apply -f customerview.yaml
```

## CQRS 패턴 적용(View)

```java
package cleaningServiceydp;

import cleaningServiceydp.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerViewViewHandler {


    @Autowired
    private CustomerViewRepository customerViewRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCustomerRegistered_then_CREATE_1 (@Payload CustomerRegisterCanceled customerRegisterCanceled) {
        try {
            if (customerRegisterCanceled.isMe()) {
                // view 객체 생성
                CustomerView customerView = new CustomerView();
                // view 객체에 이벤트의 Value 를 set 함
                customerView.setId(customerRegisterCanceled.getId());
                customerView.setCustomerName(customerRegisterCanceled.getCustomerName());
                customerView.setCustomerAddress(customerRegisterCanceled.getCustomerAddress());
                customerView.setCustomerAge(customerRegisterCanceled.getCustomerAge());
                // view 레파지 토리에 save
                customerViewRepository.save(customerView);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

```
```
root@siege:/# http GET http://customerView:8080/customerViews
HTTP/1.1 200 OK
content-type: application/hal+json;charset=UTF-8
date: Wed, 09 Sep 2020 12:28:44 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 14

{
    "_embedded": {
        "customerViews": [
            {
                "_links": {
                    "customerView": {
                        "href": "http://customerview:8080/customerViews/1"
                    },
                    "self": {
                        "href": "http://customerview:8080/customerViews/1"
                    }
                },
                "customerAddress": "incheon",
                "customerAge": 20,
                "customerName": "yeon"
            },
            {
                "_links": {
                    "customerView": {
                        "href": "http://customerview:8080/customerViews/2"
                    },
                    "self": {
                        "href": "http://customerview:8080/customerViews/2"
                    }
                },
                "customerAddress": "incheon",
                "customerAge": 20,
                "customerName": "yeon"
            }
        ]
    },
    "_links": {
        "profile": {
            "href": "http://customerview:8080/profile/customerViews"
        },
        "self": {
            "href": "http://customerview:8080/customerViews"
        }
    }
}

```
## Request/Response 적용

- 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인


```java
@FeignClient(name="kakao", url="${api.url.kakao}")
public interface KakaoService {

    @RequestMapping(method= RequestMethod.POST, path="/kakaos")
    public void kakaoAlert(@RequestBody Kakao kakao);

}
```
```
# kakao 서비스를 잠시 내려놓음
root@ip-172-26-8-112:~/azure/ssak8/yaml# kubectl delete -f kakao.yaml 
deployment.apps "kakao" deleted
service "kakao" deleted

root@siege:/# http POST http://customer:8080/customers customerName=yeon customerAddress=incheon customerAge=20
HTTP/1.1 500 Internal Server Error
content-type: application/json;charset=UTF-8
date: Wed, 09 Sep 2020 12:33:40 GMT
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 73

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/customers",
    "status": 500,
    "timestamp": "2020-09-09T12:33:41.138+0000"
}

root@ip-172-26-8-112:~/azure/ssak8/yaml# kubectl apply -f kakao.yaml 
deployment.apps/kakao created
service/kakao created

root@siege:/# http POST http://customer:8080/customers customerName=yeon customerAddress=incheon customerAge=20
HTTP/1.1 201 Created
content-type: application/json;charset=UTF-8
date: Wed, 09 Sep 2020 12:36:19 GMT
location: http://customer:8080/customers/4
server: envoy
transfer-encoding: chunked
x-envoy-upstream-service-time: 778

{
    "_links": {
        "customer": {
            "href": "http://customer:8080/customers/4"
        },
        "self": {
            "href": "http://customer:8080/customers/4"
        }
    },
    "customerAddress": "incheon",
    "customerAge": 20,
    "customerName": "yeon"
}
```


- API Gateway 적용
```console
# gateway service type 변경
$ kubectl edit service/gateway -n ssak8
(ClusterIP -> LoadBalancer)

root@ip-172-26-8-112:~/azure/ssak8/yaml# kubectl get service -n ssak8
NAME           TYPE           CLUSTER-IP    EXTERNAL-IP    PORT(S)          AGE
customer       ClusterIP      10.0.96.14    <none>         8080/TCP         82m
customerview   ClusterIP      10.0.48.174   <none>         8080/TCP         71m
gateway        LoadBalancer   10.0.147.77   40.82.143.77   8080:31431/TCP   82m
kakao          ClusterIP      10.0.203.54   <none>         8080/TCP         8m43s
```

- API Gateway 적용 확인
```console
//API gateway
http GET http://40.82.143.77:8080/customers
http GET http://40.82.143.77:8080/kakaos
http GET http://40.82.143.77:8080/customerViews
//등록
http POST http://40.82.143.77:8080/customers customerName=yeon customerAddress=incheon customerAge=20
//등록취소
http DELETE http://40.82.143.77:8080/customers/1
```

- siege 접속
```console
kubectl exec -it siege -n cleaning -- /bin/bash
```

![kiali](https://user-images.githubusercontent.com/27332622/92601353-a9e5d380-f2e7-11ea-9861-7e4d93ce82e4.JPG)

- (siege 에서) 적용 후 REST API 테스트 
```
//siege
//조회
http GET http://customer:8080/customers
http GET http://kakao:8080/kakaos
http GET http://customerView:8080/customerViews
//등록
http POST http://customer:8080/customers customerName=yeon customerAddress=incheon customerAge=20
//등록취소
http DELETE http://customer:8080/customers/1
```

## 비동기식 호출과 Eventual Consistency(Pub/sub)
- 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
결제가 이루어진 후에 알림 처리는 동기식이 아니라 비 동기식으로 처리하여 알림 시스템의 처리를 위하여 예약이 블로킹 되지 않아도록 처리한다.
 
- 이를 위하여 등록 후 곧바로 취소되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
```java
@PostRemove
    public void onPostRemove(){
    	CustomerRegisterCanceled customerRegisterCanceled = new CustomerRegisterCanceled();

    	customerRegisterCanceled.setCustomerAddress(getCustomerAddress());
    	customerRegisterCanceled.setCustomerName(getCustomerName());
    	customerRegisterCanceled.setCustomerAge(getCustomerAge());

    	BeanUtils.copyProperties(this, customerRegisterCanceled);
        customerRegisterCanceled.publishAfterCommit();
    }
```
- kakao 서비스에서는 취소 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다
```java
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCustomerRegisterCanceled_KakaoAlertCancel(@Payload CustomerRegisterCanceled customerRegisterCanceled){

        if(customerRegisterCanceled.isMe()){
            Kakao kakao = new Kakao();
            kakao.setCustomerName(customerRegisterCanceled.getCustomerName());
            kakao.setCustomerAddress(customerRegisterCanceled.getCustomerAddress());
            kakao.setCustomerAge(customerRegisterCanceled.getCustomerAge());

            kakaoRepository.save(kakao);
            System.out.println("##### listener KakaoAlertCancel : " + customerRegisterCanceled.toJson());
        }
    }
```
* kakao 취소 시스템은 등록과 완전히 분리되어있으며, 이벤트 수신에 따라 처리된다.

```
# kakao 서비스를 잠시 내려놓음
kubectl delete -f kakao.yaml

root@siege:/# http DELETE http://customer:8080/customers/5
HTTP/1.1 204 No Content
date: Wed, 09 Sep 2020 13:15:28 GMT
server: envoy
x-envoy-upstream-service-time: 20

정상작동확인

```

## CI/CD 설정
  * 각 구현체들은 github의 각각의 source repository 에 구성
  * Image repository는 Azure 사용

## 동기식 호출 / 서킷 브레이킹 / 장애격리

### 서킷 브레이킹 프레임워크의 선택: istio-injection + DestinationRule

* istio-injection 적용 (기 적용완료)
```
kubectl label namespace ssak8 istio-injection=enabled

```
* 등록, kakao 변경없음

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시
```console
siege -v -c100 -t60S -r10 --content-type "application/json" 'http://customer:8080/customers POST {"customerName": "yeon","customerAddress": "incheon","customerAge": 20}'


HTTP/1.1 201     0.51 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.50 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.59 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.58 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.50 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.50 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.10 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.91 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.92 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.51 secs:     257 bytes ==> POST http://customer:8080/customers
HTTP/1.1 201     0.51 secs:     257 bytes ==> POST http://customer:8080/customers

Lifting the server siege...
Transactions:                   8471 hits
Availability:                 100.00 %
Elapsed time:                  59.70 secs
Data transferred:               2.07 MB
Response time:                  0.70 secs
Transaction rate:             141.89 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   99.23
Successful transactions:        8471
Failed transactions:               0
Longest transaction:            5.07
Shortest transaction:           0.01

```
* 서킷 브레이킹을 위한 DestinationRule 적용
```
cd ssak8/yaml
kubectl apply -f kakao_dr.yaml

# destinationrule.networking.istio.io/dr-payment created

HTTP/1.1 500     0.68 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.70 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.71 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.72 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.92 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.68 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.82 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
HTTP/1.1 500     0.71 secs:     262 bytes ==> POST http://reservation:8080/cleaningReservations
siege aborted due to excessive socket failure; you
can change the failure threshold in $HOME/.siegerc

Transactions:                     20 hits
Availability:                   1.75 %
Elapsed time:                   9.92 secs
Data transferred:               0.29 MB
Response time:                 48.04 secs
Transaction rate:               2.02 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                   96.85
Successful transactions:          20
Failed transactions:            1123
Longest transaction:            2.53
Shortest transaction:           0.04
```

- DestinationRule 적용되어 서킷 브레이킹 동작 확인 (kiali 화면)
  ![kiali2](https://user-images.githubusercontent.com/69634194/92505880-94b56a00-f23f-11ea-9b10-b1e43e195ca2.png)

## 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 함
* (istio injection 적용한 경우) istio injection 적용 해제
```
kubectl label namespace ssak8 istio-injection=disabled --overwrite

# namespace/ssak8 labeled

kubectl apply -f reservation.yaml
kubectl apply -f payment.yaml
```
- 결제서비스 배포시 resource 설정 적용되어 있음
```
    spec:
      containers:
          ...
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```

- 결제서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 3개까지 늘려준다
```console
kubectl autoscale deploy payment -n ssak8 --min=1 --max=3 --cpu-percent=15

# horizontalpodautoscaler.autoscaling/payment autoscaled

root@ssak8-vm:~/ssak8/yaml# kubectl get all -n ssak8
NAME                               READY   STATUS    RESTARTS   AGE
pod/cleaning-bf474f568-vxl8r       2/2     Running   0          3h5m
pod/dashboard-7f7768bb5-7l8wr      2/2     Running   0          3h3m
pod/gateway-6dfcbbc84f-rwnsh       2/2     Running   0          85m
pod/message-69597f6864-fjs69       2/2     Running   0          34m
pod/payment-7749f7dc7c-kfjxb       2/2     Running   0          39m
pod/reservation-775fc6574d-kddgd   2/2     Running   0          3h12m
pod/siege                          2/2     Running   0          4h27m

NAME                  TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)          AGE
service/cleaning      ClusterIP      10.0.150.114   <none>         8080/TCP         3h5m
service/dashboard     ClusterIP      10.0.69.44     <none>         8080/TCP         3h3m
service/gateway       LoadBalancer   10.0.56.218    20.196.72.75   8080:32642/TCP   85m
service/message       ClusterIP      10.0.255.90    <none>         8080/TCP         34m
service/payment       ClusterIP      10.0.64.167    <none>         8080/TCP         39m
service/reservation   ClusterIP      10.0.23.111    <none>         8080/TCP         3h12m

NAME                          READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/cleaning      1/1     1            1           3h5m
deployment.apps/dashboard     1/1     1            1           3h3m
deployment.apps/gateway       1/1     1            1           85m
deployment.apps/message       1/1     1            1           34m
deployment.apps/payment       1/1     1            1           39m
deployment.apps/reservation   1/1     1            1           3h12m

NAME                                     DESIRED   CURRENT   READY   AGE
replicaset.apps/cleaning-bf474f568       1         1         1       3h5m
replicaset.apps/dashboard-7f7768bb5      1         1         1       3h3m
replicaset.apps/gateway-6dfcbbc84f       1         1         1       85m
replicaset.apps/message-69597f6864       1         1         1       34m
replicaset.apps/payment-7749f7dc7c       1         1         1       39m
replicaset.apps/reservation-775fc6574d   1         1         1       3h12m

NAME                                          REFERENCE            TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/payment   Deployment/payment   3%/15%    1         3         1          55s
```

- CB 에서 했던 방식대로 워크로드를 3분 동안 걸어준다.
```console
siege -v -c100 -t180S -r10 --content-type "application/json" 'http://reservation:8080/cleaningReservations POST {"customerName": "noh","price": 300000,"requestDate": "20200909","status": "ReservationApply"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```console
kubectl get deploy payment -n ssak8 -w 

NAME      READY   UP-TO-DATE   AVAILABLE   AGE
payment   1/1     1            1           43m

# siege 부하 적용 후
root@ssak8-vm:/# kubectl get deploy payment -n ssak8 -w
NAME      READY   UP-TO-DATE   AVAILABLE   AGE
payment   1/1     1            1           43m
payment   1/3     1            1           44m
payment   1/3     1            1           44m
payment   1/3     3            1           44m
payment   2/3     3            2           46m
payment   3/3     3            3           46m
```
- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다.
```console
Lifting the server siege...
Transactions:                  19309 hits
Availability:                 100.00 %
Elapsed time:                 179.75 secs
Data transferred:               6.31 MB
Response time:                  0.92 secs
Transaction rate:             107.42 trans/sec
Throughput:                     0.04 MB/sec
Concurrency:                   99.29
Successful transactions:       19309
Failed transactions:               0
Longest transaction:            7.33
Shortest transaction:           0.01
```

## 무정지 재배포 (readiness)
- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함 (위의 시나리오에서 제거되었음)
```console
kubectl delete horizontalpodautoscaler.autoscaling/payment -n ssak8
```
- yaml 설정 참고
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reservation
  namespace: ssak8
  labels:
    app: reservation
spec:
  replicas: 1
  selector:
    matchLabels:
      app: reservation
  template:
    metadata:
      labels:
        app: reservation
    spec:
      containers:
        - name: reservation
          image: ssak8acr.azurecr.io/reservation:1.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: api.url.payment
              valueFrom:
                configMapKeyRef:
                  name: ssak8-config
                  key: api.url.payment
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

---

apiVersion: v1
kind: Service
metadata:
  name: reservation
  namespace: ssak8
  labels:
    app: reservation
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: reservation
```

- siege 로 배포작업 직전에 워크로드를 모니터링 함.
```console
siege -v -c1 -t120S -r10 --content-type "application/json" 'http://reservation:8080/cleaningReservations POST {"customerName": "noh","price": 300000,"requestDate": "20200909","status": "ReservationApply"}'
```

- 새버전으로의 배포 시작
```
# 컨테이너 이미지 Update (readness, liveness 미설정 상태)
kubectl apply -f reservation_na.yaml
```

- siege 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인
```console
Lifting the server siege...
Transactions:                  22984 hits
Availability:                  98.68 %
Elapsed time:                 299.64 secs
Data transferred:               7.52 MB
Response time:                  0.01 secs
Transaction rate:              76.71 trans/sec
Throughput:                     0.03 MB/sec
Concurrency:                    0.97
Successful transactions:       22984
Failed transactions:             308
Longest transaction:            0.97
Shortest transaction:           0.00

```

- 배포기간중 Availability 가 평소 100%에서 98% 대로 떨어지는 것을 확인. 
- 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함:
```console
# deployment.yaml 의 readiness probe 의 설정:
kubectl apply -f reservation.yaml

NAME                               READY   STATUS    RESTARTS   AGE
pod/cleaning-bf474f568-vxl8r       2/2     Running   0          4h3m
pod/dashboard-7f7768bb5-7l8wr      2/2     Running   0          4h1m
pod/gateway-6dfcbbc84f-rwnsh       2/2     Running   0          143m
pod/message-69597f6864-fjs69       2/2     Running   0          92m
pod/payment-7749f7dc7c-kfjxb       2/2     Running   0          97m
pod/reservation-775fc6574d-nfnxx   1/1     Running   0          3m54s
pod/siege                          2/2     Running   0          5h24m

```
- 동일한 시나리오로 재배포 한 후 Availability 확인
```console
Lifting the server siege...
Transactions:                   6663 hits
Availability:                 100.00 %
Elapsed time:                 119.51 secs
Data transferred:               2.17 MB
Response time:                  0.02 secs
Transaction rate:              55.75 trans/sec
Throughput:                     0.02 MB/sec
Concurrency:                    0.98
Successful transactions:        6663
Failed transactions:               0
Longest transaction:            0.86
Shortest transaction:           0.00
```

- 배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.

## ConfigMap 사용
- 시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리합니다.
- configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ssak8-config
  namespace: ssak8
data:
  api.url.payment: http://payment:8080
```

- reservation.yaml (configmap 사용)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reservation
  namespace: ssak8
  labels:
    app: reservation
spec:
  replicas: 1
  selector:
    matchLabels:
      app: reservation
  template:
    metadata:
      labels:
        app: reservation
    spec:
      containers:
        - name: reservation
          image: ssak8acr.azurecr.io/reservation:1.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          env:
            - name: api.url.payment
              valueFrom:
                configMapKeyRef:
                  name: ssak8-config
                  key: api.url.payment
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

---

apiVersion: v1
kind: Service
metadata:
  name: reservation
  namespace: ssak8
  labels:
    app: reservation
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: reservation
```

- configmap 설정 정보 확인
```console
kubectl describe pod/reservation-775fc6574d-kddgd -n ssak8

...중략
Containers:
  reservation:
    Container ID:   docker://af733ea1c805029ad0baf5c448981b3b84def8e4c99656638f2560b48b14816e
    Image:          ssak8acr.azurecr.io/reservation:1.0
    Image ID:       docker-pullable://ssak8acr.azurecr.io/reservation@sha256:5a9eb3e1b40911025672798628d75de0670f927fccefea29688f9627742e3f6d
    Port:           8080/TCP
    Host Port:      0/TCP
    State:          Running
      Started:      Tue, 08 Sep 2020 13:24:05 +0000
    Ready:          True
    Restart Count:  0
    Liveness:       http-get http://:8080/actuator/health delay=120s timeout=2s period=5s #success=1 #failure=5
    Readiness:      http-get http://:8080/actuator/health delay=10s timeout=2s period=5s #success=1 #failure=10
    Environment:
      api.url.payment:  <set to the key 'api.url.payment' of config map 'ssak8-config'>  Optional: false
    Mounts:
      /var/run/secrets/kubernetes.io/serviceaccount from default-token-w4fh5 (ro)
...중략
```


# 개인 MSA
1. 고객관리 (이름, 주소 등등) => 연제경 
2. 청소부 관리 (이름, 핸드폰 번호 등등) => 노필호
3. 청소부 리뷰관리 (리뷰, 별점평가 등등) => 성은주
4. 고객 리뷰관리 (리뷰, 별점평가 등등) => 채민호
5. 결제 관리 (카드, 무통장 등등) => 박유리
