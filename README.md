# Arkive

> Arkive는 다른 프로젝트에 기능 이식을 편하게 만들기 위해 만든 아카이브/레퍼런스 프로젝트다.
> 새 기능은 Arkive에 축적하고, 타 전자정부프레임워크·Spring·Spring Boot 프로젝트에서 복붙으로 가져간다.
> 프로젝트명·패키지·테이블명·파일 위치·DB 제품(Oracle/PostgreSQL 등)은 이식 대상에 맞게 최소한으로 바꾸는 것을 전제로 한다.

## 문서 구조

| 구분 | 의미 | 위치 |
| --- | --- | --- |
| 복붙 가능 | 그대로 복사해서 사용 | `docs/ai/common/` 7종 |
| 가이드형 템플릿 | 파일째 복사한 뒤 양식만 채운다 (Arkive 내용은 작성 예시) | `docs/ai/project/` 10종 |
| 선택 | 대상 프로젝트에서 필요할 때 작성 | `docs/ai/automation/`, `docs/ai/operations/` |

## 가져가는 순서

1. `docs/ai/common/`은 그대로 복사한다.
2. `docs/ai/project/`는 폴더째 복사한 뒤 각 문서 상단의 작성 가이드대로 양식만 채운다.
3. 기능 코드는 화면 → Controller → Service → Mapper → XML → `db/versions` SQL → 설정·의존성 순서로 가져간다.

## 문서 안내

- AI 작업 지침: [AGENTS.md](AGENTS.md)
- 전체 체계·이식 구분: [docs/ai/README.md](docs/ai/README.md)
- 공통 방법: [docs/ai/common/](docs/ai/common/)
- 프로젝트 지식 (가이드형): [docs/ai/project/](docs/ai/project/)
