# Ingestion Playbook

## 1. 수집 우선순위
1. RSS/Atom
2. Sitemap
3. HTML 크롤링

## 2. 파서 인터페이스
```java
public interface TechBlogParser {
    boolean supports(ParserType parserType);
    List<ParsedPost> fetch(Source source);
}
```

`ParsedPost` 필수 필드
- title
- canonicalUrl
- publishedAt
- sourceName

## 3. 동기화 플로우
1. `SyncScheduler`가 활성 소스 조회
2. 소스 단위 `sync_jobs` 생성(`RUNNING`)
3. 해당 파서 실행
4. Normalizer로 URL/시간/태그 정규화
5. `posts` upsert + `post_tags` 갱신
6. 성공/실패 카운트 업데이트 후 `sync_jobs` 종료

## 4. 실패 처리
- 네트워크 오류: 최대 3회 재시도, 지수 백오프
- 파싱 오류: 해당 소스만 실패 처리, 전체 배치 중단 금지
- 구조 변경 감지: 연속 실패 횟수 임계치 초과 시 알림

## 5. 소스 온보딩 체크리스트
- robots.txt 및 약관 확인
- 허용 요청 빈도 확인
- feed/sitemap 안정성 확인
- canonical URL 존재 여부 확인
- publishedAt 파싱 규칙 확인
- 샘플 20건 파싱 테스트 통과

## 6. 운영 지표
- 소스별 최근 성공 시각
- 최근 24시간 수집 성공률
- 파싱 실패 Top N 소스
- 평균 수집 소요시간

## 7. 개선사항 위치
- 수집 안정화/온보딩/장애 대응 개선 항목은 `docs/improvements/BACKEND_IMPROVEMENTS.md`에서 관리합니다.
