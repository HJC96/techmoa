# Architecture

## 1. 시스템 구성
- Frontend: React (SPA), Vite
- Backend: Java 21, Spring Boot
- Database: PostgreSQL
- Cache: Redis
- Batch/Scheduler: Spring Scheduler (초기), 필요 시 워커 분리

## 2. 컴포넌트 흐름
1. Scheduler가 소스 목록 조회
2. 소스별 Parser가 최신 글 메타데이터 수집
3. Normalizer가 canonical URL/태그/시간 포맷 정규화
4. Deduplicator가 기존 글과 비교 후 upsert
5. API가 목록/상세/필터/검색 제공
6. Redis에 목록 캐시 저장

## 3. 백엔드 모듈 구조
```text
com.techmoa
├─ common
│  ├─ config
│  ├─ exception
│  └─ util
├─ source
│  ├─ domain
│  ├─ application
│  ├─ infrastructure
│  └─ presentation
├─ post
│  ├─ domain
│  ├─ application
│  ├─ infrastructure
│  └─ presentation
├─ ingestion
│  ├─ scheduler
│  ├─ parser
│  ├─ normalizer
│  └─ application
└─ admin
   ├─ application
   └─ presentation
```

## 4. 비기능 요구사항
- 안정성: 소스별 실패 격리, 재시도(backoff), 실패 로그 저장
- 성능: 목록 API p95 300ms 목표 (캐시 히트 시)
- 확장성: 파서 전략패턴으로 신규 소스 추가 비용 최소화
- 관측성: 수집 성공률, 수집 소요시간, API latency 메트릭 수집

## 5. 배포 전략 (초기)
- `docker-compose`로 로컬/개발 환경 통일
- 단일 Spring Boot 애플리케이션 배포
- 프론트 정적 배포 (예: Vercel/S3+CloudFront)

## 6. 확장 시점 기준
- 소스 수가 30+ 또는 수집 주기가 짧아져 부하 증가 시:
1. ingestion 모듈을 별도 워커 서비스로 분리
2. 메시지 큐(Kafka/RabbitMQ) 도입
3. 검색 엔진(OpenSearch) 분리

## 7. 개선사항 위치
- 아키텍처/확장 로드맵은 `docs/improvements/PROJECT_IMPROVEMENTS.md`, `docs/improvements/INFRA_IMPROVEMENTS.md`에서 관리합니다.
- API별 호출 순서는 `docs/BACKEND_SEQUENCE_DIAGRAM.md`에서 관리합니다.
