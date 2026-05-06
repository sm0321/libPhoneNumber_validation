# libPhoneNumber validation

**Google [libphonenumber](https://github.com/google/libphonenumber) (Java)** 가 기대대로 동작하는지 검증하기 위한 **테스트 하네스** 저장소입니다.  
골든 데이터·스냅샷 비교·CI로 **버전 업 시 회귀**를 빨리 잡는 것이 목적입니다.

[![CI](https://github.com/sm0321/libPhoneNumber_validation/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/sm0321/libPhoneNumber_validation/actions/workflows/ci.yml)

---

## 이 프로젝트가 하는 일

| 구분 | 설명 |
|------|------|
| **통합 테스트** | 실제 JVM에서 `libphonenumber`를 로드해 파싱·포맷·타입·국번 등을 검증합니다. |
| **골든 마스터** | `golden-dataset.json`에 저장된 입력·기대 결과와 **현재 라이브러리 출력**이 일치하는지 확인합니다 (`GoldenTest`). |
| **스냅샷 / 회귀** | 같은 골든 입력에 대해 **현재 버전**의 결과가 바뀌었는지 `diff.json`으로 요약합니다 (`SnapshotTest`). |

CI에서는 부담을 줄이기 위해 **`SnapshotTest`만** 돌리고, 변경이 있으면 Slack·아티팩트·PR 요약 등으로 알립니다.

---

## 요구 사항

- **JDK 17** (GitHub Actions와 동일하게 Temurin 17 권장)
- **Gradle** (저장소에 포함된 Wrapper 사용)

---

## 빠른 시작

```bash
# 전체 빌드 + 테스트
./gradlew build          # Windows: .\gradlew.bat build

# 스냅샷만 (CI와 유사)
./gradlew test --tests SnapshotTest

# 골든만
./gradlew test --tests GoldenTest
```

샘플 애플리케이션 실행:

```bash
./gradlew run
```

---

## 데이터셋 생성

`golden-dataset.json`은 **수동으로만** 재생성하는 것을 전제로, Gradle 태스크를 따로 둡니다.

```bash
./gradlew runDatasetGenerator
```

생성 후 `GoldenTest` / `SnapshotTest`를 다시 돌려 기대 결과와 맞는지 확인하세요.

---

## 주요 Gradle 태스크

| 태스크 | 설명 |
|--------|------|
| `build` | 컴파일, JAR, **전체 테스트** |
| `test` | JUnit 전체 |
| `run` | `org.example.Main` (템플릿 예제) |
| `runDatasetGenerator` | `generator.DatasetGenerator` → `golden-dataset.json` 작성 |

---

## 디렉터리 개요

```
src/main/java/
  org/example/Main.java          # 예제 진입점
  generator/DatasetGenerator.java  # 골든 JSON 생성기

src/test/java/
  GoldenTest.java                # 골든 마스터 검증
  SnapshotTest.java              # 현재 lib vs 골든 diff

src/test/resources/
  golden-dataset.json            # 테스트용 번호·기대값 데이터

.github/workflows/
  ci.yml                         # Actions: SnapshotTest, Slack, 아티팩트, PR 요약

build/snapshot/
  diff.json                      # 로컬/CI 실행 후 스냅샷 diff (build 산출물)
```

---

## CI / 시크릿

- **`SLACK_WEBHOOK_URL`** — Slack Incoming Webhook.

두 번째 웹훅(`CPS_WEBHOOK_URL`)은 **기본 꺼짐**. 켤 때는 `.github/workflows/ci.yml`의 Slack 스텝 안 **주석 안내(① `env` ② `curl` 블록)** 를 따라 주석 해제하고, Actions 시크릿 `CPS_WEBHOOK_URL`을 등록하면 됩니다.

워크플로는 `master` 체크아웃 후 `SnapshotTest`를 실행하고, `changes_count ≥ 1`일 때 후속 단계(알림·PR 등)가 이어지도록 구성되어 있습니다. 자세한 분기는 `.github/workflows/ci.yml`을 참고하세요.

---

## 라이브러리 버전

`build.gradle`의 `com.googlecode.libphonenumber:libphonenumber` 버전을 올리거나 내리면, **골든·스냅샷을 갱신할지** 회귀 분석이 필요합니다.

현재 선언 버전은 `build.gradle`의 `dependencies` 블록을 기준으로 합니다.

---

## 라이선스

이 저장소의 라이선스 정책이 정해져 있지 않다면, 조직 규칙에 맞게 `LICENSE` 파일을 추가하는 것을 권장합니다.  
`libphonenumber` 자체는 [Apache License 2.0](https://github.com/google/libphonenumber/blob/master/LICENSE)입니다.
